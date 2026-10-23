package com.acme.payments.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 4001)
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
