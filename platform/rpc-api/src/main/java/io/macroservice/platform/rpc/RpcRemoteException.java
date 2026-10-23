package io.macroservice.platform.rpc;

/** The remote module failed with an unexpected server error. */
public class RpcRemoteException extends RpcException {

    public RpcRemoteException(String message) {
        super(RpcErrorCodes.INTERNAL_ERROR, message);
    }

    public RpcRemoteException(String message, Throwable cause) {
        super(RpcErrorCodes.INTERNAL_ERROR, message, cause);
    }

    public RpcRemoteException(int code, String message) {
        super(code, message);
    }
}
