package io.macroservice.platform.support;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of one module, bound from {@code modules.<name>.*}.
 *
 * @param enabled    whether this process runs the module
 * @param url        JSON-RPC endpoint of the module when it runs elsewhere
 * @param binding    explicit binding; when null the registrar picks local or remote
 * @param datasource the module's own database settings; empty means "use spring.datasource"
 */
public record ModuleSettings(boolean enabled, String url, Binding binding, @DefaultValue Datasource datasource) {

    public enum Binding {
        LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE
    }

    public record Datasource(String url, String username, String password, String schema) {

        public boolean hasUrl() {
            return url != null && !url.isBlank();
        }
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    static ModuleSettings absent() {
        return new ModuleSettings(false, null, null, new Datasource(null, null, null, null));
    }
}
