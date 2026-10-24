package com.acme.orders.internal;

import com.acme.inventory.api.InventoryApi;
import com.acme.notifications.api.NotificationsApi;
import com.acme.payments.api.PaymentsApi;
import io.macroservice.platform.support.ModuleComponent;
import java.util.Map;
import org.slf4j.MDC;

/**
 * What the orders module still has to tell other modules after an order changed state. Each is published inside the
 * transaction that changed the order; Spring Modulith's event publication registry stores it in the same transaction and
 * redelivers it until its listener has completed. The listeners call the other modules through their {@code @RpcApi}, so this
 * works in every binding.
 */
public final class OrderEvents {

    private OrderEvents() {
    }

    /** Give a reservation back to inventory. */
    public record ReleaseReservation(String requestId, String reservationId, String correlationId) {
    }

    /** Void a payment authorization. */
    public record VoidPayment(String requestId, String authorizationId, String correlationId) {
    }

    /** Tell the customer that the order is confirmed. */
    public record OrderConfirmed(String requestId, String orderId, String customerId, long totalCents, String template, String correlationId) {
    }

    /** The listeners. Idempotent targets (request id) make redelivery safe. */
    @ModuleComponent(module = "orders")
    public static class Listeners {

        private final InventoryApi inventory;
        private final PaymentsApi payments;
        private final NotificationsApi notifications;
        private final OrderEventRedelivery redelivery;

        Listeners(InventoryApi inventory, PaymentsApi payments, NotificationsApi notifications, OrderEventRedelivery redelivery) {
            this.inventory = inventory;
            this.payments = payments;
            this.notifications = notifications;
            this.redelivery = redelivery;
        }

        @OrdersEventListener
        void on(ReleaseReservation event) {
            attached(event.correlationId(), () -> inventory.release(event.requestId(), event.reservationId()));
        }

        @OrdersEventListener
        void on(VoidPayment event) {
            attached(event.correlationId(), () -> payments.voidAuthorization(event.requestId(), event.authorizationId()));
        }

        @OrdersEventListener
        void on(OrderConfirmed event) {
            attached(event.correlationId(), () -> notifications.send(event.requestId(), event.customerId(), event.template(),
                    Map.of("orderId", event.orderId(), "totalCents", Long.toString(event.totalCents()))));
        }

        /** The listener runs on another thread: carry the correlation id so its calls stay attached to the order. */
        private void attached(String correlationId, Runnable call) {
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            try {
                call.run();
            } catch (RuntimeException e) {
                redelivery.failed();                  // Modulith marks the publication failed; make sure it is looked at again
                throw e;
            } finally {
                MDC.remove("correlationId");
            }
        }
    }
}
