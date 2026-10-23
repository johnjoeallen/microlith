package com.acme.orders.internal;

import io.macroservice.platform.support.ModuleComponent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process hint that outbox rows were just committed. It lets the relay react at once without polling the database
 * all the time, so an idle orders module holds no connection.
 */
@ModuleComponent(module = "orders")
public class OutboxSignal {

    private final AtomicBoolean pending = new AtomicBoolean();

    void raise() {
        pending.set(true);
    }

    boolean consume() {
        return pending.getAndSet(false);
    }
}
