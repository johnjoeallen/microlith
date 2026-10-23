package io.macroservice.platform.rpcstarter.fixtures;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 4242)
public class DeniedException extends RuntimeException {

    public DeniedException(String message) {
        super(message);
    }
}
