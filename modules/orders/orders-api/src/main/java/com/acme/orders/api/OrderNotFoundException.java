package com.acme.orders.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 2001)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
