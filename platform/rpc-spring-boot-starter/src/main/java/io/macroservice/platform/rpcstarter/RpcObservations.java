package io.macroservice.platform.rpcstarter;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.concurrent.Callable;

/**
 * One observation per cross-module call, named {@code macro.rpc.client} and tagged identically whatever the binding
 * (local, loopback-codec, loopback-http, remote), so dashboards and traces do not change when a module moves.
 * With a metrics handler registered (the actuator default) it is a timer; with a tracing bridge it is a span.
 */
final class RpcObservations {

    static final String NAME = "macro.rpc.client";

    private RpcObservations() {
    }

    static <T> T observe(ObservationRegistry registry, String module, String method, String binding, Callable<T> call) throws Exception {
        Observation observation = Observation.createNotStarted(NAME, registry)
                .contextualName(module + "." + method)
                .lowCardinalityKeyValue("module", module)
                .lowCardinalityKeyValue("method", method)
                .lowCardinalityKeyValue("binding", binding)
                .start();
        String outcome = "ok";
        try (Observation.Scope scope = observation.openScope()) {
            return call.call();
        } catch (Exception | Error failure) {
            outcome = "error";
            observation.error(failure);
            throw failure;
        } finally {
            observation.lowCardinalityKeyValue("outcome", outcome);
            observation.stop();
        }
    }
}
