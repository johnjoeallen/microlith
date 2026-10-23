package io.macroservice.platform.rpcstarter;

import java.util.concurrent.Callable;

/**
 * Wraps every dispatched call on the server side (bulkhead, metrics, tracing). Implementations call
 * {@code next.call()} to proceed and may throw an {@link io.macroservice.platform.rpc.RpcException} to reject the call.
 */
public interface RpcServerInterceptor {

    Object intercept(String module, String method, Callable<Object> next) throws Exception;
}
