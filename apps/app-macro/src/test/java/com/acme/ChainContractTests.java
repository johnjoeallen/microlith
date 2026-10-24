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
import io.macroservice.platform.demo.TraceRecord;
import io.macroservice.platform.demo.TraceStore;
import org.slf4j.MDC;
import java.time.Duration;
import static org.awaitility.Awaitility.await;
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
        assertTraceShape(mode, orders, caller, processes);
    }

    /** One order, traced: the same calls in every mode, each reported with the binding that mode uses. */
    private static void assertTraceShape(Mode mode, OrdersApi orders, ConfigurableApplicationContext caller, List<ConfigurableApplicationContext> processes) {
        String correlationId = "shape-" + mode;
        MDC.put("correlationId", correlationId);
        try {
            orders.placeOrder("shape-req-" + mode, "cust-standard", List.of(new com.acme.orders.api.OrderLine("sku-widget", 1)));
        } finally {
            MDC.remove("correlationId");
        }
        List<ConfigurableApplicationContext> stores = new ArrayList<>(processes);
        if (!stores.contains(caller)) {
            stores.add(caller);
        }
        List<String> expected = List.of("orders>customers.getCustomer", "orders>pricing.quote", "pricing>customers.getCustomer", "pricing>catalog.getProduct",
                "orders>shipping.quote", "shipping>customers.getCustomer", "shipping>inventory.getStock", "inventory>catalog.productExists",
                "orders>inventory.reserve", "orders>payments.authorize", "payments>customers.getPaymentMethod", "orders>shipping.createShipment",
                "shipping>customers.getCustomer", "shipping>inventory.getReservation", "orders>payments.capture", "orders>notifications.send",
                "notifications>customers.getCustomer");
        String binding = switch (mode) {
            case LOCAL -> "local";
            case LOOPBACK_CODEC -> "loopback-codec";
            case LOOPBACK_HTTP -> "loopback-http";
            case REMOTE -> "remote";
        };
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<TraceRecord> records = stores.stream().flatMap(c -> c.getBean(TraceStore.class).find(correlationId).stream())
                    .filter(r -> !r.caller().equals("client")).toList();
            assertThat(records.stream().map(r -> r.caller() + ">" + r.callee() + "." + r.method()).sorted().toList())
                    .containsExactlyElementsOf(expected.stream().sorted().toList());
            assertThat(records).allSatisfy(r -> assertThat(r.binding()).as(r.line()).isIn(binding, "cache")); // "cache": a @Cached hit
        });
    }
}
