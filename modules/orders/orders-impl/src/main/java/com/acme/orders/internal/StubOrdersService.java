package com.acme.orders.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveRequest;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderNotFoundException;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.orders.api.PlaceOrderRequest;
import io.macroservice.platform.support.ModuleService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory stand-in until stage 4 adds real persistence. */
@ModuleService(module = "orders")
class StubOrdersService implements OrdersApi {

    private final InventoryApi inventory;
    private final Map<String, Order> ordersById = new ConcurrentHashMap<>();
    private final Map<String, Order> ordersByRequestId = new ConcurrentHashMap<>();

    StubOrdersService(InventoryApi inventory) {
        this.inventory = inventory;
    }

    @Override
    public synchronized Order placeOrder(PlaceOrderRequest request) {
        Order existing = ordersByRequestId.get(request.requestId());
        if (existing != null) {
            return existing;
        }
        String orderId = UUID.randomUUID().toString();
        Reservation reservation = inventory
                .reserve(new ReserveRequest(request.requestId(), orderId, request.sku(), request.quantity()));
        Order order = new Order(orderId, request.requestId(), request.customerId(), reservation.sku(), reservation.quantity(), OrderStatus.CONFIRMED);
        ordersById.put(orderId, order);
        ordersByRequestId.put(request.requestId(), order);
        return order;
    }

    @Override
    public Order getOrder(String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        return order;
    }
}
