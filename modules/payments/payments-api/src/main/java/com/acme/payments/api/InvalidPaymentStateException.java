package com.acme.payments.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 4002)
public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
