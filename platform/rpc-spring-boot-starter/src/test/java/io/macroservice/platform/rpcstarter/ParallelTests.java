package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.macroservice.platform.rpc.Parallel;
import io.macroservice.platform.rpc.RpcUnavailableException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ParallelTests {

    static {
        new ParallelContext();
    }

    @Test
    void callsRunAtTheSameTime() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        Parallel.Pending<String> first = Parallel.supply(() -> meet(bothStarted, "a"));
        Parallel.Pending<String> second = Parallel.supply(() -> meet(bothStarted, "b"));
        // each waits for the other, so this only finishes if they really overlap
        assertThat(first.join() + second.join()).isEqualTo("ab");
    }

    private static String meet(CountDownLatch latch, String result) {
        latch.countDown();
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Test
    void aFailureIsRethrownAsTheExceptionTheCallThrew() {
        Parallel.Pending<String> failing = Parallel.supply(() -> {
            throw new RpcUnavailableException(-32001, "down");
        });
        assertThatThrownBy(failing::join).isExactlyInstanceOf(RpcUnavailableException.class).hasMessage("down");
    }

    @Test
    void joiningInProgramOrderThrowsTheFirstFailureOfThatOrder() {
        Parallel.Pending<String> first = Parallel.supply(() -> {
            throw new IllegalArgumentException("first");
        });
        Parallel.Pending<String> second = Parallel.supply(() -> {
            throw new IllegalStateException("second");
        });
        assertThatThrownBy(() -> {
            first.join();
            second.join();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theLoggingContextFollowsTheCallAndDoesNotLeak() {
        MDC.put("correlationId", "c-1");
        try {
            Parallel.Pending<String> seen = Parallel.supply(() -> MDC.get("correlationId"));
            assertThat(seen.join()).isEqualTo("c-1");
        } finally {
            MDC.clear();
        }
        assertThat(Parallel.supply(() -> MDC.get("correlationId")).join()).isNull();
    }
}
