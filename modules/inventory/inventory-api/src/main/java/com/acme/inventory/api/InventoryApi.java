package com.acme.inventory.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RpcApi("inventory")
public interface InventoryApi {

    /** Stock of a product; the SKU is validated through the catalog. */
    @Idempotent
    @Operation(summary = "Gets the stock level of a SKU")
    StockLevel getStock(String sku);

    /** Reserves all lines or none; idempotent by requestId. Throws {@link OutOfStockException}. */
    @Idempotent
    @Operation(summary = "Reserves stock for all lines or none")
    Reservation reserve(String requestId, String orderId, List<ReserveLine> lines);

    /** Gives a reservation's stock back; releasing a released reservation does nothing. */
    @Idempotent
    @Operation(summary = "Gives a reservation's stock back")
    void release(String requestId, String reservationId);

    @Idempotent
    @Operation(summary = "Gets a reservation")
    Reservation getReservation(String reservationId);
}
