package com.acme;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A man in the middle between a caller and a module's JSON-RPC endpoint. It always forwards the request, so the callee
 * does its work, and then damages the answer: the connection is cut ("the callee died before replying") or the body
 * is garbage. Calls are damaged only while {@link #damageNext} has a budget left.
 */
final class FaultProxy implements AutoCloseable {

    enum Damage { CUT_AFTER_FORWARD, MALFORMED_AFTER_FORWARD }

    private static final List<String> FORWARDED_HEADERS = List.of("Content-Type", "X-Correlation-Id", "traceparent", "X-Rpc-Depth");

    private final HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();
    private final AtomicInteger forwarded = new AtomicInteger();
    private volatile Damage damage;
    private final AtomicInteger budget = new AtomicInteger();
    private final AtomicInteger skip = new AtomicInteger();

    FaultProxy(int listenPort, int targetPort) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", listenPort), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", exchange -> handle(exchange, targetPort));
        server.start();
    }

    /** Damages the answers to the next {@code calls} requests. */
    void damageNext(Damage damage, int calls) {
        damageAfter(0, damage, calls);
    }

    /** Lets {@code skipped} requests through, then damages the answers to the following {@code calls}. */
    void damageAfter(int skipped, Damage damage, int calls) {
        this.damage = damage;
        skip.set(skipped);
        budget.set(calls);
    }

    int forwarded() {
        return forwarded.get();
    }

    private void handle(HttpExchange exchange, int targetPort) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + targetPort + exchange.getRequestURI()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        FORWARDED_HEADERS.forEach(h -> {
            String value = exchange.getRequestHeaders().getFirst(h);
            if (value != null) {
                request.header(h, value);
            }
        });
        HttpResponse<byte[]> response;
        try {
            response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exchange.close();
            return;
        } catch (IOException e) {
            exchange.close();
            return;
        }
        forwarded.incrementAndGet();
        if (skip.getAndUpdate(s -> s > 0 ? s - 1 : 0) == 0 && budget.getAndUpdate(b -> b > 0 ? b - 1 : 0) > 0) {
            if (damage == Damage.CUT_AFTER_FORWARD) {
                exchange.close();
                return;
            }
            byte[] garbage = "<<this is not json-rpc>>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, garbage.length);
            exchange.getResponseBody().write(garbage);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (response.body().length == 0) {
            exchange.sendResponseHeaders(response.statusCode(), -1);
        } else {
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            exchange.getResponseBody().write(response.body());
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
