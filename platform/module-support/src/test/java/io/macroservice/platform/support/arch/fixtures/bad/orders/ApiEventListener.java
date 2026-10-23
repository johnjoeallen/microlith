package io.macroservice.platform.support.arch.fixtures.bad.orders;

import io.macroservice.platform.support.arch.fixtures.bad.inventory.api.StockEvent;
import org.springframework.context.event.EventListener;

public class ApiEventListener {
    @EventListener
    void on(StockEvent event) {
    }
}
