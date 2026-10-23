package io.macroservice.platform.rpc;

/** A request or response was malformed. */
public class RpcProtocolException extends RpcException {

    public RpcProtocolException(String message) {
        super(RpcErrorCodes.INVALID_REQUEST, message);
    }

    public RpcProtocolException(String message, Throwable cause) {
        super(RpcErrorCodes.INVALID_REQUEST, message, cause);
    }

    public RpcProtocolException(int code, String message) {
        super(code, message);
    }
}
