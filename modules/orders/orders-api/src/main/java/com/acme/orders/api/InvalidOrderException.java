package com.acme.orders.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 2002)
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
