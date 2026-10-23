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
 * @param bulkhead   concurrency limit applied when this process serves the module over JSON-RPC
 */
public record ModuleSettings(boolean enabled, String url, Binding binding, @DefaultValue Datasource datasource,
        @DefaultValue Client client,
        @DefaultValue Bulkhead bulkhead) {

    public enum Binding {
        LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE
    }

    /**
     * @param url               JDBC URL; empty means "use spring.datasource.url" (same instance, different schema)
     * @param schema            the module's schema; defaults to the module name
     * @param migrationUsername DDL-capable role for Flyway; falls back to the runtime username
     * @param maxPoolSize       Hikari maximum pool size; the unit of the connection budget
     * @param minimumIdle       connections kept open while idle; 0 means an idle module holds none
     */
    public record Datasource(String url, String username, String password, String schema, String migrationUsername,
            String migrationPassword, @DefaultValue("3") int maxPoolSize, @DefaultValue("0") int minimumIdle,
            @DefaultValue("30s") Duration idleTimeout, @DefaultValue("10m") Duration maxLifetime,
            @DefaultValue("5s") Duration connectionTimeout) {

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

    /**
     * @param maxConcurrentCalls simultaneous server-side calls; defaults to the datasource's max-pool-size
     * @param maxWait            how long a call waits for a permit before it is rejected
     */
    public record Bulkhead(Integer maxConcurrentCalls, @DefaultValue("1s") Duration maxWait) {
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    static ModuleSettings absent() {
        return new ModuleSettings(false, null, null, new Datasource(null, null, null, null, null, null, 3, 0, Duration.ofSeconds(30), Duration.ofMinutes(10),
                        Duration.ofSeconds(5)),
                new Client(Duration.ofSeconds(2), Duration.ofSeconds(10), 0, Duration.ofMillis(200)),
                new Bulkhead(null, Duration.ofSeconds(1)));
    }
}
