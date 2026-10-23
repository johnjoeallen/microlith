package com.acme.inventory.api;

import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;

@RpcApi("inventory")
public interface InventoryApi {

    @Idempotent
    StockLevel getStock(String sku);

    /** Reserves stock; idempotent by {@link ReserveRequest#requestId()}. Throws {@link OutOfStockException}. */
    @Idempotent
    Reservation reserve(ReserveRequest request);
}
