package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcException;
import io.macroservice.platform.support.ModuleProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ModuleBulkheadTests {

    private static ModuleProperties properties(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values)).bind("modules", Bindable.of(ModuleProperties.class)).orElseGet(ModuleProperties::new);
    }

    @Test
    void rejectsCallsBeyondThePermitsWithoutDeadlockingAndRecovers() throws Exception {
        ModuleBulkhead bulkhead = new ModuleBulkhead(properties(Map.of(
                "modules.slow.bulkhead.max-concurrent-calls", "2", "modules.slow.bulkhead.max-wait", "200ms")));
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            List<Future<Object>> running = List.of(
                    executor.submit(() -> bulkhead.intercept("slow", "m", () -> { started.countDown(); release.await(); return "one"; })),
                    executor.submit(() -> bulkhead.intercept("slow", "m", () -> { started.countDown(); release.await(); return "two"; })));
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            long begin = System.nanoTime();
            assertThatThrownBy(() -> bulkhead.intercept("slow", "m", () -> "three"))
                    .isInstanceOf(RpcException.class).hasMessageContaining("Module 'slow' is overloaded")
                    .extracting(e -> ((RpcException) e).code()).isEqualTo(RpcErrorCodes.OVERLOADED);
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin)).isLessThan(1500);

            release.countDown();
            assertThat(running.get(0).get(5, TimeUnit.SECONDS)).isEqualTo("one");
            assertThat(running.get(1).get(5, TimeUnit.SECONDS)).isEqualTo("two");
            assertThat(bulkhead.intercept("slow", "m", () -> "after")).isEqualTo("after");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void otherModulesAreNotAffected() throws Exception {
        ModuleBulkhead bulkhead = new ModuleBulkhead(properties(Map.of(
                "modules.a.bulkhead.max-concurrent-calls", "1", "modules.a.bulkhead.max-wait", "50ms")));
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            executor.submit(() -> bulkhead.intercept("a", "m", () -> { inside.countDown(); release.await(); return "a"; }));
            assertThat(inside.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(bulkhead.intercept("b", "m", () -> "b")).isEqualTo("b");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void permitsDefaultToThePoolSize() {
        ModuleBulkhead bulkhead = new ModuleBulkhead(properties(Map.of("modules.p.datasource.max-pool-size", "7")));
        assertThat(ModuleBulkhead.limitOf(bulkhead == null ? null : properties(Map.of("modules.p.datasource.max-pool-size", "7")).of("p"))).isEqualTo(7);
        assertThat(ModuleBulkhead.limitOf(properties(Map.of()).of("unset"))).isEqualTo(3);
    }
}
