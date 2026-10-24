package com.acme.orders.internal;

import io.macroservice.platform.support.ModuleComponent;
import io.macroservice.platform.support.ModuleConfigurationException;
import org.springframework.core.env.Environment;

/** Fails at start-up, with a clear message, when {@code modules.orders.async} is neither {@code outbox} nor {@code events}. */
@ModuleComponent(module = "orders")
class AsyncModeCheck {

    AsyncModeCheck(Environment environment) {
        String mode = environment.getProperty("modules.orders.async", "outbox");
        if (!mode.equals("outbox") && !mode.equals("events")) {
            throw new ModuleConfigurationException("modules.orders.async must be 'outbox' or 'events' but was '%s'".formatted(mode));
        }
    }
}
