package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.catalog.api.ProductNotFoundException;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationNotFoundException;
import com.acme.inventory.api.ReservationStatus;
import com.acme.inventory.api.ReserveLine;
import com.acme.inventory.api.StockLevel;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.rpc.RpcUnavailableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Contract of InventoryApi (which itself calls catalog), run against every binding. Results and exception types must be
 * identical; a failure in only one binding is a parity bug.
 */
class InventoryContractTests {

    enum Mode { LOCAL, LOOPBACK_CODEC, LOOPBACK_HTTP, REMOTE }

    private final TestApps apps = new TestApps();

    @AfterEach
    void close() {
        apps.close();
    }

    private InventoryApi inventory(Mode mode) {
        List<String> hosted = List.of("inventory", "catalog");
        return switch (mode) {
            case LOCAL -> apps.start(false, TestApps.local("inventory", "catalog")).getBean(InventoryApi.class);
            case LOOPBACK_CODEC -> apps.start(false, TestApps.local("inventory", "catalog"), TestApps.binding("loopback-codec", hosted))
                    .getBean(InventoryApi.class);
            case LOOPBACK_HTTP -> apps.start(true, TestApps.local("inventory", "catalog"), TestApps.binding("loopback-http", hosted),
                    TestApps.args("--server.port=0")).getBean(InventoryApi.class);
            case REMOTE -> {
                ConfigurableApplicationContext server = apps.start(true, TestApps.local("inventory", "catalog"), TestApps.args("--server.port=0"));
                String port = server.getEnvironment().getProperty("local.server.port");
                yield apps.start(false, TestApps.demo(), TestApps.topology(Map.of("inventory", "http://localhost:" + port + "/rpc/inventory")))
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
    void getStockValidatesTheSkuThroughTheCatalog(Mode mode) {
        InventoryApi api = inventory(mode);
        assertThat(api.getStock("sku-widget")).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
        assertThat(api.getStock("sku-oos")).isEqualTo(new StockLevel("sku-oos", 0, "wh-1"));
        assertThatThrownBy(() -> api.getStock("sku-nope")).isExactlyInstanceOf(ProductNotFoundException.class).hasMessage("Unknown product: sku-nope");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void reserveIsIdempotentAndReleaseGivesStockBack(Mode mode) {
        InventoryApi api = inventory(mode);
        List<ReserveLine> lines = List.of(new ReserveLine("sku-gadget", 2), new ReserveLine("sku-widget", 1));
        Reservation first = api.reserve("req-1", "order-1", lines);
        assertThat(first.orderId()).isEqualTo("order-1");
        assertThat(first.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(first.lines()).containsExactlyInAnyOrderElementsOf(lines);
        assertThat(api.reserve("req-1", "order-1", lines)).isEqualTo(first);
        assertThat(api.getStock("sku-gadget").available()).isEqualTo(48);
        assertThat(api.getReservation(first.reservationId())).isEqualTo(first);

        api.release("rel-1", first.reservationId());
        api.release("rel-1", first.reservationId());
        assertThat(api.getStock("sku-gadget").available()).isEqualTo(50);
        assertThat(api.getReservation(first.reservationId()).status()).isEqualTo(ReservationStatus.RELEASED);
        assertThatThrownBy(() -> api.release("rel-2", "nope")).isExactlyInstanceOf(ReservationNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void outOfStockIsTheSameExceptionTypeEverywhereAndReservesNothing(Mode mode) {
        InventoryApi api = inventory(mode);
        assertThatThrownBy(() -> api.reserve("req-2", "order-2", List.of(new ReserveLine("sku-gadget", 1), new ReserveLine("sku-oos", 1))))
                .isExactlyInstanceOf(OutOfStockException.class).hasMessage("Out of stock: sku-oos");
        assertThat(api.getStock("sku-gadget").available()).as("all or nothing").isEqualTo(50);
    }

    @Test
    void unreachableRemoteIsReportedAsUnavailable() {
        InventoryApi api = apps.start(false, TestApps.demo(), TestApps.topology(Map.of())).getBean(InventoryApi.class);
        assertThatThrownBy(() -> api.getStock("sku-widget")).isInstanceOf(RpcUnavailableException.class);
    }
}
