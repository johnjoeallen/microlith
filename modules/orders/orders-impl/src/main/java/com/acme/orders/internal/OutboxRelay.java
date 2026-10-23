package com.acme.orders.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.notifications.api.NotificationsApi;
import com.acme.payments.api.PaymentsApi;
import io.macroservice.platform.support.ModuleComponent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Executes outbox entries by calling the target module's @RpcApi method. Targets are idempotent by the entry's
 * request id, so redelivery is safe. A failing entry is retried with exponential backoff; a callee outage never
 * affects the order that produced the entry.
 */
@ModuleComponent(module = "orders")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final Duration LEASE = Duration.ofSeconds(30);

    private final OutboxRepository outbox;
    private final TransactionTemplate transactions;
    private final InventoryApi inventory;
    private final PaymentsApi payments;
    private final NotificationsApi notifications;
    private final Duration backoffBase;
    private final Duration backoffMax;
    private final int maxAttempts;
    private final Duration idleScanInterval;
    private final OutboxSignal signal;
    private final AtomicBoolean keepScanning = new AtomicBoolean();
    private volatile Instant nextIdleScan;

    OutboxRelay(OutboxRepository outbox, @Qualifier("ordersTm") PlatformTransactionManager tm, InventoryApi inventory,
            PaymentsApi payments, NotificationsApi notifications, OutboxSignal signal, Environment environment) {
        this.outbox = outbox;
        this.transactions = new TransactionTemplate(tm);
        this.inventory = inventory;
        this.payments = payments;
        this.notifications = notifications;
        this.signal = signal;
        this.backoffBase = environment.getProperty("modules.orders.outbox.backoff-base", Duration.class, Duration.ofSeconds(1));
        this.backoffMax = environment.getProperty("modules.orders.outbox.backoff-max", Duration.class, Duration.ofSeconds(60));
        this.maxAttempts = environment.getProperty("modules.orders.outbox.max-attempts", Integer.class, 30);
        this.idleScanInterval = environment.getProperty("modules.orders.outbox.idle-scan-interval", Duration.class, Duration.ofSeconds(60));
        this.nextIdleScan = Instant.now().plus(idleScanInterval);
    }

    @Scheduled(fixedDelayString = "${modules.orders.outbox.poll-interval:1s}")
    public void poll() {
        // Scan the database only when there is a reason: rows were just committed, earlier work is still pending, or the
        // slow idle scan (crash recovery, rows written by another instance) is due. Otherwise an idle module would keep
        // a connection busy forever.
        boolean woken = signal.consume();
        if (!woken && !keepScanning.get() && Instant.now().isBefore(nextIdleScan)) {
            return;
        }
        pollOnce();
        boolean morePending = transactions.execute(status -> outbox.countByStatus("PENDING")) > 0;
        keepScanning.set(morePending);
        if (!morePending) {
            nextIdleScan = Instant.now().plus(idleScanInterval);
        }
    }

    /** Processes the entries that are due now; returns how many were attempted. */
    int pollOnce() {
        List<OutboxEntryEntity> claimed = transactions.execute(status -> {
            List<OutboxEntryEntity> due = outbox.claim(Instant.now(), PageRequest.of(0, 20));
            due.forEach(entry -> entry.setNextAttemptAt(Instant.now().plus(LEASE)));
            return due;
        });
        for (OutboxEntryEntity entry : claimed) {
            try {
                dispatch(entry);
                transactions.executeWithoutResult(status -> outbox.findById(entry.getId()).orElseThrow().setStatus("DONE"));
            } catch (RuntimeException e) {
                retryLater(entry, e);
            }
        }
        return claimed.size();
    }

    private void retryLater(OutboxEntryEntity claimedEntry, RuntimeException failure) {
        transactions.executeWithoutResult(status -> {
            OutboxEntryEntity entry = outbox.findById(claimedEntry.getId()).orElseThrow();
            int attempts = entry.getAttempts() + 1;
            long millis = Math.min(backoffMax.toMillis(), backoffBase.toMillis() * (1L << Math.min(attempts - 1, 20)));
            entry.setAttempts(attempts);
            entry.setNextAttemptAt(Instant.now().plusMillis(millis));
            String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
            entry.setLastError(message.substring(0, Math.min(message.length(), 500)));
            if (attempts >= maxAttempts) {
                entry.setStatus("DEAD");
                log.error("Outbox entry {} ({}) gave up after {} attempts: {}", entry.getRequestId(), entry.getTarget(), attempts, message);
            } else {
                log.warn("Outbox entry {} ({}) failed, attempt {}; retrying in {} ms: {}", entry.getRequestId(), entry.getTarget(), attempts, millis, message);
            }
        });
    }

    private void dispatch(OutboxEntryEntity entry) {
        Map<String, String> args = Payload.decode(entry.getPayload());
        switch (entry.getTarget()) {
            case "inventory.release" -> inventory.release(entry.getRequestId(), args.get("reservationId"));
            case "payments.void" -> payments.voidAuthorization(entry.getRequestId(), args.get("authId"));
            case "notifications.send" -> notifications.send(entry.getRequestId(), args.get("customerId"), args.get("template"),
                    Map.of("orderId", args.get("orderId"), "totalCents", args.get("totalCents")));
            default -> throw new IllegalStateException("Unknown outbox target " + entry.getTarget());
        }
    }
}
