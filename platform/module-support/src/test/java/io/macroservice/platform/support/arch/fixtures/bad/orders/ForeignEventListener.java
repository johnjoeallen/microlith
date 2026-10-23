package io.macroservice.platform.support.arch.fixtures.bad.orders;

import io.macroservice.platform.support.arch.fixtures.bad.inventory.internal.InternalEvent;
import org.springframework.context.event.EventListener;

public class ForeignEventListener {
    @EventListener
    void on(InternalEvent event) {
    }
}
