package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationNotFoundException;
import com.acme.inventory.api.ReserveLine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The boundary interceptor around local cross-module calls: an outer transaction is not joined (so a local call
 * behaves like a remote one), and every call is observed with the same tags as a remote call.
 */
class BoundaryInterceptorTests {

    private final TestApps apps = new TestApps();

    @AfterEach
    void close() {
        apps.close();
    }

    private ConfigurableApplicationContext start(String... extra) {
        return apps.start(false, TestApps.local("inventory", "catalog"), TestApps.args(extra));
    }

    /** Reserves stock inside an outer transaction on the inventory database and marks that transaction rollback-only. */
    private static Reservation reserveInRolledBackTransaction(ConfigurableApplicationContext context) {
        InventoryApi inventory = context.getBean(InventoryApi.class);
        TransactionTemplate outer = new TransactionTemplate(context.getBean("inventoryTm", PlatformTransactionManager.class));
        String requestId = "tx-" + UUID.randomUUID();
        return outer.execute(status -> {
            Reservation reservation = inventory.reserve(requestId, "o-" + requestId, List.of(new ReserveLine("sku-widget", 1)));
            status.setRollbackOnly();
            return reservation;
        });
    }

    @Test
    void anOuterTransactionIsNotJoinedByDefault() {
        ConfigurableApplicationContext context = start();

        Reservation reservation = reserveInRolledBackTransaction(context);

        // the callee committed on its own, although the caller's transaction rolled back: exactly what a remote callee does
        assertThat(context.getBean(InventoryApi.class).getReservation(reservation.reservationId()).reservationId()).isEqualTo(reservation.reservationId());
    }

    @Test
    void withSuspensionSwitchedOffTheCalleeJoinsTheOuterTransaction() {
        ConfigurableApplicationContext context = start("--modules.boundary.suspend-transactions=false");

        Reservation reservation = reserveInRolledBackTransaction(context);

        assertThatThrownBy(() -> context.getBean(InventoryApi.class).getReservation(reservation.reservationId()))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void theCallerTransactionResumesAfterTheCall() {
        ConfigurableApplicationContext context = start();
        InventoryApi inventory = context.getBean(InventoryApi.class);
        TransactionTemplate outer = new TransactionTemplate(context.getBean("inventoryTm", PlatformTransactionManager.class));

        Boolean activeBefore = outer.execute(status -> {
            inventory.getStock("sku-widget");
            return org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
        });

        assertThat(activeBefore).isTrue();
    }

    @Test
    void everyBindingIsObservedWithTheSameTags() {
        ConfigurableApplicationContext local = start();
        local.getBean(InventoryApi.class).getStock("sku-widget");
        assertThat(count(local, "local")).isEqualTo(1);
        assertThat(timer(local, "local").getId().getTags()).extracting(t -> t.getKey()).contains("module", "method", "binding", "outcome");

        ConfigurableApplicationContext codec = apps.start(false, TestApps.local("inventory", "catalog"), TestApps.binding("loopback-codec", List.of("inventory")));
        codec.getBean(InventoryApi.class).getStock("sku-widget");
        // counted once by the caller's proxy under its own binding; the dispatcher's local call is not counted again
        assertThat(count(codec, "loopback-codec")).isEqualTo(1);
        assertThat(codec.getBean(MeterRegistry.class).find("macro.rpc.client").tag("binding", "local").tag("module", "inventory").timer()).isNull();
        assertThat(timer(codec, "loopback-codec").getId().getTags()).extracting(t -> t.getKey())
                .containsExactlyInAnyOrderElementsOf(timer(local, "local").getId().getTags().stream().map(t -> t.getKey()).toList());
    }

    @Test
    void aFailingCallIsObservedAsAnError() {
        ConfigurableApplicationContext context = start();
        assertThatThrownBy(() -> context.getBean(InventoryApi.class).getReservation("nope")).isInstanceOf(ReservationNotFoundException.class);

        Timer timer = context.getBean(MeterRegistry.class).find("macro.rpc.client").tag("method", "getReservation").tag("outcome", "error").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    private static long count(ConfigurableApplicationContext context, String binding) {
        Timer timer = timer(context, binding);
        return timer == null ? 0 : timer.count();
    }

    private static Timer timer(ConfigurableApplicationContext context, String binding) {
        return context.getBean(MeterRegistry.class).find("macro.rpc.client").tag("module", "inventory").tag("method", "getStock")
                .tag("binding", binding).timer();
    }
}
