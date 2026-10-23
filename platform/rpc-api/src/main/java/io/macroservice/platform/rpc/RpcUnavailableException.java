package io.macroservice.platform.rpc;

/** The remote module is unreachable or the call timed out. */
public class RpcUnavailableException extends RpcException {

    public RpcUnavailableException(String message) {
        super(RpcErrorCodes.UNAVAILABLE, message);
    }

    public RpcUnavailableException(String message, Throwable cause) {
        super(RpcErrorCodes.UNAVAILABLE, message, cause);
    }

    public RpcUnavailableException(int code, String message) {
        super(code, message);
    }
}
