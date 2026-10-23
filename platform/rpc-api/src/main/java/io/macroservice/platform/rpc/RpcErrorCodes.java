package io.macroservice.platform.rpc;

/** JSON-RPC 2.0 reserved error codes plus the codes this platform uses in the implementation-defined range. */
public final class RpcErrorCodes {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    /** The target module could not be reached or timed out (client side only). */
    public static final int UNAVAILABLE = -32001;
    /** The concurrency limit of the target module was exceeded (server side). */
    public static final int OVERLOADED = -32002;

    private RpcErrorCodes() {
    }
}
