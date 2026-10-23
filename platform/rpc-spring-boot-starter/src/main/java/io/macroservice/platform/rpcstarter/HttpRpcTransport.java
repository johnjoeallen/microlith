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

/** HTTP transport: RestClient over the JDK HttpClient with per-module timeouts. */
public class HttpRpcTransport implements RpcTransport {

    private final Supplier<String> url;
    private final RestClient client;

    public HttpRpcTransport(Supplier<String> url, ModuleSettings.Client settings) {
        this.url = url;
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build());
        factory.setReadTimeout(settings.readTimeout());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String send(String module, String requestJson, Map<String, String> headers) {
        String target = url.get();
        try {
            return client.post().uri(target).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .headers(h -> headers.forEach(h::set)).body(requestJson)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        String body = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
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
}
