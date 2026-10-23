package com.acme.inventory.api;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("inventory")
public interface InventoryApi {

    StockLevel getStock(String sku);

    /** Reserves stock; idempotent by {@link ReserveRequest#requestId()}. Throws {@link OutOfStockException}. */
    Reservation reserve(ReserveRequest request);
}
