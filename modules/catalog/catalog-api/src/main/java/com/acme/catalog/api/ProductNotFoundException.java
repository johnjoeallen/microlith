package com.acme.catalog.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 3001)
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
