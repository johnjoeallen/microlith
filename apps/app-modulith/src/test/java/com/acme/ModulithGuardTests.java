package com.acme;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** app-modulith is the plain modular monolith: any shape other than "every module local" fails at startup. */
class ModulithGuardTests {

    private static void start(String... args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.NONE).run(args).close();
    }

    @Test
    void aRemoteBindingIsRejected() {
        assertThatThrownBy(() -> start("--modules.inventory.binding=remote", "--modules.inventory.url=http://localhost:9/rpc/inventory"))
                .hasMessageContaining("app-modulith runs every module locally").hasMessageContaining("module 'inventory' has binding remote");
    }

    @Test
    void loopbackBindingsAreRejected() {
        assertThatThrownBy(() -> start("--modules.orders.binding=loopback-codec")).hasMessageContaining("module 'orders' has binding loopback-codec");
        assertThatThrownBy(() -> start("--modules.pricing.binding=loopback-http")).hasMessageContaining("module 'pricing' has binding loopback-http");
    }

    @Test
    void aDisabledModuleIsRejected() {
        assertThatThrownBy(() -> start("--modules.catalog.enabled=false", "--modules.catalog.url=http://localhost:9/rpc/catalog"))
                .hasMessageContaining("module 'catalog' is not enabled");
    }
}
