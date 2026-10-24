package io.macroservice.platform.rpcstarter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * What this process put on the wire as an HTTP client: how many calls, and the size of the JSON-RPC envelopes before encoding against the bytes actually
 * sent and received. Read by the demo's {@code /demo/wire}; the load driver takes the difference between two reads.
 */
public final class WireStats {

    private static final LongAdder CALLS = new LongAdder();
    private static final LongAdder RAW_REQUEST = new LongAdder();
    private static final LongAdder SENT_REQUEST = new LongAdder();
    private static final LongAdder RAW_RESPONSE = new LongAdder();
    private static final LongAdder RECEIVED_RESPONSE = new LongAdder();

    private WireStats() {
    }

    static void request(long rawBytes, long sentBytes) {
        CALLS.increment();
        RAW_REQUEST.add(rawBytes);
        SENT_REQUEST.add(sentBytes);
    }

    static void response(long rawBytes, long receivedBytes) {
        RAW_RESPONSE.add(rawBytes);
        RECEIVED_RESPONSE.add(receivedBytes);
    }

    public static Map<String, Long> snapshot() {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("calls", CALLS.sum());
        values.put("rawRequestBytes", RAW_REQUEST.sum());
        values.put("sentRequestBytes", SENT_REQUEST.sum());
        values.put("rawResponseBytes", RAW_RESPONSE.sum());
        values.put("receivedResponseBytes", RECEIVED_RESPONSE.sum());
        return values;
    }
}
