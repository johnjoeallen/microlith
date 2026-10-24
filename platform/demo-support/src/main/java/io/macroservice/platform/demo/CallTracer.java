package io.macroservice.platform.demo;

import io.macroservice.platform.rpc.CacheSignals;
import io.macroservice.platform.rpc.RpcApi;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Wraps one {@code @RpcApi} bean (a local implementation or a client proxy). Every call is recorded with the module
 * that made it, the module it reached, the binding used, its depth and duration. The caller is found from the call
 * stack ({@code com.acme.<module>}), which also attributes calls made by background threads such as the outbox relay.
 */
final class CallTracer implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger("macro.trace");
    private static final Pattern MODULE_FRAME = Pattern.compile("^com\\.acme\\.([a-z][a-z0-9]*)\\..*");

    private final Object target;
    private final String module;
    private final String binding;
    private final TraceStore store;

    CallTracer(Object target, String module, String binding, TraceStore store) {
        this.target = target;
        this.module = module;
        this.binding = binding;
        this.store = store;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class || !method.getDeclaringClass().isAnnotationPresent(RpcApi.class)) {
            return call(method, args);
        }
        boolean serverEntry = CallContext.consumeServerEntry();
        boolean generated = false;
        if (MDC.get(CallContext.CORRELATION_KEY) == null && module.equals("orders") && method.getName().equals("placeOrder")) {
            MDC.put(CallContext.CORRELATION_KEY, UUID.randomUUID().toString());
            generated = true;
        }
        String correlationId = MDC.get(CallContext.CORRELATION_KEY);
        int depth = CallContext.depth();
        String caller = serverEntry ? null : callerModule();
        long wallStart = System.currentTimeMillis() * 1000;
        long started = System.nanoTime();
        String outcome = "ok";
        String used = binding;
        try {
            if (!serverEntry) {
                CallContext.depth(depth + 1);
            }
            CacheSignals.consumeHit();
            Object result = call(method, args);
            if (CacheSignals.consumeHit()) {
                used = "cache";                      // answered by the @Cached layer, no module call was made
            }
            return result;
        } catch (Throwable failure) {
            outcome = "error:" + failure.getClass().getSimpleName();
            throw failure;
        } finally {
            if (!serverEntry) {
                CallContext.depth(depth);
                if (correlationId != null) {
                    TraceRecord record = new TraceRecord(correlationId, wallStart, caller, module, method.getName(), used, depth,
                            (System.nanoTime() - started) / 1_000_000.0, outcome);
                    store.add(record);
                    log.info("{}", record.line());
                }
            }
            if (generated) {
                MDC.remove(CallContext.CORRELATION_KEY);
            }
        }
    }

    private Object call(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static String callerModule() {
        return StackWalker.getInstance().walk(frames -> frames.map(StackWalker.StackFrame::getClassName).map(MODULE_FRAME::matcher)
                .filter(Matcher::matches).map(m -> m.group(1)).findFirst()).orElse("client");
    }
}
