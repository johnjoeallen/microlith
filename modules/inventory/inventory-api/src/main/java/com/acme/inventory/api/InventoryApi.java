package com.acme.inventory.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import java.util.List;

@RpcApi("inventory")
public interface InventoryApi {

    /** Stock of a product; the SKU is validated through the catalog. */
    @Idempotent
    StockLevel getStock(String sku);

    /** Reserves all lines or none; idempotent by requestId. Throws {@link OutOfStockException}. */
    @Idempotent
    Reservation reserve(String requestId, String orderId, List<ReserveLine> lines);

    /** Gives a reservation's stock back; releasing a released reservation does nothing. */
    @Idempotent
    void release(String requestId, String reservationId);

    @Idempotent
    Reservation getReservation(String reservationId);
}
