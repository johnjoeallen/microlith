package io.macroservice.platform.rpcstarter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import tools.jackson.databind.json.JsonMapper;

/** Encodes a JSON-RPC envelope (a JSON string) in a {@link WireFormat} and decodes a body back to the JSON string, from its Content-Type and Content-Encoding. */
final class WireCodec {

    private WireCodec() {
    }

    static byte[] encode(WireFormat format, String json, JsonMapper mapper) {
        byte[] bytes = format.bson() ? BsonJson.toBson(mapper.readTree(json)) : json.getBytes(StandardCharsets.UTF_8);
        return format.gzip() ? gzip(bytes) : bytes;
    }

    /** @param contentType the Content-Type header (BSON when it names application/bson, JSON otherwise); @param contentEncoding gzip or nothing */
    static String decode(byte[] body, String contentType, String contentEncoding, JsonMapper mapper) {
        byte[] bytes = isGzip(contentEncoding) ? gunzip(body) : body;
        if (contentType != null && contentType.toLowerCase().contains(WireFormat.BSON_TYPE)) {
            return mapper.writeValueAsString(BsonJson.fromBson(bytes, mapper));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static boolean isGzip(String headerValue) {
        return headerValue != null && headerValue.toLowerCase().contains("gzip");
    }

    static boolean isBson(String headerValue) {
        return headerValue != null && headerValue.toLowerCase().contains(WireFormat.BSON_TYPE);
    }

    private static byte[] gzip(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length / 2 + 32);
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] bytes) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
