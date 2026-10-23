package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcErrorCodes;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * HTTP binding of the dispatcher. {@code POST /rpc/{module}} carries the JSON-RPC envelope (always HTTP 200 except
 * for notifications, which get 204). {@code POST /rpc/{module}/{method}} is a convenience alias: bare params object in,
 * bare result out; failures use an HTTP status and an object with {@code code}, {@code message} and optional {@code data}.
 */
@RestController
@RequestMapping("/rpc")
class RpcController {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String TRACEPARENT_HEADER = "traceparent";

    private final RpcDispatcher dispatcher;
    private final JsonMapper mapper;

    RpcController(RpcDispatcher dispatcher, JsonMapper mapper) {
        this.dispatcher = dispatcher;
        this.mapper = mapper;
    }

    @PostMapping("/{module}")
    ResponseEntity<String> envelope(@PathVariable String module, @RequestBody(required = false) String body,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId,
            @RequestHeader(name = TRACEPARENT_HEADER, required = false) String traceparent) {
        return withContext(correlationId, traceparent, () -> {
            JsonNode response = dispatcher.handleEnvelope(module, body);
            return response == null ? ResponseEntity.noContent().build() : json(HttpStatus.OK, response);
        });
    }

    @PostMapping("/{module}/{method}")
    ResponseEntity<String> alias(@PathVariable String module, @PathVariable String method,
            @RequestBody(required = false) String body,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId,
            @RequestHeader(name = TRACEPARENT_HEADER, required = false) String traceparent) {
        return withContext(correlationId, traceparent, () -> {
            JsonNode params;
            try {
                params = body == null || body.isBlank() ? mapper.createObjectNode() : mapper.readTree(body);
            } catch (JacksonException e) {
                return failure(new RpcCallException(RpcErrorCodes.PARSE_ERROR, "Parse error", null));
            }
            if (!params.isObject()) {
                return failure(new RpcCallException(RpcErrorCodes.INVALID_PARAMS,
                        "Invalid params: the body must be a JSON object of named parameters", null));
            }
            try {
                return json(HttpStatus.OK, dispatcher.call(module, method, params));
            } catch (RpcCallException e) {
                return failure(e);
            }
        });
    }

    private ResponseEntity<String> failure(RpcCallException e) {
        ObjectNode body = mapper.createObjectNode();
        body.put("code", e.code());
        body.put("message", e.getMessage());
        if (e.data() != null) {
            body.set("data", e.data());
        }
        return json(statusOf(e.code()), body);
    }

    private static HttpStatus statusOf(int code) {
        return switch (code) {
            case RpcErrorCodes.PARSE_ERROR, RpcErrorCodes.INVALID_REQUEST, RpcErrorCodes.INVALID_PARAMS -> HttpStatus.BAD_REQUEST;
            case RpcErrorCodes.METHOD_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RpcErrorCodes.OVERLOADED -> HttpStatus.SERVICE_UNAVAILABLE;
            case RpcErrorCodes.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    private ResponseEntity<String> json(HttpStatus status, JsonNode body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(body));
    }

    private static ResponseEntity<String> withContext(String correlationId, String traceparent,
            java.util.function.Supplier<ResponseEntity<String>> action) {
        String previousCorrelation = MDC.get(MdcHeaderPropagator.CORRELATION_KEY);
        String previousTrace = MDC.get(MdcHeaderPropagator.TRACEPARENT_KEY);
        try {
            put(MdcHeaderPropagator.CORRELATION_KEY, correlationId);
            put(MdcHeaderPropagator.TRACEPARENT_KEY, traceparent);
            return action.get();
        } finally {
            restore(MdcHeaderPropagator.CORRELATION_KEY, previousCorrelation);
            restore(MdcHeaderPropagator.TRACEPARENT_KEY, previousTrace);
        }
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previous);
        }
    }
}
