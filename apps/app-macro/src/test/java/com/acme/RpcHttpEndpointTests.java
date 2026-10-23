package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** The raw wire format: the JSON-RPC envelope on /rpc/{module} and the bare alias on /rpc/{module}/{method}. */
class RpcHttpEndpointTests {

    private static final TestApps APPS = new TestApps();
    private static RestClient http;

    @BeforeAll
    static void start() {
        var server = APPS.start(true, TestApps.local("inventory", "catalog"), TestApps.args("--server.port=0"));
        http = RestClient.builder().baseUrl("http://localhost:" + server.getEnvironment().getProperty("local.server.port")).build();
    }

    @AfterAll
    static void stop() {
        APPS.close();
    }

    private static String post(String path, String body) {
        return http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((request, response) -> response.getStatusCode().value() + " " + new String(response.getBody().readAllBytes()));
    }

    @Test
    void envelope() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.getStock\",\"params\":{\"sku\":\"sku-widget\"},\"id\":1}"))
                .isEqualTo("200 {\"jsonrpc\":\"2.0\",\"result\":{\"sku\":\"sku-widget\",\"available\":100,\"warehouse\":\"wh-1\"},\"id\":1}");
    }

    @Test
    void businessErrorEnvelope() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.reserve\",\"params\":{\"requestId\":\"r\",\"orderId\":\"o\",\"lines\":[{\"sku\":\"sku-oos\",\"quantity\":1}]},\"id\":2}"))
                .contains("\"code\":1001").contains("com.acme.inventory.api.OutOfStockException").contains("Out of stock: sku-oos");
    }

    @Test
    void notificationGetsNoContent() {
        assertThat(post("/rpc/inventory", "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.getStock\",\"params\":{\"sku\":\"sku-widget\"}}")).isEqualTo("204 ");
    }

    @Test
    void aliasReturnsBareResult() {
        assertThat(post("/rpc/inventory/getStock", "{\"sku\":\"sku-widget\"}")).isEqualTo("200 {\"sku\":\"sku-widget\",\"available\":100,\"warehouse\":\"wh-1\"}");
        assertThat(post("/rpc/catalog/productExists", "{\"sku\":\"sku-widget\"}")).isEqualTo("200 true");
    }

    @Test
    void aliasMapsErrorsToHttpStatus() {
        assertThat(post("/rpc/inventory/nope", "{}")).startsWith("404 ").contains("-32601");
        assertThat(post("/rpc/inventory/getStock", "[1]")).startsWith("400 ").contains("-32602");
        assertThat(post("/rpc/inventory/getStock", "{bad")).startsWith("400 ").contains("-32700");
        assertThat(post("/rpc/inventory/reserve", "{\"requestId\":\"r\",\"orderId\":\"o\",\"lines\":[{\"sku\":\"sku-oos\",\"quantity\":1}]}"))
                .startsWith("422 ").contains("\"code\":1001");
    }

    @Test
    void aModuleThisProcessDoesNotHostIsMethodNotFound() {
        assertThat(post("/rpc/orders", "{\"jsonrpc\":\"2.0\",\"method\":\"orders.getOrder\",\"params\":{\"orderId\":\"x\"},\"id\":1}")).contains("-32601");
    }
}
