package io.macroservice.platform.demo;

import io.macroservice.platform.rpcstarter.RpcHeaderPropagator;
import java.util.Map;

/** Tells the next process how deep in the call tree its work starts. */
class TraceHeaderPropagator implements RpcHeaderPropagator {

    static final String DEPTH_HEADER = "X-Rpc-Depth";

    @Override
    public void contribute(Map<String, String> headers) {
        headers.put(DEPTH_HEADER, Integer.toString(CallContext.depth()));
    }
}
