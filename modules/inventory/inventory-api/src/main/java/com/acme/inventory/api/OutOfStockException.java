package com.acme.inventory.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 1001)
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String message) {
        super(message);
    }
}
