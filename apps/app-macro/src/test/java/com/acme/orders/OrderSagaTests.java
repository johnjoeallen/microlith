package com.acme.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.customers.api.Address;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.Tier;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationStatus;
import com.acme.inventory.api.ReserveLine;
import com.acme.notifications.api.Notification;
import com.acme.notifications.api.NotificationUnavailableException;
import com.acme.notifications.api.NotificationsApi;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.payments.api.Authorization;
import com.acme.payments.api.AuthorizationStatus;
import com.acme.payments.api.PaymentDeclinedException;
import com.acme.payments.api.PaymentsApi;
import com.acme.pricing.api.PricedLine;
import com.acme.pricing.api.PricingApi;
import com.acme.pricing.api.Quote;
import com.acme.shipping.api.Shipment;
import com.acme.shipping.api.ShippingApi;
import com.acme.shipping.api.ShippingQuote;
import com.acme.shipping.api.UndeliverableException;
import io.macroservice.platform.rpc.RpcUnavailableException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The order saga with every collaborator mocked at its -api: each scenario of section 16.5, plus resuming an
 * interrupted order. The orders schema, the state machine, the outbox and its relay are real.
 */
@ApplicationModuleTest
@ActiveProfiles("demo")
class OrderSagaTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "orders");
        registry.add("modules.orders.outbox.poll-interval", () -> "100ms");
        registry.add("modules.orders.outbox.backoff-base", () -> "100ms");
        registry.add("modules.orders.outbox.backoff-max", () -> "500ms");
    }

    @MockitoBean
    CustomersApi customers;
    @MockitoBean
    PricingApi pricing;
    @MockitoBean
    ShippingApi shipping;
    @MockitoBean
    InventoryApi inventory;
    @MockitoBean
    PaymentsApi payments;
    @MockitoBean
    NotificationsApi notifications;

    @Autowired
    OrdersApi orders;

    @Autowired
    @Qualifier("ordersDataSource")
    DataSource ordersDataSource;

    private static final List<OrderLine> LINES = List.of(new OrderLine("sku-widget", 2));

    @BeforeEach
    void happyCollaborators() {
        reset(customers, pricing, shipping, inventory, payments, notifications);
        when(customers.getCustomer(anyString())).thenReturn(new Customer("c", "N", "n@example.com", Tier.STANDARD, new Address("1 St", "Town", "IE")));
        when(pricing.quote(anyString(), anyList())).thenReturn(quote(2000, 0));
        when(shipping.quote(anyString(), anyList())).thenReturn(new ShippingQuote("c", "wh-1", 700));
        when(inventory.reserve(anyString(), anyString(), anyList())).thenAnswer(call ->
                new Reservation("res-" + call.getArgument(0), call.getArgument(1), ReservationStatus.RESERVED, List.of(new ReserveLine("sku-widget", 2))));
        when(payments.authorize(anyString(), anyString(), anyLong())).thenAnswer(call ->
                new Authorization("auth-" + call.getArgument(0), call.getArgument(1), call.getArgument(2), AuthorizationStatus.AUTHORIZED));
        when(shipping.createShipment(anyString(), anyString(), anyString(), anyString())).thenAnswer(call ->
                new Shipment("ship-" + call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3), "1 St"));
        when(notifications.send(anyString(), anyString(), anyString(), any())).thenAnswer(call ->
                new Notification("n-" + call.getArgument(0), call.getArgument(1), "n@example.com", call.getArgument(2)));
    }

    private static Quote quote(long subtotal, long discount) {
        return new Quote("c", List.of(new PricedLine("sku-widget", 2, subtotal / 2, subtotal)), subtotal, discount, subtotal - discount);
    }

    private static String newRequestId() {
        return "req-" + UUID.randomUUID();
    }

    private String scalar(String sql, String argument) throws Exception {
        try (Connection c = ordersDataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, argument);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private String outboxStatus(String requestId) throws Exception {
        return scalar("select status from outbox where request_id = ?", requestId);
    }

    @Test
    void happyPathCallsTheStepsInOrder() {
        String requestId = newRequestId();
        Order order = orders.placeOrder(requestId, "c", LINES);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.subtotalCents()).isEqualTo(2000);
        assertThat(order.shippingCents()).isEqualTo(700);
        assertThat(order.totalCents()).isEqualTo(2700);

        // the customer check and the two quotes are independent reads that run at the same time: they come in any order, all before reserve
        InOrder customerFirst = inOrder(customers, inventory);
        customerFirst.verify(customers).getCustomer("c");
        customerFirst.verify(inventory).reserve(eq(requestId + ":reserve"), eq(order.orderId()), anyList());
        InOrder pricedFirst = inOrder(pricing, inventory);
        pricedFirst.verify(pricing).quote(eq("c"), anyList());
        pricedFirst.verify(inventory).reserve(eq(requestId + ":reserve"), eq(order.orderId()), anyList());

        InOrder steps = inOrder(shipping, inventory, payments);
        steps.verify(shipping).quote(eq("c"), anyList());
        steps.verify(inventory).reserve(eq(requestId + ":reserve"), eq(order.orderId()), anyList());
        steps.verify(payments).authorize(requestId + ":authorize", "c", 2700);
        steps.verify(shipping).createShipment(eq(requestId + ":ship"), eq(order.orderId()), eq("c"), eq("res-" + requestId + ":reserve"));
        steps.verify(payments).capture(requestId + ":capture", "auth-" + requestId + ":authorize");
        verify(notifications, timeout(10_000)).send(eq(requestId + ":notify"), eq("c"), eq("order-confirmed"), any());
    }

    @Test
    void theTierDiscountComesFromPricing() {
        when(pricing.quote(anyString(), anyList())).thenReturn(quote(2000, 200));
        Order order = orders.placeOrder(newRequestId(), "c", LINES);
        assertThat(order.discountCents()).isEqualTo(200);
        assertThat(order.totalCents()).isEqualTo(2000 - 200 + 700);
    }

    @Test
    void outOfStockRejectsTheOrderAndNothingElseHappens() throws Exception {
        when(inventory.reserve(anyString(), anyString(), anyList())).thenThrow(new OutOfStockException("Out of stock: sku-oos"));
        String requestId = newRequestId();
        assertThatThrownBy(() -> orders.placeOrder(requestId, "c", LINES)).isExactlyInstanceOf(OutOfStockException.class);
        verifyNoInteractions(payments);
        verify(shipping, never()).createShipment(anyString(), anyString(), anyString(), anyString());
        assertThat(scalar("select status from orders where request_id = ?", requestId)).isEqualTo("REJECTED");
        assertThat(scalar("select count(*) from outbox where order_id = (select order_id from orders where request_id = ?)", requestId)).isEqualTo("0");
        assertThatThrownBy(() -> orders.placeOrder(requestId, "c", LINES)).as("a repeat gets the same outcome").isExactlyInstanceOf(OutOfStockException.class);
        verify(inventory, times(1)).reserve(anyString(), anyString(), anyList());
    }

    @Test
    void aDeclinedPaymentRejectsTheOrderAndReleasesTheReservation() throws Exception {
        when(payments.authorize(anyString(), anyString(), anyLong())).thenThrow(new PaymentDeclinedException("declined"));
        String requestId = newRequestId();
        Order order = orders.placeOrder(requestId, "c", LINES);
        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        InOrder steps = inOrder(inventory);
        steps.verify(inventory).reserve(eq(requestId + ":reserve"), anyString(), anyList());
        steps.verify(inventory, timeout(10_000)).release(requestId + ":release", "res-" + requestId + ":reserve");
        verify(payments, never()).voidAuthorization(anyString(), anyString());
        verify(shipping, never()).createShipment(anyString(), anyString(), anyString(), anyString());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(outboxStatus(requestId + ":release")).isEqualTo("DONE"));
    }

    @Test
    void aShippingFailureFailsTheOrderAndVoidsThePaymentAndReleasesTheReservation() throws Exception {
        when(shipping.createShipment(anyString(), anyString(), anyString(), anyString())).thenThrow(new UndeliverableException("Cannot deliver"));
        String requestId = newRequestId();
        Order order = orders.placeOrder(requestId, "c", LINES);
        assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
        verify(payments, timeout(10_000)).voidAuthorization(requestId + ":void", "auth-" + requestId + ":authorize");
        verify(inventory, timeout(10_000)).release(requestId + ":release", "res-" + requestId + ":reserve");
        verify(payments, never()).capture(anyString(), anyString());
        verify(notifications, never()).send(eq(requestId + ":notify"), anyString(), anyString(), any());
    }

    @Test
    void aNotificationsOutageNeverFailsTheOrderAndTheMessageIsDeliveredAfterRecovery() throws Exception {
        String requestId = newRequestId();
        int[] failures = {0};
        doAnswer(call -> {
            // earlier tests may still have entries in the shared outbox; only this order's message is made to fail
            if (call.getArgument(0).equals(requestId + ":notify") && failures[0]++ < 3) {
                throw new NotificationUnavailableException("down");
            }
            return new Notification("n", "c", "n@example.com", "order-confirmed");
        }).when(notifications).send(anyString(), anyString(), anyString(), any());
        Order order = orders.placeOrder(requestId, "c", LINES);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(notifications, timeout(15_000).atLeast(4)).send(eq(requestId + ":notify"), eq("c"), eq("order-confirmed"), eq(Map.of("orderId", order.orderId(), "totalCents", "2700")));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(outboxStatus(requestId + ":notify")).isEqualTo("DONE"));
    }

    @Test
    void theSameRequestTwiceCausesOneOfEverything() throws Exception {
        String requestId = newRequestId();
        Order first = orders.placeOrder(requestId, "c", LINES);
        Order second = orders.placeOrder(requestId, "c", LINES);
        assertThat(second).isEqualTo(first);
        verify(inventory, times(1)).reserve(anyString(), anyString(), anyList());
        verify(payments, times(1)).authorize(anyString(), anyString(), anyLong());
        verify(shipping, times(1)).createShipment(anyString(), anyString(), anyString(), anyString());
        verify(payments, times(1)).capture(anyString(), anyString());
        assertThat(scalar("select count(*) from orders where request_id = ?", requestId)).isEqualTo("1");
    }

    @Test
    void anInterruptedOrderResumesWhereItStopped() throws Exception {
        when(payments.authorize(anyString(), anyString(), anyLong())).thenThrow(new RpcUnavailableException("payments unreachable"));
        String requestId = newRequestId();
        assertThatThrownBy(() -> orders.placeOrder(requestId, "c", LINES)).isInstanceOf(RpcUnavailableException.class);
        assertThat(scalar("select status from orders where request_id = ?", requestId)).isEqualTo("RESERVED");

        doAnswer(call -> new Authorization("auth-1", call.getArgument(1), call.getArgument(2), AuthorizationStatus.AUTHORIZED))
                .when(payments).authorize(anyString(), anyString(), anyLong());
        Order order = orders.placeOrder(requestId, "c", LINES);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventory, times(1)).reserve(eq(requestId + ":reserve"), anyString(), anyList());
    }
}
