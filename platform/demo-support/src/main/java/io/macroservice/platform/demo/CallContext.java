package io.macroservice.platform.demo;

/** Per-thread bookkeeping of the call trace. */
final class CallContext {

    static final String CORRELATION_KEY = "correlationId";

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> SERVER_ENTRY = ThreadLocal.withInitial(() -> false);

    private CallContext() {
    }

    static int depth() {
        return DEPTH.get();
    }

    static void depth(int depth) {
        if (depth == 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    /** Marks that the next call is the dispatcher invoking a local bean for a caller that already recorded the edge. */
    static void markServerEntry() {
        SERVER_ENTRY.set(true);
    }

    static boolean consumeServerEntry() {
        boolean entry = SERVER_ENTRY.get();
        SERVER_ENTRY.remove();
        return entry;
    }
}
