package com.acme.orders.internal;

import io.macroservice.platform.support.ModuleComponent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;

/** {@code modules.orders.async=events}: the follow-up is a Spring Modulith event; the registry stores it in the current transaction. */
@ModuleComponent(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "events")
class EventFollowUps implements FollowUps {

    private final ApplicationEventPublisher events;

    EventFollowUps(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(Object followUp) {
        events.publishEvent(followUp);
    }
}
