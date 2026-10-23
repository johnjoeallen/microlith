package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcException;
import io.macroservice.platform.support.ModuleProperties;
import io.macroservice.platform.support.ModuleSettings;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Per-module concurrency limit on the server side, sized from the module's {@code max-pool-size} (or
 * {@code bulkhead.max-concurrent-calls}). With virtual threads thousands of requests could otherwise queue on a pool
 * of a few connections. A call that cannot get a permit within {@code bulkhead.max-wait} is rejected with
 * {@link RpcErrorCodes#OVERLOADED} instead of timing out deep inside a transaction.
 */
public class ModuleBulkhead implements RpcServerInterceptor {

    private final ModuleProperties properties;
    private final Map<String, Semaphore> permits = new ConcurrentHashMap<>();

    public ModuleBulkhead(ModuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(String module, String method, Callable<Object> next) throws Exception {
        ModuleSettings settings = properties.of(module);
        int limit = limitOf(settings);
        Semaphore semaphore = permits.computeIfAbsent(module, key -> new Semaphore(limit, true));
        if (!semaphore.tryAcquire(settings.bulkhead().maxWait().toMillis(), TimeUnit.MILLISECONDS)) {
            throw new RpcException(RpcErrorCodes.OVERLOADED,
                    "Module '%s' is overloaded: more than %d concurrent calls, retry later".formatted(module, limit));
        }
        try {
            return next.call();
        } finally {
            semaphore.release();
        }
    }

    static int limitOf(ModuleSettings settings) {
        Integer configured = settings.bulkhead().maxConcurrentCalls();
        return configured != null ? configured : settings.datasource().maxPoolSize();
    }
}
