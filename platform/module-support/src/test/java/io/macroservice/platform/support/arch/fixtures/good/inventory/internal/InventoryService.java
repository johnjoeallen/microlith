package io.macroservice.platform.support.arch.fixtures.good.inventory.internal;

import io.macroservice.platform.support.ModuleService;
import io.macroservice.platform.support.arch.fixtures.good.inventory.api.InventoryApi;

@ModuleService(module = "inventory")
public class InventoryService implements InventoryApi {
    public int stock(String sku) {
        return 1;
    }
}
