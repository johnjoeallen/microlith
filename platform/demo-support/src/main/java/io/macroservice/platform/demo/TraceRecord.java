package io.macroservice.platform.demo;

/**
 * One cross-module call, recorded on the caller's side.
 *
 * @param startedAtMicros wall-clock start, so records from several processes can be merged in order
 * @param binding         the binding actually used: local, loopback-codec, loopback-http or remote
 * @param depth           0 for a call made by a module's own code, +1 for each enclosing call
 * @param outcome         {@code ok} or {@code error:<exception class>}
 */
public record TraceRecord(String correlationId, long startedAtMicros, String caller, String callee, String method,
        String binding, int depth, double durationMs, String outcome) {

    /** The one-line form that is also logged, for example {@code pricing -> catalog.getProduct [local] 1 ms ok}. */
    public String line() {
        return "%s -> %s.%s [%s] %d ms %s".formatted(caller, callee, method, binding, Math.round(durationMs), outcome);
    }
}
