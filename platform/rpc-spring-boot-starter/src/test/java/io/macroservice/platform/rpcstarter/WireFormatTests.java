package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WireFormatTests {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String ENVELOPE = """
            {"jsonrpc":"2.0","method":"orders.placeOrder","id":7,"params":{"requestId":"r-1","customerId":"cust-é","lines":[
            {"sku":"sku-widget","quantity":2},{"sku":"sku-gadget","quantity":1}],"note":null,"flag":true,"big":5000000000,"ratio":0.25,
            "nested":{"empty":{},"none":[],"list":[1,"two",[3],{"four":4}]}}}""".replace("\n", "");

    @Test
    void everyFormatRoundTripsTheEnvelope() {
        JsonNode expected = MAPPER.readTree(ENVELOPE);
        for (WireFormat format : WireFormat.values()) {
            byte[] encoded = WireCodec.encode(format, ENVELOPE, MAPPER);
            String contentType = format.mediaType();
            String contentEncoding = format.gzip() ? "gzip" : null;
            assertThat(MAPPER.readTree(WireCodec.decode(encoded, contentType, contentEncoding, MAPPER))).as(format.label()).isEqualTo(expected);
        }
    }

    @Test
    void integersKeepTheirWidthThroughBson() {
        JsonNode back = MAPPER.readTree(WireCodec.decode(WireCodec.encode(WireFormat.BSON, "{\"a\":1,\"b\":5000000000,\"c\":1.5}", MAPPER),
                WireFormat.BSON_TYPE, null, MAPPER));
        assertThat(back.get("a").isInt()).isTrue();
        assertThat(back.get("b").isLong()).isTrue();
        assertThat(back.get("c").isFloatingPointNumber()).isTrue();
    }

    @Test
    void gzipIsActuallyCompressedAndBsonIsBinary() {
        String repetitive = "{\"jsonrpc\":\"2.0\",\"result\":\"" + "abc".repeat(500) + "\",\"id\":1}";
        assertThat(WireCodec.encode(WireFormat.GZIP, repetitive, MAPPER).length).isLessThan(repetitive.length() / 5);
        assertThat(WireCodec.encode(WireFormat.BSON, "{\"a\":1}", MAPPER)[0]).isNotEqualTo((byte) '{');
    }

    @Test
    void aBodyThatIsNotWhatItClaimsFailsToDecode() {
        assertThatThrownBy(() -> WireCodec.decode("not gzip".getBytes(), "application/json", "gzip", MAPPER)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> WireCodec.decode(new byte[] {1, 2, 3}, "application/bson", null, MAPPER)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void theFormatNamesParse() {
        assertThat(WireFormat.parse(null)).isEqualTo(WireFormat.JSON);
        assertThat(WireFormat.parse(" ")).isEqualTo(WireFormat.JSON);
        assertThat(WireFormat.parse("GZIP")).isEqualTo(WireFormat.GZIP);
        assertThat(WireFormat.parse("bson+gzip")).isEqualTo(WireFormat.BSON_GZIP);
        assertThat(WireFormat.parse("bson-gzip")).isEqualTo(WireFormat.BSON_GZIP);
        assertThatThrownBy(() -> WireFormat.parse("protobuf")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("rpc.client.wire");
    }
}
