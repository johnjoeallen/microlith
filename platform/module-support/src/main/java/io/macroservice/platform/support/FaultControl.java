package io.macroservice.platform.support;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo-only switches that make a module misbehave on purpose ({@code notifications.down}, {@code payments.decline},
 * {@code inventory.latency}). Exists only in the {@code demo} profile; modules ask for it through an ObjectProvider.
 * Only the modules enabled in this process look at it.
 */
public class FaultControl {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();
    private final Map<String, Duration> delays = new ConcurrentHashMap<>();

    public void setFlag(String name, boolean on) {
        flags.put(name, on);
    }

    public boolean flag(String name) {
        return flags.getOrDefault(name, false);
    }

    public void setDelay(String name, Duration delay) {
        delays.put(name, delay);
    }

    public Duration delay(String name) {
        return delays.getOrDefault(name, Duration.ZERO);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> all = new java.util.TreeMap<>();
        flags.forEach(all::put);
        delays.forEach((k, v) -> all.put(k, v.toString()));
        return all;
    }

    public void sleep(String name) {
        Duration delay = delay(name);
        if (!delay.isZero()) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
