package io.macroservice.platform.rpc;

/**
 * Per-thread hand-over between the {@link Cached} layer and whoever observes calls around it (the demo call tracer):
 * the layer reports a hit, the observer reads it right after the call; the dispatcher asks the layer to be skipped for
 * the one call it makes on behalf of a remote caller (that caller has its own cache).
 */
public final class CacheSignals {

    private static final ThreadLocal<Boolean> HIT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASS_NEXT = new ThreadLocal<>();

    private CacheSignals() {
    }

    public static void markHit() {
        HIT.set(Boolean.TRUE);
    }

    /** True if the call that just returned on this thread was answered from a cache; resets the signal. */
    public static boolean consumeHit() {
        boolean hit = HIT.get() != null;
        HIT.remove();
        return hit;
    }

    public static void bypassNext() {
        BYPASS_NEXT.set(Boolean.TRUE);
    }

    public static boolean consumeBypass() {
        boolean bypass = BYPASS_NEXT.get() != null;
        BYPASS_NEXT.remove();
        return bypass;
    }
}
