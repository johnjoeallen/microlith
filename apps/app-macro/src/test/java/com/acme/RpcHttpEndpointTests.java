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
        // the JDK client as the platform's own transport uses it, with Spring's automatic compression off so that the Content-Encoding the server sent stays visible
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory();
        factory.enableCompression(false);
        http = RestClient.builder().requestFactory(factory)
                .baseUrl("http://localhost:" + server.getEnvironment().getProperty("local.server.port")).build();
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

    private static final String STOCK = "{\"jsonrpc\":\"2.0\",\"method\":\"inventory.getStock\",\"params\":{\"sku\":\"sku-widget\"},\"id\":1}";
    private static final String STOCK_ANSWER = "{\"jsonrpc\":\"2.0\",\"result\":{\"sku\":\"sku-widget\",\"available\":100,\"warehouse\":\"wh-1\"},\"id\":1}";

    private record Reply(int status, String contentType, String contentEncoding, byte[] body) {
    }

    private static Reply exchange(byte[] body, String contentType, String contentEncoding, String accept, String acceptEncoding) {
        return http.post().uri("/rpc/inventory").headers(h -> {
            h.set("Content-Type", contentType);
            if (contentEncoding != null) {
                h.set("Content-Encoding", contentEncoding);
            }
            if (accept != null) {
                h.set("Accept", accept);
            }
            if (acceptEncoding != null) {
                h.set("Accept-Encoding", acceptEncoding);
            }
        }).body(body).exchange((request, response) -> new Reply(response.getStatusCode().value(),
                response.getHeaders().getFirst("Content-Type"), response.getHeaders().getFirst("Content-Encoding"), response.getBody().readAllBytes()));
    }

    private static byte[] gzip(byte[] bytes) throws java.io.IOException {
        var out = new java.io.ByteArrayOutputStream();
        try (var gz = new java.util.zip.GZIPOutputStream(out)) {
            gz.write(bytes);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] bytes) throws java.io.IOException {
        try (var in = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        }
    }

    /** BSON is built and read with the MongoDB library itself, not with the platform's own converter, so this also checks the format is real BSON. */
    private static byte[] bson(String json) {
        var buffer = org.bson.RawBsonDocument.parse(json).getByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static String bsonToJson(byte[] bytes) {
        return new org.bson.RawBsonDocument(bytes).toJson();
    }

    private static void sameJson(String actual, String expected) {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        assertThat(mapper.readTree(actual)).isEqualTo(mapper.readTree(expected));
    }

    @Test
    void gzipJsonIsAcceptedAndAnsweredInKind() throws Exception {
        Reply reply = exchange(gzip(STOCK.getBytes()), "application/json", "gzip", "application/json", "gzip");
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.contentEncoding()).isEqualTo("gzip");
        assertThat(reply.contentType()).startsWith("application/json");
        sameJson(new String(gunzip(reply.body())), STOCK_ANSWER);
    }

    @Test
    void bsonIsAcceptedAndAnsweredInKind() {
        Reply reply = exchange(bson(STOCK), "application/bson", null, "application/bson", null);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.contentType()).startsWith("application/bson");
        assertThat(reply.contentEncoding()).isNull();
        sameJson(bsonToJson(reply.body()), STOCK_ANSWER);
    }

    @Test
    void gzipBsonIsAcceptedAndAnsweredInKind() throws Exception {
        Reply reply = exchange(gzip(bson(STOCK)), "application/bson", "gzip", "application/bson", "gzip");
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.contentEncoding()).isEqualTo("gzip");
        assertThat(reply.contentType()).startsWith("application/bson");
        sameJson(bsonToJson(gunzip(reply.body())), STOCK_ANSWER);
    }

    @Test
    void theFormatOfTheRequestDoesNotDecideTheFormatOfTheAnswer() {
        // a BSON request from a caller that asks for JSON gets JSON: the caller says what it can read
        Reply reply = exchange(bson(STOCK), "application/bson", null, "application/json", null);
        assertThat(reply.contentType()).startsWith("application/json");
        sameJson(new String(reply.body()), STOCK_ANSWER);
    }

    @Test
    void aBodyThatCannotBeDecodedIsAParseError() {
        Reply reply = exchange("not gzip at all".getBytes(), "application/json", "gzip", null, null);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(new String(reply.body())).contains("-32700");
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
