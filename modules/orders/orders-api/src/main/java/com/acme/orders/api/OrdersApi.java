package com.acme.orders.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RpcApi("orders")
public interface OrdersApi {

    /**
     * Runs the order saga; idempotent by requestId (a repeated call resumes or returns the stored outcome).
     * Throws {@link com.acme.inventory.api.OutOfStockException} when stock is missing.
     */
    @Idempotent
    @Operation(summary = "Places an order (runs the order saga)")
    Order placeOrder(String requestId, String customerId, List<OrderLine> lines);

    @Idempotent
    @Operation(summary = "Gets an order and its status")
    Order getOrder(String orderId);
}
