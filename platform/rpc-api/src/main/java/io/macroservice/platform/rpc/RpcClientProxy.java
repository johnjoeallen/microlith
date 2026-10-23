package io.macroservice.platform.rpc;

/**
 * Marker implemented by every generated client proxy, so that the server-side dispatcher and the startup validator
 * never mistake a proxy for a local implementation.
 */
public interface RpcClientProxy {
}
