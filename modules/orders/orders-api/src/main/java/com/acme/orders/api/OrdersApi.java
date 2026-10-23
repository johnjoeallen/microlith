package com.acme.orders.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import java.util.List;

@RpcApi("orders")
public interface OrdersApi {

    /**
     * Runs the order saga; idempotent by requestId (a repeated call resumes or returns the stored outcome).
     * Throws {@link com.acme.inventory.api.OutOfStockException} when stock is missing.
     */
    @Idempotent
    Order placeOrder(String requestId, String customerId, List<OrderLine> lines);

    @Idempotent
    Order getOrder(String orderId);
}
