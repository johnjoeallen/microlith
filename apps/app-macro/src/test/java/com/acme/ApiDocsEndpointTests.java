package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import io.macroservice.platform.rpcdocs.OpenApiValidator;
import io.macroservice.platform.support.testing.SharedPostgres;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The generated OpenAPI and OpenRPC documents served under /rpc, and Swagger UI in the dev profile only. */
class ApiDocsEndpointTests {

    private final TestApps apps = new TestApps();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @AfterEach
    void close() {
        apps.close();
    }

    private RestClient client(ConfigurableApplicationContext context) {
        return RestClient.builder().baseUrl("http://localhost:" + context.getEnvironment().getProperty("local.server.port")).build();
    }

    private static String get(RestClient client, String path) {
        return client.get().uri(path).exchange((request, response) -> response.getStatusCode().value() + " " + new String(response.getBody().readAllBytes()));
    }

    private JsonNode json(RestClient client, String path) {
        String response = get(client, path);
        assertThat(response).startsWith("200 ");
        return mapper.readTree(response.substring(4));
    }

    @Test
    void mergedDocumentCoversEveryLocalModuleAndIsValidOpenApi31() {
        RestClient client = client(apps.start(true, TestApps.allLocal(), TestApps.args("--server.port=0")));

        JsonNode merged = json(client, "/rpc/openapi.json");

        assertThat(OpenApiValidator.validate(merged)).isEmpty();
        assertThat(merged.get("openapi").asString()).isEqualTo("3.1.0");
        assertThat(merged.get("tags").valueStream().map(t -> t.get("name").asString()).toList()).containsExactlyElementsOf(TestApps.MODULES.stream().sorted().toList());
        JsonNode getStock = merged.at("/paths/~1rpc~1inventory~1getStock/post");
        assertThat(getStock.get("operationId").asString()).isEqualTo("inventory.getStock");
        assertThat(getStock.get("summary").asString()).isEqualTo("Gets the stock level of a SKU");
        assertThat(merged.at("/paths/~1rpc~1payments~1voidAuthorization/post/operationId").asString()).isEqualTo("payments.voidAuthorization");
        assertThat(merged.at("/components/schemas/Order/properties/status/$ref").asString()).isEqualTo("#/components/schemas/OrderStatus");
    }

    @Test
    void perModuleDocumentsExistInBothFormats() {
        RestClient client = client(apps.start(true, TestApps.allLocal(), TestApps.args("--server.port=0")));

        for (String module : TestApps.MODULES) {
            JsonNode openApi = json(client, "/rpc/" + module + "/openapi.json");
            assertThat(OpenApiValidator.validate(openApi)).as(module).isEmpty();
            assertThat(openApi.get("paths").propertyNames()).allMatch(p -> p.startsWith("/rpc/" + module + "/"));
            JsonNode openRpc = json(client, "/rpc/" + module + "/openrpc.json");
            assertThat(openRpc.get("openrpc").asString()).isEqualTo("1.3.2");
            assertThat(openRpc.get("methods").valueStream().map(m -> m.get("name").asString())).allMatch(n -> n.startsWith(module + "."));
        }
        JsonNode inventory = json(client, "/rpc/inventory/openrpc.json");
        assertThat(inventory.get("methods").valueStream().map(m -> m.get("name").asString()).toList())
                .containsExactly("inventory.getReservation", "inventory.getStock", "inventory.release", "inventory.reserve");
        assertThat(inventory.at("/methods/3/params/2/schema/items/$ref").asString()).isEqualTo("#/components/schemas/ReserveLine");
    }

    @Test
    void aProcessDocumentsOnlyTheModulesItRuns() {
        RestClient client = client(apps.start(true, TestApps.local("inventory", "catalog"), TestApps.args("--server.port=0")));

        assertThat(get(client, "/rpc/orders/openapi.json")).startsWith("404 ").contains("-32601");
        assertThat(get(client, "/rpc/orders/openrpc.json")).startsWith("404 ");
        JsonNode merged = json(client, "/rpc/openapi.json");
        assertThat(merged.get("tags").valueStream().map(t -> t.get("name").asString()).toList()).containsExactly("catalog", "inventory");
        assertThat(merged.get("paths").propertyNames()).noneMatch(p -> p.startsWith("/rpc/orders/"));
    }

    @Test
    void swaggerUiIsServedInDevOnly() {
        String[] modules = {"inventory", "catalog"};
        RestClient dev = client(apps.start(true, TestApps.args("--spring.profiles.active=dev", "--server.port=0"),
                TestApps.topology(java.util.Map.of(), modules), SharedPostgres.args(modules)));
        assertThat(get(dev, "/swagger-ui/index.html")).startsWith("200 ").contains("swagger-ui");
        String initializer = get(dev, "/swagger-ui/swagger-initializer.js");
        assertThat(initializer).startsWith("200 ").contains("/rpc/openapi.json").contains("/rpc/inventory/openapi.json");
        assertThat(get(dev, "/swagger-ui/swagger-ui-bundle.js")).startsWith("200 ");

        RestClient plain = client(apps.start(true, TestApps.args("--server.port=0"),
                TestApps.topology(java.util.Map.of(), modules), SharedPostgres.args(modules)));
        assertThat(get(plain, "/swagger-ui/index.html")).startsWith("404 ");
        assertThat(get(plain, "/swagger-ui/swagger-initializer.js")).startsWith("404 ");
        assertThat(get(plain, "/rpc/openapi.json")).startsWith("200 ");
    }
}
