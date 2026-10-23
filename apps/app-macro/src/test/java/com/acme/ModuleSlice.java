package com.acme;

import io.macroservice.platform.support.testing.SharedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Properties for a module slice test: only {@code module} runs (with its own schema on the shared PostgreSQL); every
 * other module is disabled, so its API is served by a client proxy that the test replaces with a mock.
 */
public final class ModuleSlice {

    private ModuleSlice() {
    }

    public static void properties(DynamicPropertyRegistry registry, String module) {
        for (String other : TestApps.MODULES) {
            if (other.equals(module)) {
                registry.add("modules." + other + ".enabled", () -> "true");
            } else {
                registry.add("modules." + other + ".enabled", () -> "false");
                registry.add("modules." + other + ".url", () -> "http://127.0.0.1:1/rpc/" + other);
            }
        }
        SharedPostgres.register(registry, module);
    }
}
