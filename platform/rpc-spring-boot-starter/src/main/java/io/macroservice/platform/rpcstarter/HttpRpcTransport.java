package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcProtocolException;
import io.macroservice.platform.rpc.RpcUnavailableException;
import io.macroservice.platform.support.ModuleSettings;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/** HTTP transport: RestClient over the JDK HttpClient with per-module timeouts. */
public class HttpRpcTransport implements RpcTransport {

    private final Supplier<String> url;
    private final RestClient client;
    private final WireFormat wire;
    private final JsonMapper mapper;

    public HttpRpcTransport(Supplier<String> url, ModuleSettings.Client settings) {
        this(url, settings, WireFormat.JSON, null);
    }

    /** @param wire how the envelope is put on the request (the server answers in kind); @param mapper needed for BSON, may be null for JSON and gzip */
    public HttpRpcTransport(Supplier<String> url, ModuleSettings.Client settings, WireFormat wire, JsonMapper mapper) {
        this.url = url;
        this.wire = wire;
        this.mapper = mapper;
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build());
        factory.setReadTimeout(settings.readTimeout());
        // Spring's JDK client adds "Accept-Encoding: gzip, deflate" to every request and silently decompresses the answer (on by default). Off: the wire format is
        // chosen by this transport alone, a plain JSON call stays plain, and the bytes counted are the bytes that crossed the connection.
        factory.enableCompression(false);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String send(String module, String requestJson, Map<String, String> headers) {
        String target = url.get();
        try {
            RestClient.RequestBodySpec request = client.post().uri(target).contentType(MediaType.parseMediaType(wire.mediaType()))
                    .accept(MediaType.parseMediaType(wire.mediaType())).headers(h -> headers.forEach(h::set));
            if (wire.gzip()) {
                request = request.header("Content-Encoding", "gzip").header("Accept-Encoding", "gzip");
            }
            long raw = requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (wire == WireFormat.JSON) {
                WireStats.request(raw, raw);
                request = request.body(requestJson);
            } else {
                byte[] encoded = WireCodec.encode(wire, requestJson, mapper);
                WireStats.request(raw, encoded.length);
                request = request.body(encoded);
            }
            return request.exchange((req, response) -> {
                int status = response.getStatusCode().value();
                byte[] bytes = response.getBody().readAllBytes();
                String body = decode(module, target, bytes, response.getHeaders().getFirst("Content-Type"),
                        response.getHeaders().getFirst("Content-Encoding"));
                WireStats.response(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, bytes.length);
                if (status == 204 || (body.isBlank() && status < 300)) {
                    return null;
                }
                if (status == 502 || status == 503 || status == 504) {
                    throw new RpcUnavailableException("Module '%s' at %s is unavailable (HTTP %d)".formatted(module, target, status));
                }
                if (body.isBlank()) {
                    throw new RpcProtocolException("Empty response from module '%s' at %s (HTTP %d)".formatted(module, target, status));
                }
                return body;
            });
        } catch (ResourceAccessException e) {
            throw new RpcUnavailableException("Module '%s' at %s is unreachable: %s".formatted(module, target, e.getMessage()), e);
        } catch (RestClientException e) {
            throw new RpcProtocolException("Call to module '%s' at %s failed: %s".formatted(module, target, e.getMessage()), e);
        }
    }

    private String decode(String module, String target, byte[] bytes, String contentType, String contentEncoding) {
        if (bytes.length == 0) {
            return "";
        }
        if (!WireCodec.isGzip(contentEncoding) && !WireCodec.isBson(contentType)) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            return WireCodec.decode(bytes, contentType, contentEncoding, mapper);
        } catch (RuntimeException e) {
            throw new RpcProtocolException("Module '%s' at %s sent a body that could not be decoded: %s".formatted(module, target, e.getMessage()), e);
        }
    }
}
