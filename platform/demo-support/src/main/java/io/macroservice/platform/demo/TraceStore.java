package io.macroservice.platform.demo;

import java.util.ArrayDeque;
import java.util.List;

/** Bounded in-memory buffer of the most recent calls (oldest are dropped first). */
public class TraceStore {

    private final int capacity;
    private final ArrayDeque<TraceRecord> records = new ArrayDeque<>();

    public TraceStore(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(TraceRecord record) {
        if (records.size() == capacity) {
            records.removeFirst();
        }
        records.addLast(record);
    }

    public synchronized List<TraceRecord> find(String correlationId) {
        return records.stream().filter(r -> r.correlationId().equals(correlationId))
                .sorted(java.util.Comparator.comparingLong(TraceRecord::startedAtMicros)).toList();
    }

    public synchronized int size() {
        return records.size();
    }
}
