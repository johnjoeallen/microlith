package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.Parallel;
import java.util.Map;
import org.slf4j.MDC;

/** Registers the logging MDC (correlation id, traceparent) with {@link Parallel}, so a parallel call is logged and traced like a plain one. */
final class ParallelContext {

    ParallelContext() {
        Parallel.register("mdc", new Parallel.ContextPropagator() {
            @Override
            public Object capture() {
                return MDC.getCopyOfContextMap();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Runnable restore(Object captured) {
                Map<String, String> context = (Map<String, String>) captured;
                if (context != null) {
                    MDC.setContextMap(context);
                }
                return MDC::clear;
            }
        });
    }
}
