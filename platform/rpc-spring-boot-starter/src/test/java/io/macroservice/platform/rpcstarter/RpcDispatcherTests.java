package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcException;
import io.macroservice.platform.rpcstarter.fixtures.CalcService;
import io.macroservice.platform.rpcstarter.overload.OverloadedApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RpcDispatcherTests {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final CalcService calc = new CalcService();
    private final RpcDispatcher dispatcher = new RpcDispatcher(mapper, new StaticListableBeanFactory(Map.of("calc", calc)), List.of());

    private JsonNode send(String body) {
        JsonNode response = dispatcher.handleEnvelope("calc", body);
        return response == null ? null : mapper.readTree(mapper.writeValueAsString(response));
    }

    private static String request(String method, String params, String id) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"%s\"%s%s}".formatted(method,
                params == null ? "" : ",\"params\":" + params, id == null ? "" : ",\"id\":" + id);
    }

    @Test
    void successfulCallReturnsResultAndId() {
        JsonNode response = send(request("calc.add", "{\"a\":1,\"b\":2}", "7"));
        assertThat(response.get("jsonrpc").stringValue()).isEqualTo("2.0");
        assertThat(response.get("result").intValue()).isEqualTo(3);
        assertThat(response.get("id").intValue()).isEqualTo(7);
        assertThat(response.has("error")).isFalse();
    }

    @Test
    void recordParametersAndResultsRoundTrip() {
        JsonNode response = send(request("calc.echo", "{\"echo\":{\"text\":\"hi\",\"count\":2}}", "\"x\""));
        assertThat(response.get("result").get("text").stringValue()).isEqualTo("hi");
        assertThat(response.get("result").get("count").intValue()).isEqualTo(2);
        assertThat(response.get("id").stringValue()).isEqualTo("x");
    }

    @Test
    void missingNonPrimitiveParameterIsNull() {
        assertThat(send(request("calc.nullable", "{}", "1")).get("result").isNull()).isTrue();
    }

    @Test
    void parseError() {
        JsonNode response = send("{not json");
        assertThat(response.get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.PARSE_ERROR);
        assertThat(response.get("id").isNull()).isTrue();
        assertThat(send("").get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.PARSE_ERROR);
    }

    @Test
    void invalidRequests() {
        for (String body : List.of("[]", "1", "\"text\"", "{}", "{\"jsonrpc\":\"1.0\",\"method\":\"calc.add\",\"id\":1}",
                "{\"jsonrpc\":\"2.0\",\"method\":5,\"id\":1}", "{\"jsonrpc\":\"2.0\",\"method\":\"calc.add\",\"id\":[1]}")) {
            assertThat(send(body).get("error").get("code").intValue()).as(body).isEqualTo(RpcErrorCodes.INVALID_REQUEST);
        }
    }

    @Test
    void methodNotFound() {
        assertThat(send(request("calc.nope", "{}", "1")).get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.METHOD_NOT_FOUND);
        assertThat(send(request("other.add", "{}", "1")).get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.METHOD_NOT_FOUND);
        assertThat(send(request("add", "{}", "1")).get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.METHOD_NOT_FOUND);
        assertThat(dispatcher.handleEnvelope("missing", request("missing.add", "{}", "1")).get("error").get("code").intValue())
                .isEqualTo(RpcErrorCodes.METHOD_NOT_FOUND);
    }

    @Test
    void invalidParams() {
        for (String params : List.of("[1,2]", "{\"a\":1}", "{\"a\":\"x\",\"b\":2}", "\"s\"")) {
            assertThat(send(request("calc.add", params, "1")).get("error").get("code").intValue()).as(params)
                    .isEqualTo(RpcErrorCodes.INVALID_PARAMS);
        }
    }

    @Test
    void unexpectedFailureIsAnInternalErrorWithoutDetails() {
        JsonNode response = send(request("calc.boom", "{}", "1"));
        assertThat(response.get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.INTERNAL_ERROR);
        assertThat(response.toString()).doesNotContain("secret internal detail").doesNotContain("IllegalStateException")
                .doesNotContain("stack").doesNotContain("at io.");
    }

    @Test
    void businessExceptionCarriesItsCodeAndClassName() {
        JsonNode error = send(request("calc.denied", "{\"who\":\"bob\"}", "1")).get("error");
        assertThat(error.get("code").intValue()).isEqualTo(4242);
        assertThat(error.get("message").stringValue()).isEqualTo("denied: bob");
        assertThat(error.get("data").get("exception").stringValue()).isEqualTo("io.macroservice.platform.rpcstarter.fixtures.DeniedException");
    }

    @Test
    void notificationsGetNoResponse() {
        assertThat(send(request("calc.fire", "{\"message\":\"m\"}", null))).isNull();
        assertThat(calc.fired).containsExactly("m");
        assertThat(send(request("calc.denied", "{\"who\":\"x\"}", null))).isNull();
        assertThat(send(request("calc.nope", "{}", null))).isNull();
    }

    @Test
    void batchAnswersEveryRequestButNotNotifications() {
        String batch = "[" + request("calc.add", "{\"a\":1,\"b\":1}", "1") + "," + request("calc.fire", "{\"message\":\"b\"}", null)
                + "," + request("calc.nope", "{}", "3") + ",5]";
        JsonNode response = send(batch);
        assertThat(response.isArray()).isTrue();
        assertThat(response).hasSize(3);
        assertThat(response.get(0).get("result").intValue()).isEqualTo(2);
        assertThat(response.get(1).get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.METHOD_NOT_FOUND);
        assertThat(response.get(2).get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.INVALID_REQUEST);
        assertThat(calc.fired).contains("b");
    }

    @Test
    void batchOfNotificationsGetsNoResponse() {
        assertThat(send("[" + request("calc.fire", "{\"message\":\"n1\"}", null) + "," + request("calc.fire", "{\"message\":\"n2\"}", null) + "]")).isNull();
    }

    @Test
    void interceptorCanRejectACall() {
        RpcDispatcher guarded = new RpcDispatcher(mapper, new StaticListableBeanFactory(Map.of("calc", calc)),
                List.of((module, method, next) -> {
                    throw new RpcException(RpcErrorCodes.OVERLOADED, "too many concurrent calls to " + module);
                }));
        JsonNode response = guarded.handleEnvelope("calc", request("calc.add", "{\"a\":1,\"b\":1}", "1"));
        assertThat(response.get("error").get("code").intValue()).isEqualTo(RpcErrorCodes.OVERLOADED);
    }

    @Test
    void overloadedMethodNamesAreRejected() {
        assertThatThrownBy(() -> RpcMethodIndex.methodsOf(OverloadedApi.class))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("overloaded method 'find'");
    }
}
