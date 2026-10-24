package com.acme.orders.internal;

import io.macroservice.platform.support.ModuleComponent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** {@code modules.orders.async=outbox} (the default): the follow-up becomes a row in the orders outbox table, delivered by {@link OutboxRelay}. */
@ModuleComponent(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "outbox", matchIfMissing = true)
class OutboxFollowUps implements FollowUps {

    private final OutboxRepository outbox;
    private final OutboxSignal signal;

    OutboxFollowUps(OutboxRepository outbox, OutboxSignal signal) {
        this.outbox = outbox;
        this.signal = signal;
    }

    @Override
    public void publish(Object followUp) {
        switch (followUp) {
            case OrderEvents.ReleaseReservation e -> enqueue(e.correlationId(), e.orderId(), "inventory.release", e.requestId(),
                    map("reservationId", e.reservationId()));
            case OrderEvents.VoidPayment e -> enqueue(e.correlationId(), e.orderId(), "payments.void", e.requestId(), map("authId", e.authorizationId()));
            case OrderEvents.OrderConfirmed e -> enqueue(e.correlationId(), e.orderId(), "notifications.send", e.requestId(),
                    map("customerId", e.customerId(), "template", e.template(), "orderId", e.orderId(), "totalCents", Long.toString(e.totalCents())));
            default -> throw new IllegalArgumentException("Not an order follow-up: " + followUp);
        }
    }

    private void enqueue(String correlationId, String orderId, String target, String requestId, Map<String, String> args) {
        Instant now = Instant.now();
        Map<String, String> payload = new LinkedHashMap<>(args);
        if (correlationId != null) {
            payload.put(Payload.CORRELATION_KEY, correlationId);      // the relay runs later, on another thread
        }
        outbox.save(new OutboxEntryEntity(UUID.randomUUID().toString(), orderId, target, requestId, Payload.encode(payload),
                "PENDING", 0, now, null, now));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                signal.raise();
            }
        });
    }

    private static Map<String, String> map(String... keysAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
