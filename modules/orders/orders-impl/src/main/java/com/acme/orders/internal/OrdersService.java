package com.acme.orders.internal;

import com.acme.customers.api.CustomersApi;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveLine;
import com.acme.orders.api.InvalidOrderException;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderNotFoundException;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.payments.api.Authorization;
import com.acme.payments.api.PaymentDeclinedException;
import com.acme.payments.api.PaymentsApi;
import com.acme.pricing.api.PricingApi;
import com.acme.pricing.api.Quote;
import com.acme.pricing.api.QuoteLine;
import com.acme.shipping.api.ShipLine;
import com.acme.shipping.api.Shipment;
import com.acme.shipping.api.ShippingApi;
import com.acme.shipping.api.ShippingQuote;
import io.macroservice.platform.rpc.RpcError;
import io.macroservice.platform.support.ModuleService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The order saga. All ordering of calls is explicit here; there is no cross-module event and no cross-module
 * transaction. Each step is idempotent through a request id derived from the order's request id, and the order's
 * persisted state tells a repeated or resumed call where to continue:
 *
 * <pre>
 * PENDING --reserve--> RESERVED --authorize--> AUTHORIZED --ship, capture--> CONFIRMED
 *    |                    |                        |
 *    | out of stock       | declined               | shipping failed
 *    v                    v                        v
 * REJECTED (throws)    REJECTED (+release)      FAILED (+void, +release)
 * </pre>
 *
 * Compensations and the confirmation message are written to the outbox in the same transaction as the state change.
 * Only business failures (exceptions marked {@code @RpcError}) trigger compensation; a transport failure leaves the
 * order where it is, so the caller can repeat the request and the saga resumes.
 */
@ModuleService(module = "orders")
class OrdersService implements OrdersApi {

    private final OrderStore store;
    private final CustomersApi customers;
    private final PricingApi pricing;
    private final ShippingApi shipping;
    private final InventoryApi inventory;
    private final PaymentsApi payments;

    OrdersService(OrderStore store, CustomersApi customers, PricingApi pricing, ShippingApi shipping, InventoryApi inventory,
            PaymentsApi payments) {
        this.store = store;
        this.customers = customers;
        this.pricing = pricing;
        this.shipping = shipping;
        this.inventory = inventory;
        this.payments = payments;
    }

    @Override
    public Order placeOrder(String requestId, String customerId, List<OrderLine> lines) {
        validate(requestId, customerId, lines);
        OrderEntity order = store.findByRequestId(requestId);
        if (order == null) {
            customers.getCustomer(customerId);                                                       // 1. validate the customer
            Quote quote = pricing.quote(customerId, lines.stream().map(l -> new QuoteLine(l.sku(), l.quantity())).toList());   // 2.
            ShippingQuote delivery = shipping.quote(customerId, lines.stream().map(l -> new ShipLine(l.sku(), l.quantity())).toList());   // 3.
            try {
                order = store.createPending(requestId, customerId, encode(lines), quote.subtotalCents(), quote.discountCents(),
                        delivery.costCents(), quote.totalCents() + delivery.costCents());
            } catch (DataIntegrityViolationException concurrentDuplicate) {
                order = store.findByRequestId(requestId);
            }
        }
        return toApi(drive(order));
    }

    private OrderEntity drive(OrderEntity order) {
        String requestId = order.getRequestId();
        if (order.getStatus() == OrderStatus.REJECTED && "OUT_OF_STOCK".equals(order.getFailureCode())) {
            throw new OutOfStockException(order.getFailureReason());
        }
        if (order.getStatus() == OrderStatus.PENDING) {                                              // 4. reserve stock
            try {
                Reservation reservation = inventory.reserve(requestId + ":reserve", order.getOrderId(), reserveLines(order));
                order = store.markReserved(order.getOrderId(), reservation.reservationId());
            } catch (OutOfStockException e) {
                store.reject(order.getOrderId(), "OUT_OF_STOCK", e.getMessage(), List.of());
                throw e;
            }
        }
        if (order.getStatus() == OrderStatus.RESERVED) {                                             // 5. authorize payment
            try {
                Authorization authorization = payments.authorize(requestId + ":authorize", order.getCustomerId(), order.getTotalCents());
                order = store.markAuthorized(order.getOrderId(), authorization.authId());
            } catch (PaymentDeclinedException e) {
                return store.reject(order.getOrderId(), "PAYMENT_DECLINED", e.getMessage(), List.of(release(order)));
            }
        }
        if (order.getStatus() == OrderStatus.AUTHORIZED) {
            Shipment shipment;
            try {                                                                                    // 6. create the shipment
                shipment = shipping.createShipment(requestId + ":ship", order.getOrderId(), order.getCustomerId(), order.getReservationId());
            } catch (RuntimeException e) {
                if (!isBusinessFailure(e)) {
                    throw e;
                }
                return store.fail(order.getOrderId(), "SHIPPING_FAILED", e.getMessage(), List.of(voidPayment(order), release(order)));
            }
            payments.capture(requestId + ":capture", order.getAuthorizationId());                    // 7. capture, CONFIRMED
            order = store.confirm(order.getOrderId(), shipment.shipmentId(), notification(order));   // 8. notification via the outbox
        }
        return order;
    }

    @Override
    public Order getOrder(String orderId) {
        OrderEntity order = store.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        return toApi(order);
    }

    private static boolean isBusinessFailure(RuntimeException e) {
        return AnnotationUtils.findAnnotation(e.getClass(), RpcError.class) != null;
    }

    private static OutboxCommand release(OrderEntity order) {
        return new OutboxCommand("inventory.release", order.getRequestId() + ":release", map("reservationId", order.getReservationId()));
    }

    private static OutboxCommand voidPayment(OrderEntity order) {
        return new OutboxCommand("payments.void", order.getRequestId() + ":void", map("authId", order.getAuthorizationId()));
    }

    private static OutboxCommand notification(OrderEntity order) {
        return new OutboxCommand("notifications.send", order.getRequestId() + ":notify", map("customerId", order.getCustomerId(),
                "template", "order-confirmed", "orderId", order.getOrderId(), "totalCents", Long.toString(order.getTotalCents())));
    }

    private static Map<String, String> map(String... keysAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static void validate(String requestId, String customerId, List<OrderLine> lines) {
        if (requestId == null || requestId.isBlank() || customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("requestId and customerId are required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("An order needs at least one line");
        }
        for (OrderLine line : lines) {
            if (line == null || line.sku() == null || !line.sku().matches("[A-Za-z0-9_-]+") || line.quantity() <= 0) {
                throw new InvalidOrderException("Invalid order line: sku must be alphanumeric and quantity positive");
            }
        }
    }

    private static String encode(List<OrderLine> lines) {
        return String.join(",", lines.stream().map(l -> l.sku() + ":" + l.quantity()).toList());
    }

    private static List<OrderLine> decode(String text) {
        List<OrderLine> lines = new ArrayList<>();
        for (String part : text.split(",")) {
            String[] fields = part.split(":");
            lines.add(new OrderLine(fields[0], Integer.parseInt(fields[1])));
        }
        return lines;
    }

    private static List<ReserveLine> reserveLines(OrderEntity order) {
        return decode(order.getLines()).stream().map(l -> new ReserveLine(l.sku(), l.quantity())).toList();
    }

    private static Order toApi(OrderEntity e) {
        return new Order(e.getOrderId(), e.getRequestId(), e.getCustomerId(), e.getStatus(), decode(e.getLines()), e.getSubtotalCents(),
                e.getDiscountCents(), e.getShippingCents(), e.getTotalCents(), e.getFailureReason());
    }
}
