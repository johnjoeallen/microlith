package io.macroservice.platform.rpcstarter;

import java.util.concurrent.Callable;

/** Marks dispatcher-made calls for the {@link BoundaryInterceptor}; clears the mark if the bean was not wrapped. */
class BoundaryServerInterceptor implements RpcServerInterceptor {

    @Override
    public Object intercept(String module, String method, Callable<Object> next) throws Exception {
        BoundaryContext.markServerEntry();
        try {
            return next.call();
        } finally {
            BoundaryContext.consumeServerEntry();
        }
    }
}
