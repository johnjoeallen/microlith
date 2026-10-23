package com.acme.inventory.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 1002)
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
