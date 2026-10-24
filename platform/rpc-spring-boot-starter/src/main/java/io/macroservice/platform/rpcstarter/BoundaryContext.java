package io.macroservice.platform.rpcstarter;

/** Tells the {@link BoundaryInterceptor} that the next call is the dispatcher serving a call another proxy recorded. */
final class BoundaryContext {

    private static final ThreadLocal<Boolean> SERVER_ENTRY = ThreadLocal.withInitial(() -> false);

    private BoundaryContext() {
    }

    static void markServerEntry() {
        SERVER_ENTRY.set(true);
    }

    static boolean consumeServerEntry() {
        boolean entry = SERVER_ENTRY.get();
        SERVER_ENTRY.remove();
        return entry;
    }
}
