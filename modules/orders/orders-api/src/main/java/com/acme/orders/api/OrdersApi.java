package com.acme.orders.api;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("orders")
public interface OrdersApi {

    /** Idempotent by {@link PlaceOrderRequest#requestId()}. */
    Order placeOrder(PlaceOrderRequest request);

    Order getOrder(String orderId);
}
