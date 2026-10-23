package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** The raw wire format: the JSON-RPC envelope on /rpc/{module} and the bare alias on /rpc/{module}/{method}. */
class RpcHttpEndpointTests {

    private static ConfigurableApplicationContext server;
    private static RestClient http;

    @BeforeAll
    static void start() {
        server = new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(
                "--server.port=0", "--modules.orders.enabled=false", "--modules.orders.url=http://127.0.0.1:1/rpc/orders",
                "--modules.inventory.enabled=true", "--spring.datasource.url=jdbc:postgresql://localhost:5432/macro");
        http = RestClient.builder().baseUrl("http://localhost:" + server.getEnvironment().getProperty("local.server.port")).build();
    }

    @AfterAll
    static void stop() {
        server.close();
    }

    private static String post(String path, String body) {
        return http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((request, response) -> response.getStatusCode().value() + " " + new String(response.getBody().readAllBytes()));
    }

    @Test
    void envelope() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.getStock\",\"params\":{\"sku\":\"sku-1\"},\"id\":1}"))
                .isEqualTo("200 {\"jsonrpc\":\"2.0\",\"result\":{\"sku\":\"sku-1\",\"available\":100},\"id\":1}");
    }

    @Test
    void businessErrorEnvelope() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.reserve\",\"params\":{\"request\":{\"requestId\":\"r\",\"orderId\":\"o\",\"sku\":\"sku-2\",\"quantity\":99}},\"id\":2}"))
                .contains("\"code\":1001").contains("com.acme.inventory.api.OutOfStockException").contains("Out of stock: sku-2");
    }

    @Test
    void notificationGetsNoContent() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.getStock\",\"params\":{\"sku\":\"sku-1\"}}")).isEqualTo("204 ");
    }

    @Test
    void aliasReturnsBareResult() {
        assertThat(post("/rpc/inventory/getStock", "{\"sku\":\"sku-1\"}")).isEqualTo("200 {\"sku\":\"sku-1\",\"available\":100}");
    }

    @Test
    void aliasMapsErrorsToHttpStatus() {
        assertThat(post("/rpc/inventory/nope", "{}")).startsWith("404 ").contains("-32601");
        assertThat(post("/rpc/inventory/getStock", "[1]")).startsWith("400 ").contains("-32602");
        assertThat(post("/rpc/inventory/getStock", "{bad")).startsWith("400 ").contains("-32700");
        assertThat(post("/rpc/inventory/reserve", "{\"request\":{\"requestId\":\"r\",\"orderId\":\"o\",\"sku\":\"sku-2\",\"quantity\":99}}"))
                .startsWith("422 ").contains("\"code\":1001");
    }

    @Test
    void unknownModuleIsMethodNotFound() {
        assertThat(post("/rpc/orders", "{\"jsonrpc\":\"2.0\",\"method\":\"orders.getOrder\",\"params\":{\"orderId\":\"x\"},\"id\":1}"))
                .contains("-32601");
    }
}
