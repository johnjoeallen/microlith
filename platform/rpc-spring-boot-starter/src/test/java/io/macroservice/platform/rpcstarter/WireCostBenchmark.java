package io.macroservice.platform.rpcstarter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What each wire format costs in CPU and saves in bytes for one envelope of a given size, without a network, a database or a web server in the way.
 * Not part of the normal build: {@code ./mvnw -o test -pl platform/rpc-spring-boot-starter -Dtest=WireCostBenchmark -Dwire.bench=true}.
 *
 * <p>The comparison is "object tree to bytes on the sender, bytes to object tree on the receiver", the part a format actually changes: JSON text with
 * Jackson against BSON through {@link BsonJson}, and gzip on top of either. (The platform's transport still passes JSON text between the proxy and the wire,
 * so a BSON call there also pays a text parse and print that a native BSON path would not; that extra cost is not counted here.)
 */
@EnabledIfSystemProperty(named = "wire.bench", matches = "true")
class WireCostBenchmark {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** An envelope shaped like the demo's order calls: a request carrying {@code lines} order lines. */
    private static JsonNode envelope(int lines) {
        StringBuilder sb = new StringBuilder("{\"jsonrpc\":\"2.0\",\"method\":\"shipping.quote\",\"id\":42,\"params\":{\"customerId\":\"perf-c-1234\",\"lines\":[");
        for (int i = 0; i < lines; i++) {
            sb.append(i == 0 ? "" : ",").append("{\"sku\":\"perf-s-").append(100 + i * 7).append("\",\"quantity\":").append(1 + i % 3).append('}');
        }
        return MAPPER.readTree(sb.append("]}}").toString());
    }

    private record Format(String name, boolean bson, boolean gzip) {
    }

    @Test
    void costOfEachFormat() {
        List<Format> formats = List.of(new Format("json", false, false), new Format("gzip", false, true), new Format("bson", true, false),
                new Format("bson+gzip", true, true));
        StringBuilder out = new StringBuilder("\nlines  envelope  format     bytes   vs json   encode us  decode us   extra us vs json   break-even MB/s\n");
        for (int lines : new int[] {1, 4, 10, 40, 150}) {
            JsonNode tree = envelope(lines);
            double baseUs = Double.NaN;
            long baseBytes = 0;
            for (Format f : formats) {
                byte[] raw = f.bson() ? BsonJson.toBson(tree) : MAPPER.writeValueAsBytes(tree);
                byte[] wire = f.gzip() ? gzipRaw(raw) : raw;
                double enc = timeMicros(() -> pipelineEncode(f, tree));
                double dec = timeMicros(() -> pipelineDecode(f, wire));
                if (f.name().equals("json")) {
                    baseUs = enc + dec;
                    baseBytes = wire.length;
                }
                double extra = enc + dec - baseUs;
                long saved = baseBytes - wire.length;
                String breakEven = f.name().equals("json") ? "-" : saved <= 0 ? "never (no bytes saved)" : extra <= 0 ? "any link" : String.format(Locale.ROOT, "%.1f", saved / extra);
                out.append(String.format(Locale.ROOT, "%5d  %8d  %-9s %6d  %7.0f%%  %9.1f  %9.1f  %17.1f   %s%n", lines, MAPPER.writeValueAsBytes(tree).length, f.name(),
                        wire.length, wire.length * 100.0 / baseBytes, enc, dec, extra, breakEven));
            }
        }
        System.out.println(out);
    }

    private static Object pipelineEncode(Format f, JsonNode tree) {
        byte[] raw = f.bson() ? BsonJson.toBson(tree) : MAPPER.writeValueAsBytes(tree);
        return f.gzip() ? gzipRaw(raw) : raw;
    }

    private static Object pipelineDecode(Format f, byte[] wire) {
        byte[] raw = f.gzip() ? gunzipRaw(wire) : wire;
        return f.bson() ? BsonJson.fromBson(raw, MAPPER) : MAPPER.readTree(raw);
    }

    private static byte[] gzipRaw(byte[] bytes) {
        try (var out = new java.io.ByteArrayOutputStream(bytes.length / 2 + 32); var gz = new java.util.zip.GZIPOutputStream(out)) {
            gz.write(bytes);
            gz.finish();
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static byte[] gunzipRaw(byte[] bytes) {
        try (var in = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** Median of several timed batches, after a warm-up, in microseconds per call. */
    private static double timeMicros(java.util.function.Supplier<Object> work) {
        Object sink = null;
        for (int i = 0; i < 20_000; i++) {
            sink = work.get();
        }
        List<Double> batches = new ArrayList<>();
        for (int b = 0; b < 7; b++) {
            int n = 20_000;
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                sink = work.get();
            }
            batches.add((System.nanoTime() - start) / 1000.0 / n);
        }
        if (sink == null) {
            throw new IllegalStateException();
        }
        batches.sort(Double::compare);
        return batches.get(batches.size() / 2);
    }
}
