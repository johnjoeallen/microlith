package com.acme.orders.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.macroservice.platform.support.ModuleConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches on scheduling (the redelivery of failed events) only when the orders module is enabled. */
@ModuleConfiguration(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "events")
@EnableScheduling
class OrdersEventsConfiguration {
}
