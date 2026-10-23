package com.acme.orders.internal;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small text encodings for the order lines and the outbox payload. */
final class Payload {

    private Payload() {
    }

    static String encode(Map<String, String> values) {
        List<String> parts = new ArrayList<>();
        values.forEach((k, v) -> parts.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
        return String.join("&", parts);
    }

    static Map<String, String> decode(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!text.isEmpty()) {
            for (String part : text.split("&")) {
                int eq = part.indexOf('=');
                values.put(URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return values;
    }
}
