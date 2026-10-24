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

    /**
     * The envelope endpoint accepts JSON, gzip-compressed JSON, BSON and gzip-compressed BSON (Content-Type and Content-Encoding) and answers in the
     * format the caller asked for (Accept and Accept-Encoding); a caller that sends neither gets plain JSON, as before.
     */
    @PostMapping("/{module}")
    ResponseEntity<byte[]> envelope(@PathVariable String module, @RequestBody(required = false) byte[] body,
            @RequestHeader(name = "Content-Type", required = false) String contentType,
            @RequestHeader(name = "Content-Encoding", required = false) String contentEncoding,
            @RequestHeader(name = "Accept", required = false) String accept,
            @RequestHeader(name = "Accept-Encoding", required = false) String acceptEncoding,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId,
            @RequestHeader(name = TRACEPARENT_HEADER, required = false) String traceparent) {
        return withContext(correlationId, traceparent, () -> {
            String json;
            try {
                json = body == null ? null : WireCodec.decode(body, contentType, contentEncoding, mapper);
            } catch (RuntimeException undecodable) {
                json = "{";      // not gzip or not BSON: answered as a parse error, like malformed JSON
            }
            JsonNode response = dispatcher.handleEnvelope(module, json);
            if (response == null) {
                return ResponseEntity.noContent().build();
            }
            boolean bson = WireCodec.isBson(accept);
            boolean gzip = WireCodec.isGzip(acceptEncoding);
            WireFormat format = bson ? (gzip ? WireFormat.BSON_GZIP : WireFormat.BSON) : (gzip ? WireFormat.GZIP : WireFormat.JSON);
            ResponseEntity.BodyBuilder reply = ResponseEntity.status(HttpStatus.OK).contentType(MediaType.parseMediaType(format.mediaType()));
            if (gzip) {
                reply = reply.header("Content-Encoding", "gzip");
            }
            return reply.body(WireCodec.encode(format, mapper.writeValueAsString(response), mapper));
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

    private static <T> ResponseEntity<T> withContext(String correlationId, String traceparent,
            java.util.function.Supplier<ResponseEntity<T>> action) {
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
