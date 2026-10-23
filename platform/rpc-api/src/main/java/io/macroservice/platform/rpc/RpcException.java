package io.macroservice.platform.rpc;

/** Base class of all platform-level RPC failures. Business failures use their own {@link RpcError} exceptions. */
public class RpcException extends RuntimeException {

    private final int code;

    public RpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public RpcException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
