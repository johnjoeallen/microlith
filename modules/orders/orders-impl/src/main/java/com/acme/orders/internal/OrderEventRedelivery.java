package com.acme.orders.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.macroservice.platform.support.ModuleComponent;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Spring Modulith stores events and marks a publication FAILED when its listener throws, but it does not decide when to try
 * again: this does. A failed publication is resubmitted after an exponential delay (base, doubling, capped) that grows with the
 * number of attempts, and left FAILED after a maximum number of attempts.
 *
 * <p>An idle orders module must not touch its database (and must start without it), so this does not poll all the time: it scans
 * while a listener has recently failed, plus one slow scan shortly after start and then every {@code idle-scan-interval}, which
 * recovers publications a process left behind when it died (those older than {@code stale-after}).
 */
@ModuleComponent(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "events")
public class OrderEventRedelivery {

    private static final Logger log = LoggerFactory.getLogger(OrderEventRedelivery.class);

    private final IncompleteEventPublications publications;
    private final Duration backoffBase;
    private final Duration backoffMax;
    private final int maxAttempts;
    private final Duration idleScanInterval;
    private final Duration staleAfter;
    private volatile Instant scanUntil = Instant.EPOCH;
    private volatile Instant nextIdleScan;
    private volatile boolean firstScan = true;

    OrderEventRedelivery(IncompleteEventPublications publications, Environment environment) {
        this.publications = publications;
        this.backoffBase = environment.getProperty("modules.orders.events.backoff-base", Duration.class, Duration.ofSeconds(1));
        this.backoffMax = environment.getProperty("modules.orders.events.backoff-max", Duration.class, Duration.ofSeconds(60));
        this.maxAttempts = environment.getProperty("modules.orders.events.max-attempts", Integer.class, 30);
        this.idleScanInterval = environment.getProperty("modules.orders.events.idle-scan-interval", Duration.class, Duration.ofSeconds(60));
        this.staleAfter = environment.getProperty("modules.orders.events.stale-after", Duration.class, Duration.ofMinutes(2));
        this.nextIdleScan = Instant.now().plus(environment.getProperty("modules.orders.events.startup-scan-delay", Duration.class, Duration.ofSeconds(5)));
    }

    /** A listener just failed: look again until its retry is due. */
    void failed() {
        scanUntil = Instant.now().plus(backoffMax).plusSeconds(2);
    }

    @Scheduled(fixedDelayString = "${modules.orders.events.retry-interval:1s}")
    public void poll() {
        Instant now = Instant.now();
        boolean recent = now.isBefore(scanUntil);
        boolean idleScan = !now.isBefore(nextIdleScan);
        if (!recent && !idleScan) {
            return;
        }
        try {
            if (idleScan) {
                if (firstScan) {
                    publications.resubmitIncompletePublications(p -> true);       // this process just started: nothing of ours is in flight
                } else {
                    publications.resubmitIncompletePublicationsOlderThan(staleAfter);
                }
                firstScan = false;
                nextIdleScan = now.plus(idleScanInterval);
            }
            if (recent) {
                publications.resubmitIncompletePublications(ResubmissionOptions.defaults().withMinAge(Duration.ZERO).withFilter(this::due));
            }
        } catch (RuntimeException e) {
            log.warn("Could not look for undelivered order events: {}", e.toString());
            nextIdleScan = now.plus(idleScanInterval);
        }
    }

    private boolean due(EventPublication publication) {
        int attempts = Math.max(1, publication.getCompletionAttempts());
        if (attempts >= maxAttempts) {
            return false;
        }
        long millis = Math.min(backoffMax.toMillis(), backoffBase.toMillis() * (1L << Math.min(attempts - 1, 20)));
        Instant last = publication.getLastResubmissionDate() != null ? publication.getLastResubmissionDate() : publication.getPublicationDate();
        boolean due = Instant.now().isAfter(last.plusMillis(millis));
        if (due) {
            log.warn("Event {} ({}) failed, attempt {}; resubmitting", publication.getIdentifier(), publication.getEvent().getClass().getSimpleName(), attempts);
        }
        return due;
    }
}
