package com.acme.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.ProductNotFoundException;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationStatus;
import com.acme.inventory.api.ReserveLine;
import com.acme.inventory.api.StockLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The inventory module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class InventoryModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "inventory");
    }

    @MockitoBean
    CatalogApi catalog;

    @Autowired
    InventoryApi inventory;

    @Test
    void stockIsValidatedThroughTheCatalog() {
        when(catalog.productExists("sku-widget")).thenReturn(true);
        when(catalog.productExists("sku-nope")).thenReturn(false);
        assertThat(inventory.getStock("sku-widget")).isEqualTo(new StockLevel("sku-widget", 100, "wh-1"));
        assertThatThrownBy(() -> inventory.getStock("sku-nope")).isExactlyInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reserveAndReleaseAreIdempotentAndAllOrNothing() {
        when(catalog.productExists("sku-gadget")).thenReturn(true);
        List<ReserveLine> lines = List.of(new ReserveLine("sku-gadget", 5));
        Reservation reservation = inventory.reserve("r-1", "o-1", lines);
        assertThat(inventory.reserve("r-1", "o-1", lines)).isEqualTo(reservation);
        assertThat(inventory.getStock("sku-gadget").available()).isEqualTo(45);

        assertThatThrownBy(() -> inventory.reserve("r-2", "o-2", List.of(new ReserveLine("sku-gadget", 1), new ReserveLine("sku-oos", 1))))
                .isExactlyInstanceOf(OutOfStockException.class);
        assertThat(inventory.getStock("sku-gadget").available()).as("nothing was taken").isEqualTo(45);

        inventory.release("rel-1", reservation.reservationId());
        inventory.release("rel-1", reservation.reservationId());
        assertThat(inventory.getStock("sku-gadget").available()).isEqualTo(50);
        assertThat(inventory.getReservation(reservation.reservationId()).status()).isEqualTo(ReservationStatus.RELEASED);
    }
}
