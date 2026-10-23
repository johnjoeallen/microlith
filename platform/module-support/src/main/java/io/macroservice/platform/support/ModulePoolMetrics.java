package io.macroservice.platform.support;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Exposes each module's Hikari pool as {@code macro.db.pool.connections{module,state=active|idle|pending|total}}
 * and {@code macro.db.pool.timeouts{module}}.
 */
final class ModulePoolMetrics implements MetricsTrackerFactory {

    private final String module;
    private final MeterRegistry registry;

    ModulePoolMetrics(String module, MeterRegistry registry) {
        this.module = module;
        this.registry = registry;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats stats) {
        gauge("active", stats, PoolStats::getActiveConnections);
        gauge("idle", stats, PoolStats::getIdleConnections);
        gauge("pending", stats, PoolStats::getPendingThreads);
        gauge("total", stats, PoolStats::getTotalConnections);
        Counter timeouts = Counter.builder("macro.db.pool.timeouts").description("Connection acquire timeouts")
                .tag("module", module).register(registry);
        return new IMetricsTracker() {
            @Override
            public void recordConnectionTimeout() {
                timeouts.increment();
            }
        };
    }

    private void gauge(String state, PoolStats stats, java.util.function.ToDoubleFunction<PoolStats> value) {
        Gauge.builder("macro.db.pool.connections", stats, value).tag("module", module).tag("state", state)
                .strongReference(true).register(registry);
    }
}
