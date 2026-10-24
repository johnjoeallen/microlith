package com.acme.shipping.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RpcApi("shipping")
public interface ShippingApi {

    @Idempotent
    @Operation(summary = "Quotes shipping for the lines")
    ShippingQuote quote(String customerId, List<ShipLine> lines);

    /** Idempotent by requestId. Throws {@link UndeliverableException}. */
    @Idempotent
    @Operation(summary = "Creates a shipment for a reservation")
    Shipment createShipment(String requestId, String orderId, String customerId, String reservationId);
}
