package com.acme.orders.internal;

import com.acme.orders.api.OrderStatus;
import io.macroservice.platform.support.ModuleComponent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Every order state change, together with the outbox rows that belong to it, in ONE transaction of the orders
 * schema. Nothing in here calls another module.
 */
@ModuleComponent(module = "orders")
@Transactional("ordersTm")
public class OrderStore {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final OutboxSignal signal;

    OrderStore(OrderRepository orders, OutboxRepository outbox, OutboxSignal signal) {
        this.orders = orders;
        this.outbox = outbox;
        this.signal = signal;
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

    OrderEntity reject(String orderId, String code, String reason, List<OutboxCommand> compensations) {
        return finish(orderId, OrderStatus.REJECTED, code, reason, compensations);
    }

    OrderEntity fail(String orderId, String code, String reason, List<OutboxCommand> compensations) {
        return finish(orderId, OrderStatus.FAILED, code, reason, compensations);
    }

    OrderEntity confirm(String orderId, String shipmentId, OutboxCommand notification) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setShipmentId(shipmentId);
        enqueue(orderId, notification);
        return order;
    }

    private OrderEntity finish(String orderId, OrderStatus status, String code, String reason, List<OutboxCommand> commands) {
        OrderEntity order = orders.findById(orderId).orElseThrow();
        order.setStatus(status);
        order.setFailureCode(code);
        order.setFailureReason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)));
        commands.forEach(command -> enqueue(orderId, command));
        return order;
    }

    private void enqueue(String orderId, OutboxCommand command) {
        Instant now = Instant.now();
        // The relay runs later, on another thread: carry the correlation id so its calls stay attached to the order.
        Map<String, String> args = new LinkedHashMap<>(command.args());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            args.put(Payload.CORRELATION_KEY, correlationId);
        }
        outbox.save(new OutboxEntryEntity(UUID.randomUUID().toString(), orderId, command.target(), command.requestId(),
                Payload.encode(args), "PENDING", 0, now, null, now));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                signal.raise();
            }
        });
    }

    @Transactional(value = "ordersTm", readOnly = true)
    public long pendingOutbox() {
        return outbox.countByStatus("PENDING");
    }
}
