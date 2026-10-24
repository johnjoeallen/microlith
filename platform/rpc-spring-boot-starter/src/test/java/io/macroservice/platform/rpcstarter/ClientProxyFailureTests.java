package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.RpcProtocolException;
import io.macroservice.platform.rpc.RpcRemoteException;
import io.macroservice.platform.rpc.RpcUnavailableException;
import io.macroservice.platform.rpcstarter.fixtures.DeniedException;
import io.macroservice.platform.support.ModuleSettings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * How the client proxy reports each way a call can fail, against a scripted HTTP server: every failure maps to the
 * documented exception type, and only {@code @Idempotent} methods are retried (and only on "unavailable").
 */
class ClientProxyFailureTests {

    @RpcApi("flaky")
    public interface FlakyApi {

        @Idempotent
        int read();

        int write();
    }

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private HttpServer server;
    private final ConcurrentLinkedQueue<Consumer<HttpExchange>> script = new ConcurrentLinkedQueue<>();
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rpc/flaky", exchange -> {
            requests.incrementAndGet();
            Consumer<HttpExchange> next = script.poll();
            (next == null ? (Consumer<HttpExchange>) ClientProxyFailureTests::ok : next).accept(exchange);
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private FlakyApi client(int retries, Duration readTimeout) {
        return client("http://127.0.0.1:" + server.getAddress().getPort() + "/rpc/flaky", retries, readTimeout);
    }

    private FlakyApi client(String url, int retries, Duration readTimeout) {
        ModuleSettings.Client settings = new ModuleSettings.Client(Duration.ofMillis(500), readTimeout, retries, Duration.ofMillis(10));
        return new RpcClientProxyFactory(JsonMapper.builder().build(), java.util.List.of())
                .create(FlakyApi.class, "flaky", new HttpRpcTransport(() -> url, settings), settings);
    }

    private static String body(HttpExchange exchange) {
        try {
            return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void reply(HttpExchange exchange, int status, String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String idOf(HttpExchange exchange) {
        Matcher matcher = ID.matcher(body(exchange));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static void ok(HttpExchange exchange) {
        reply(exchange, 200, "{\"jsonrpc\":\"2.0\",\"result\":7,\"id\":" + idOf(exchange) + "}");
    }

    private static Consumer<HttpExchange> error(String errorJson) {
        return exchange -> reply(exchange, 200, "{\"jsonrpc\":\"2.0\",\"error\":" + errorJson + ",\"id\":" + idOf(exchange) + "}");
    }

    private static Consumer<HttpExchange> unavailable() {
        return exchange -> {
            body(exchange);
            reply(exchange, 503, "{\"code\":-32002,\"message\":\"overloaded\"}");
        };
    }

    @Test
    void aSlowCalleeTimesOutAsUnavailable() {
        script.add(exchange -> {
            body(exchange);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        assertThatThrownBy(() -> client(0, Duration.ofMillis(300)).write()).isInstanceOf(RpcUnavailableException.class).hasMessageContaining("flaky");
    }

    @Test
    void aRefusedConnectionIsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        assertThatThrownBy(() -> client("http://127.0.0.1:" + closedPort + "/rpc/flaky", 0, Duration.ofSeconds(1)).write())
                .isInstanceOf(RpcUnavailableException.class).hasMessageContaining("unreachable");
    }

    @Test
    void aConnectionClosedMidCallIsUnavailable() {
        script.add(exchange -> {
            body(exchange);
            exchange.close();
        });
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isInstanceOf(RpcUnavailableException.class);
    }

    @Test
    void aDeclaredBusinessErrorComesBackAsTheSameExceptionType() {
        script.add(error("{\"code\":4242,\"message\":\"denied: eve\",\"data\":{\"exception\":\"" + DeniedException.class.getName() + "\"}}"));
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isExactlyInstanceOf(DeniedException.class).hasMessage("denied: eve");
    }

    @Test
    void anUnknownServerErrorIsARemoteException() {
        script.add(error("{\"code\":-32603,\"message\":\"Internal error\"}"));
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isExactlyInstanceOf(RpcRemoteException.class).hasMessageContaining("Internal error");
    }

    @Test
    void anOverloadedServerIsUnavailable() {
        script.add(error("{\"code\":-32002,\"message\":\"overloaded\"}"));
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isInstanceOf(RpcUnavailableException.class);
    }

    @Test
    void methodNotFoundIsAProtocolError() {
        script.add(error("{\"code\":-32601,\"message\":\"Method not found\"}"));
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isInstanceOf(RpcProtocolException.class);
    }

    @Test
    void aMalformedResponseIsAProtocolError() {
        script.add(exchange -> {
            body(exchange);
            reply(exchange, 200, "this is {not json");
        });
        script.add(exchange -> {
            body(exchange);
            reply(exchange, 200, "{\"result\":7}");
        });
        script.add(exchange -> reply(exchange, 200, "{\"jsonrpc\":\"2.0\",\"result\":7,\"id\":999}"));
        script.add(exchange -> {
            String id = idOf(exchange);
            reply(exchange, 200, "{\"jsonrpc\":\"2.0\",\"result\":\"seven\",\"id\":" + id + "}");
        });
        FlakyApi client = client(0, Duration.ofSeconds(2));
        assertThatThrownBy(client::write).isInstanceOf(RpcProtocolException.class).hasMessageContaining("malformed");
        assertThatThrownBy(client::write).isInstanceOf(RpcProtocolException.class).hasMessageContaining("not JSON-RPC");
        assertThatThrownBy(client::write).isInstanceOf(RpcProtocolException.class).hasMessageContaining("unexpected response id");
        assertThatThrownBy(client::write).isInstanceOf(RpcProtocolException.class).hasMessageContaining("does not match");
    }

    @Test
    void anEmptyAnswerToACallThatNeedsAResultIsAProtocolError() {
        script.add(exchange -> {
            body(exchange);
            reply(exchange, 200, "");
        });
        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).write()).isInstanceOf(RpcProtocolException.class);
    }

    @Test
    void anIdempotentMethodIsRetriedWhenUnavailableAndSucceeds() {
        script.add(unavailable());
        script.add(unavailable());
        assertThat(client(2, Duration.ofSeconds(2)).read()).isEqualTo(7);
        assertThat(requests).hasValue(3);
    }

    @Test
    void retriesStopAfterTheConfiguredNumberAndTheLastFailureIsThrown() {
        for (int i = 0; i < 5; i++) {
            script.add(unavailable());
        }
        assertThatThrownBy(() -> client(2, Duration.ofSeconds(2)).read()).isInstanceOf(RpcUnavailableException.class);
        assertThat(requests).hasValue(3);
    }

    @Test
    void aMethodThatIsNotIdempotentIsNeverRetried() {
        script.add(unavailable());
        assertThatThrownBy(() -> client(2, Duration.ofSeconds(2)).write()).isInstanceOf(RpcUnavailableException.class);
        assertThat(requests).hasValue(1);
    }

    @Test
    void businessAndProtocolErrorsAreNotRetriedEvenWhenIdempotent() {
        script.add(error("{\"code\":-32603,\"message\":\"Internal error\"}"));
        assertThatThrownBy(() -> client(2, Duration.ofSeconds(2)).read()).isInstanceOf(RpcRemoteException.class);
        assertThat(requests).hasValue(1);
    }
}
