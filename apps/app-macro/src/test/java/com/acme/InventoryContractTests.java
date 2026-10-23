package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReserveRequest;
import com.acme.inventory.api.StockLevel;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.support.testing.SharedPostgres;
import io.macroservice.platform.rpc.RpcUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Contract of InventoryApi, run against every binding. Results and exception types must be identical; a failure in
 * only one binding is a parity bug.
 */
class InventoryContractTests {

    enum Mode { LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE }

    private static final String[] COMMON = TestApps.ORDERS_ELSEWHERE;

    private final java.util.List<ConfigurableApplicationContext> contexts = new java.util.ArrayList<>();

    @AfterEach
    void close() {
        contexts.forEach(ConfigurableApplicationContext::close);
    }

    private ConfigurableApplicationContext start(boolean web, String... args) {
        boolean database = java.util.Arrays.asList(args).contains("--modules.inventory.enabled=true");
        String[] all = java.util.stream.Stream.of(COMMON, args, database ? SharedPostgres.args("inventory") : new String[0])
                .flatMap(java.util.Arrays::stream).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(web ? org.springframework.boot.WebApplicationType.SERVLET : org.springframework.boot.WebApplicationType.NONE)
                .run(all);
        contexts.add(context);
        return context;
    }

    private InventoryApi inventory(Mode mode) {
        return switch (mode) {
            case LOCAL -> start(false, "--modules.inventory.enabled=true").getBean(InventoryApi.class);
            case LOOPBACK_CODEC -> start(false, "--modules.inventory.enabled=true", "--modules.inventory.binding=loopback-codec")
                    .getBean(InventoryApi.class);
            case LOOPBACK_HTTP -> start(true, "--server.port=0", "--modules.inventory.enabled=true", "--modules.inventory.binding=loopback-http")
                    .getBean(InventoryApi.class);
            case REMOTE -> {
                ConfigurableApplicationContext server = start(true, "--server.port=0", "--modules.inventory.enabled=true");
                String port = server.getEnvironment().getProperty("local.server.port");
                yield start(false, "--modules.inventory.enabled=false", "--modules.inventory.url=http://localhost:" + port + "/rpc/inventory")
                        .getBean(InventoryApi.class);
            }
        };
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void bindingIsWhatWeExpect(Mode mode) {
        assertThat(inventory(mode) instanceof RpcClientProxy).isEqualTo(mode != Mode.LOCAL);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void getStock(Mode mode) {
        InventoryApi api = inventory(mode);
        assertThat(api.getStock("sku-1")).isEqualTo(new StockLevel("sku-1", 100));
        assertThat(api.getStock("unknown")).isEqualTo(new StockLevel("unknown", 0));
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void reserveIsIdempotentAndReducesStock(Mode mode) {
        InventoryApi api = inventory(mode);
        ReserveRequest request = new ReserveRequest("req-1", "order-1", "sku-2", 2);
        Reservation first = api.reserve(request);
        assertThat(first.orderId()).isEqualTo("order-1");
        assertThat(first.sku()).isEqualTo("sku-2");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.reservationId()).isNotBlank();
        assertThat(api.reserve(request)).isEqualTo(first);
        assertThat(api.getStock("sku-2").available()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void outOfStockIsTheSameExceptionTypeEverywhere(Mode mode) {
        InventoryApi api = inventory(mode);
        assertThatThrownBy(() -> api.reserve(new ReserveRequest("req-2", "order-2", "sku-2", 50)))
                .isExactlyInstanceOf(OutOfStockException.class).hasMessage("Out of stock: sku-2");
        assertThat(api.getStock("sku-2").available()).isEqualTo(5);
    }

    @Test
    void unreachableRemoteIsReportedAsUnavailable() {
        InventoryApi api = start(false, "--modules.inventory.enabled=false", "--modules.inventory.url=http://127.0.0.1:1/rpc/inventory")
                .getBean(InventoryApi.class);
        assertThatThrownBy(() -> api.getStock("sku-1")).isInstanceOf(RpcUnavailableException.class);
    }
}
