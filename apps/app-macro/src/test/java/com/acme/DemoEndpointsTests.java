package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import io.macroservice.platform.demo.TraceRecord;
import io.macroservice.platform.demo.TraceStore;
import com.acme.orders.api.Order;
import com.acme.orders.api.OrderLine;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.awaitility.Awaitility.await;

/** The demo-profile helpers: /demo/topology, /demo/trace with correlation, /demo/faults. */
class DemoEndpointsTests {

    private final TestApps apps = new TestApps();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @AfterEach
    void close() {
        apps.close();
    }

    private RestClient client(ConfigurableApplicationContext context) {
        return RestClient.builder().baseUrl("http://localhost:" + context.getEnvironment().getProperty("local.server.port")).build();
    }

    private JsonNode get(RestClient client, String path) {
        return mapper.readTree(client.get().uri(path).retrieve().body(String.class));
    }

    @Test
    void topologyShowsEnabledModulesBindingsAndDatabasesButNoPassword() {
        ConfigurableApplicationContext context = apps.start(true, TestApps.demo(), TestApps.topology(Map.of("catalog", "http://localhost:9/rpc/catalog"), "orders", "notifications"),
                io.macroservice.platform.support.testing.SharedPostgres.args("orders", "notifications"), TestApps.args("--server.port=0"));
        String text = client(context).get().uri("/demo/topology").retrieve().body(String.class);
        JsonNode topology = mapper.readTree(text);
        assertThat(topology.get("modules")).hasSize(8);
        JsonNode orders = module(topology, "orders");
        assertThat(orders.get("enabled").asBoolean()).isTrue();
        assertThat(orders.get("binding").stringValue()).isEqualTo("local");
        assertThat(orders.get("datasource").get("port").asInt()).isPositive();
        assertThat(orders.get("datasource").get("schema").stringValue()).startsWith("orders_");
        JsonNode catalog = module(topology, "catalog");
        assertThat(catalog.get("enabled").asBoolean()).isFalse();
        assertThat(catalog.get("binding").stringValue()).isEqualTo("remote");
        assertThat(catalog.get("url").stringValue()).isEqualTo("http://localhost:9/rpc/catalog");
        assertThat(catalog.get("datasource").isNull()).isTrue();
        assertThat(text).doesNotContain("password").doesNotContain("_app").doesNotContain("test");
    }

    private static JsonNode module(JsonNode topology, String name) {
        for (JsonNode module : topology.get("modules")) {
            if (module.get("module").stringValue().equals(name)) {
                return module;
            }
        }
        throw new AssertionError("no module " + name);
    }

    @Test
    void anOrderIsTracedUnderItsCorrelationId() {
        ConfigurableApplicationContext context = apps.start(true, TestApps.allLocal(), TestApps.args("--server.port=0"));
        MDC.put("correlationId", "trace-test-1");
        Order order;
        try {
            order = context.getBean(OrdersApi.class).placeOrder("trace-req-1", "cust-standard", List.of(new OrderLine("sku-widget", 1)));
        } finally {
            MDC.remove("correlationId");
        }
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);

        RestClient http = client(context);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode records = get(http, "/demo/trace?correlationId=trace-test-1");
            assertThat(records.toString()).contains("\"callee\":\"notifications\"");
        });
        JsonNode records = get(http, "/demo/trace?correlationId=trace-test-1");
        assertThat(get(http, "/demo/trace?correlationId=nobody")).isEmpty();
        boolean chain = false;
        for (JsonNode r : records) {
            assertThat(r.get("binding").stringValue()).isEqualTo("local");
            assertThat(r.get("outcome").stringValue()).isEqualTo("ok");
            chain |= r.get("caller").stringValue().equals("inventory") && r.get("callee").stringValue().equals("catalog")
                    && r.get("method").stringValue().equals("productExists")
                    && r.get("depth").asInt() == 3; // the test calls orders directly, so that entry call is one more level
        }
        assertThat(chain).as("orders -> shipping -> inventory -> catalog is nested three calls deep below the entry call").isTrue();
        TraceStore store = context.getBean(TraceStore.class);
        List<TraceRecord> stored = store.find("trace-test-1");
        assertThat(stored).isNotEmpty();
        assertThat(stored.getFirst().line()).matches("client -> orders\\.placeOrder \\[local\\] \\d+ ms ok");
    }

    @Test
    void faultsApplyOnlyToModulesEnabledInThisProcess() {
        ConfigurableApplicationContext context = apps.start(true, TestApps.demo(), TestApps.topology(Map.of(), "notifications", "customers"),
                io.macroservice.platform.support.testing.SharedPostgres.args("notifications", "customers"), TestApps.args("--server.port=0"));
        RestClient http = client(context);
        JsonNode result = mapper.readTree(http.post().uri("/demo/faults").contentType(MediaType.APPLICATION_JSON)
                .body("{\"notificationsDown\":true,\"paymentsDecline\":true,\"inventoryLatencyMs\":250,\"nonsense\":1}").retrieve().body(String.class));
        assertThat(result.get("applied").toString()).contains("notificationsDown").doesNotContain("paymentsDecline");
        assertThat(result.get("ignored").get("paymentsDecline").stringValue()).contains("not enabled in this process");
        assertThat(result.get("ignored").get("inventoryLatencyMs").stringValue()).contains("not enabled in this process");
        assertThat(result.get("ignored").get("nonsense").stringValue()).isEqualTo("unknown switch");
        assertThat(get(http, "/demo/faults").get("state").get("notifications.down").asBoolean()).isTrue();
        http.post().uri("/demo/faults").contentType(MediaType.APPLICATION_JSON).body("{\"notificationsDown\":false}").retrieve().toBodilessEntity();
        assertThat(get(http, "/demo/faults").get("state").get("notifications.down").asBoolean()).isFalse();
    }
}
