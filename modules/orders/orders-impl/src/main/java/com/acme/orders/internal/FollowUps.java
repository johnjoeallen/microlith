package com.acme.orders.internal;

/**
 * What the orders module still has to tell other modules after an order changed state (release a reservation, void a
 * payment, confirm to the customer). It is called inside the transaction that changes the order and must make the follow-up
 * durable there; delivery happens after the commit. {@code modules.orders.async} picks how: {@code outbox} (a row in the
 * module's outbox table, delivered by {@link OutboxRelay}) or {@code events} (a Spring Modulith event, stored by the event
 * publication registry and delivered by an asynchronous listener).
 */
interface FollowUps {

    /** One of {@link OrderEvents.ReleaseReservation}, {@link OrderEvents.VoidPayment} or {@link OrderEvents.OrderConfirmed}. */
    void publish(Object followUp);
}
