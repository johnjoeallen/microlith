package io.macroservice.platform.rpcstarter;

import java.util.Map;

/** Moves one serialized JSON-RPC request to the target module and returns the serialized response. */
public interface RpcTransport {

    /**
     * @return the response body, or null when the server sent none (notification)
     * @throws io.macroservice.platform.rpc.RpcUnavailableException when the target cannot be reached or times out
     */
    String send(String module, String requestJson, Map<String, String> headers);
}
