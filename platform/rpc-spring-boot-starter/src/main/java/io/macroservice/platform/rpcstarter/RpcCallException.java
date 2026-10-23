package io.macroservice.platform.rpcstarter;

import tools.jackson.databind.JsonNode;

/** A failed call as the JSON-RPC error it maps to. */
public class RpcCallException extends RuntimeException {

    private final int code;
    private final transient JsonNode data;

    public RpcCallException(int code, String message, JsonNode data) {
        super(message, null, false, false);
        this.code = code;
        this.data = data;
    }

    public int code() {
        return code;
    }

    /** Optional structured detail; null when absent. */
    public JsonNode data() {
        return data;
    }
}
