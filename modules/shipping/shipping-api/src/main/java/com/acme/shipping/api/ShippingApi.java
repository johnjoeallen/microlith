package com.acme.shipping.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import java.util.List;

@RpcApi("shipping")
public interface ShippingApi {

    @Idempotent
    ShippingQuote quote(String customerId, List<ShipLine> lines);

    /** Idempotent by requestId. Throws {@link UndeliverableException}. */
    @Idempotent
    Shipment createShipment(String requestId, String orderId, String customerId, String reservationId);
}
