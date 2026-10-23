package com.acme.shipping.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 5001)
public class UndeliverableException extends RuntimeException {

    public UndeliverableException(String message) {
        super(message);
    }
}
