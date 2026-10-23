package com.acme.customers.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 3101)
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
