package io.macroservice.platform.support.arch.fixtures.good.orders;

import io.macroservice.platform.support.ModuleService;
import io.macroservice.platform.support.arch.fixtures.good.inventory.api.InventoryApi;
import org.springframework.context.event.EventListener;

@ModuleService(module = "orders")
public class OrdersService {

    public record OwnEvent(String id) {
    }

    private final InventoryApi inventory;

    public OrdersService(InventoryApi inventory) {
        this.inventory = inventory;
    }

    @EventListener
    void on(OwnEvent event) {
        inventory.stock(event.id());
    }
}
