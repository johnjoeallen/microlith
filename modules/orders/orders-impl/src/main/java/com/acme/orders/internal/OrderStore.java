package com.acme.orders.internal;

import com.acme.orders.api.OrderStatus;
import io.macroservice.platform.support.ModuleComponent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every order state change, together with the events that belong to it, in ONE transaction of the orders schema. Spring
 * Modulith's event publication registry stores each published event in that transaction (the {@code event_publication}
 * table), and delivers it to its listener after the commit. Nothing in here calls another module.
 */
@ModuleComponent(module = "orders")
@Transactional("ordersTm")
public class OrderStore {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    OrderStore(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    @Transactional(value = "ordersTm", readOnly = true)
    OrderEntity findByRequestId(String requestId) {
        return orders.findByRequestId(requestId).orElse(null);
    }

    @Transactional(value = "ordersTm", readOnly = true)
    OrderEntity findById(String orderId) {
        return orders.findById(orderId).orElse(null);
    }

    OrderEntity createPending(String requestId, String customerId, String lines, long subtotal, long discount, long shipping, long total) {
        return orders.saveAndFlush(new OrderEntity(UUID.randomUUID().toString(), requestId, customerId, OrderStatus.PENDING, lines,
                subtotal, discount, shipping, total, null, null, null, null, null));
    }

    OrderEntity markReserved(String orderId, String reservationId) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.RESERVED);
        order.setReservationId(reservationId);
        return order;
    }

    OrderEntity markAuthorized(String orderId, String authorizationId) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.AUTHORIZED);
        order.setAuthorizationId(authorizationId);
        return order;
    }

    OrderEntity reject(String orderId, String code, String reason, List<?> compensations) {
        return finish(orderId, OrderStatus.REJECTED, code, reason, compensations);
    }

    OrderEntity fail(String orderId, String code, String reason, List<?> compensations) {
        return finish(orderId, OrderStatus.FAILED, code, reason, compensations);
    }

    OrderEntity confirm(String orderId, String shipmentId, Object notification) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setShipmentId(shipmentId);
        events.publishEvent(notification);
        return order;
    }

    private OrderEntity finish(String orderId, OrderStatus status, String code, String reason, List<?> commands) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(status);
        order.setFailureCode(code);
        order.setFailureReason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)));
        commands.forEach(events::publishEvent);
        return order;
    }
}
