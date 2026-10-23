package io.macroservice.platform.rpcstarter;

import java.util.Map;

/**
 * Extension point: contributes headers (trace ids, the caller's principal, service tokens) to every outgoing
 * remote call. Declare a bean of this type to add your own.
 */
public interface RpcHeaderPropagator {

    void contribute(Map<String, String> headers);
}
