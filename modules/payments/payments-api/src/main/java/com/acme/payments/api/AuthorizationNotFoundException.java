package com.acme.payments.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 4003)
public class AuthorizationNotFoundException extends RuntimeException {

    public AuthorizationNotFoundException(String message) {
        super(message);
    }
}
