package com.acme.orders.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.ReserveRequest;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderNotFoundException;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.orders.api.PlaceOrderRequest;
import io.macroservice.platform.support.ModuleService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stage 4 flow: record the order, ask inventory for a reservation, record the outcome. The call to another module
 * happens outside any transaction (never hold a connection across a cross-module call); the full saga with
 * compensation arrives in stage 5.
 */
@ModuleService(module = "orders")
class OrdersService implements OrdersApi {

    private final OrderRepository orders;
    private final InventoryApi inventory;
    private final TransactionTemplate transactions;

    OrdersService(OrderRepository orders, InventoryApi inventory, @Qualifier("ordersTm") PlatformTransactionManager tm) {
        this.orders = orders;
        this.inventory = inventory;
        this.transactions = new TransactionTemplate(tm);
    }

    @Override
    public Order placeOrder(PlaceOrderRequest request) {
        OrderEntity order = transactions.execute(status -> orders.findByRequestId(request.requestId())
                .orElseGet(() -> orders.save(new OrderEntity(UUID.randomUUID().toString(), request.requestId(),
                        request.customerId(), request.sku(), request.quantity(), OrderStatus.PENDING))));
        if (order.getStatus() != OrderStatus.PENDING) {
            return toApi(order);
        }
        OrderStatus outcome = OrderStatus.CONFIRMED;
        RuntimeException failure = null;
        try {
            inventory.reserve(new ReserveRequest(request.requestId(), order.getOrderId(), request.sku(), request.quantity()));
        } catch (OutOfStockException e) {
            outcome = OrderStatus.REJECTED;
            failure = e;
        }
        OrderStatus finalStatus = outcome;
        OrderEntity saved = transactions.execute(status -> {
            OrderEntity current = orders.findById(order.getOrderId()).orElseThrow();
            current.setStatus(finalStatus);
            return current;
        });
        if (failure != null) {
            throw failure;
        }
        return toApi(saved);
    }

    @Override
    public Order getOrder(String orderId) {
        return transactions.execute(status -> orders.findById(orderId).map(OrdersService::toApi)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)));
    }

    private static Order toApi(OrderEntity e) {
        return new Order(e.getOrderId(), e.getRequestId(), e.getCustomerId(), e.getSku(), e.getQuantity(), e.getStatus());
    }
}
