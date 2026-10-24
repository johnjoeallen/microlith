package io.macroservice.platform.demo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bounded in-memory buffer of the most recent calls (oldest are dropped first), indexed by correlation id. */
public class TraceStore {

    private final int capacity;
    private final ArrayDeque<TraceRecord> records = new ArrayDeque<>();
    private final Map<String, List<TraceRecord>> byCorrelationId = new HashMap<>();

    public TraceStore(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(TraceRecord record) {
        if (records.size() == capacity) {
            drop(records.removeFirst());
        }
        records.addLast(record);
        byCorrelationId.computeIfAbsent(record.correlationId(), id -> new ArrayList<>(16)).add(record);
    }

    private void drop(TraceRecord oldest) {
        List<TraceRecord> forId = byCorrelationId.get(oldest.correlationId());
        if (forId != null) {
            forId.remove(oldest);
            if (forId.isEmpty()) {
                byCorrelationId.remove(oldest.correlationId());
            }
        }
    }

    public synchronized List<TraceRecord> find(String correlationId) {
        return byCorrelationId.getOrDefault(correlationId, List.of()).stream()
                .sorted(Comparator.comparingLong(TraceRecord::startedAtMicros)).toList();
    }

    public synchronized int size() {
        return records.size();
    }
}
