package com.acme.orders.internal;

import io.macroservice.platform.support.ModuleConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches on scheduling only when the orders module is enabled, so the relay never runs in a process without it. */
@ModuleConfiguration(module = "orders")
@EnableScheduling
class OrdersOutboxConfiguration {
}
