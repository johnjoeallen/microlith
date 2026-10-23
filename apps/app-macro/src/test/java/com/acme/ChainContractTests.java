package com.acme;

import com.acme.inventory.api.InventoryApi;
import com.acme.orders.api.OrdersApi;
import com.acme.catalog.api.CatalogApi;
import com.acme.customers.api.CustomersApi;
import com.acme.notifications.api.NotificationsApi;
import com.acme.payments.api.PaymentsApi;
import com.acme.pricing.api.PricingApi;
import com.acme.shipping.api.ShippingApi;
import io.macroservice.demo.scenarios.ApiContracts;
import io.macroservice.demo.scenarios.OrderScenarios;
import io.macroservice.platform.rpc.RpcClientProxy;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The interdependent chain orders -> shipping -> inventory -> catalog (plus customers, pricing, payments and
 * notifications), driven by the same scenarios through every binding. Results and exception types must be identical.
 */
class ChainContractTests {

    enum Mode { LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE }

    private final TestApps apps = new TestApps();

    @AfterEach
    void close() {
        apps.close();
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void everyScenarioGivesTheSameOutcome(Mode mode) throws IOException {
        List<ConfigurableApplicationContext> hosts = new ArrayList<>();
        ConfigurableApplicationContext caller;
        String[] all = TestApps.MODULES.toArray(String[]::new);
        switch (mode) {
            case LOCAL -> caller = apps.start(false, TestApps.allLocal());
            case LOOPBACK_CODEC -> caller = apps.start(false, TestApps.allLocal(), TestApps.binding("loopback-codec", TestApps.MODULES));
            case LOOPBACK_HTTP -> caller = apps.start(true, TestApps.allLocal(), TestApps.binding("loopback-http", TestApps.MODULES),
                    TestApps.args("--server.port=0"));
            case REMOTE -> {
                Map<String, Integer> ports = new LinkedHashMap<>();
                for (String module : TestApps.MODULES) {
                    ports.put(module, freePort());
                }
                Map<String, String> urls = new LinkedHashMap<>();
                ports.forEach((module, port) -> urls.put(module, "http://localhost:" + port + "/rpc/" + module));
                for (String module : TestApps.MODULES) {
                    hosts.add(apps.start(true, TestApps.demo(), TestApps.topology(urls, module),
                            io.macroservice.platform.support.testing.SharedPostgres.args(module), TestApps.args("--server.port=" + ports.get(module))));
                }
                caller = apps.start(false, TestApps.demo(), TestApps.topology(urls));
            }
            default -> throw new IllegalStateException();
        }
        List<ConfigurableApplicationContext> processes = mode == Mode.REMOTE ? hosts : List.of(caller);
        OrdersApi orders = caller.getBean(OrdersApi.class);
        assertThat(orders instanceof RpcClientProxy).isEqualTo(mode != Mode.LOCAL);
        new ApiContracts(caller.getBean(CatalogApi.class), caller.getBean(CustomersApi.class), caller.getBean(PricingApi.class),
                caller.getBean(InventoryApi.class), caller.getBean(PaymentsApi.class), caller.getBean(ShippingApi.class),
                caller.getBean(NotificationsApi.class)).runAll();
        new OrderScenarios(orders, caller.getBean(InventoryApi.class), ScenarioSupport.probe(processes), ScenarioSupport.faults(processes)).runAll();
    }
}
