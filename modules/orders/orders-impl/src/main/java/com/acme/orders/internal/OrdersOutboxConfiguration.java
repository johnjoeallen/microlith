package com.acme.orders.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.macroservice.platform.support.ModuleConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches on scheduling only when the orders module is enabled, so the relay never runs in a process without it. */
@ModuleConfiguration(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "outbox", matchIfMissing = true)
@EnableScheduling
class OrdersOutboxConfiguration {
}
