package io.macroservice.platform.rpc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Runs independent calls to other modules at the same time, each on its own virtual thread, and hands the results back in
 * program order:
 *
 * <pre>
 * Pending&lt;Quote&gt; quote = Parallel.supply(() -&gt; pricing.quote(...));
 * Pending&lt;ShippingQuote&gt; delivery = Parallel.supply(() -&gt; shipping.quote(...));
 * Quote q = quote.join();            // a failure is thrown here, unwrapped, exactly as a plain call would throw it
 * ShippingQuote d = delivery.join();
 * </pre>
 *
 * Joining in the order the calls would have been made sequentially keeps the exception a caller sees the same as before.
 * Thread-bound context (logging MDC, the demo call trace) is carried into the worker by the registered {@link ContextPropagator}s.
 * Only use it for calls that do not depend on each other and are safe to repeat or abandon (reads).
 */
public final class Parallel {

    private static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Map<String, ContextPropagator> PROPAGATORS = new ConcurrentHashMap<>();

    private Parallel() {
    }

    /** Carries one kind of thread-bound state from the thread that calls {@link #supply} into the worker thread. */
    public interface ContextPropagator {

        /** Called on the calling thread; returns whatever {@link #restore} needs. */
        Object capture();

        /** Called on the worker thread before the task; returns the action that undoes it afterwards. */
        Runnable restore(Object captured);
    }

    /** Registers a propagator under a name; registering the same name again replaces it. */
    public static void register(String name, ContextPropagator propagator) {
        PROPAGATORS.put(name, propagator);
    }

    public static <T> Pending<T> supply(Supplier<T> call) {
        List<ContextPropagator> propagators = List.copyOf(PROPAGATORS.values());
        Object[] captured = new Object[propagators.size()];
        for (int i = 0; i < captured.length; i++) {
            captured[i] = propagators.get(i).capture();
        }
        return new Pending<>(CompletableFuture.supplyAsync(() -> {
            Runnable[] undo = new Runnable[captured.length];
            for (int i = 0; i < undo.length; i++) {
                undo[i] = propagators.get(i).restore(captured[i]);
            }
            try {
                return call.get();
            } finally {
                for (Runnable action : undo) {
                    action.run();
                }
            }
        }, EXECUTOR));
    }

    /** The result of a call that is running on another thread. */
    public static final class Pending<T> {

        private final CompletableFuture<T> future;

        private Pending(CompletableFuture<T> future) {
            this.future = future;
        }

        /** Waits for the call; a failure is rethrown as the exception the call itself threw. */
        public T join() {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for a parallel call", e);
            } catch (ExecutionException | CompletionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(cause);
            }
        }
    }
}
