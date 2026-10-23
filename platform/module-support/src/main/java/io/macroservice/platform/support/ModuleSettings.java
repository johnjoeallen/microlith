package io.macroservice.platform.support;

import java.time.Duration;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of one module, bound from {@code modules.<name>.*}.
 *
 * @param enabled    whether this process runs the module
 * @param url        JSON-RPC endpoint of the module when it runs elsewhere
 * @param binding    explicit binding; when null the registrar picks local or remote
 * @param datasource the module's own database settings; empty means "use spring.datasource"
 * @param client     timeouts and retry policy used when this module is called through a client proxy
 */
public record ModuleSettings(boolean enabled, String url, Binding binding, @DefaultValue Datasource datasource,
        @DefaultValue Client client) {

    public enum Binding {
        LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE
    }

    public record Datasource(String url, String username, String password, String schema) {

        public boolean hasUrl() {
            return url != null && !url.isBlank();
        }
    }

    /**
     * @param retries how often an {@code @Idempotent} call is repeated after an unavailable/timeout failure (0 = off)
     */
    public record Client(@DefaultValue("2s") Duration connectTimeout, @DefaultValue("10s") Duration readTimeout,
            @DefaultValue("0") int retries, @DefaultValue("200ms") Duration retryBackoff) {
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    static ModuleSettings absent() {
        return new ModuleSettings(false, null, null, new Datasource(null, null, null, null),
                new Client(Duration.ofSeconds(2), Duration.ofSeconds(10), 0, Duration.ofMillis(200)));
    }
}
