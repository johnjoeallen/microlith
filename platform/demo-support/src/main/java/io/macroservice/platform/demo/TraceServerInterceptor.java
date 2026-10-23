package io.macroservice.platform.demo;

import io.macroservice.platform.rpcstarter.RpcServerInterceptor;
import java.util.concurrent.Callable;

/**
 * The dispatcher invokes a local bean for a caller that already recorded the edge (on its own side), so the traced
 * wrapper must not record it a second time.
 */
class TraceServerInterceptor implements RpcServerInterceptor {

    @Override
    public Object intercept(String module, String method, Callable<Object> next) throws Exception {
        CallContext.markServerEntry();
        try {
            return next.call();
        } finally {
            CallContext.consumeServerEntry();
        }
    }
}
