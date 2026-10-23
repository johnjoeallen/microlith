package com.acme.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.customers.api.Address;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.Tier;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.Reservation;
import com.acme.inventory.api.ReservationStatus;
import com.acme.inventory.api.ReserveLine;
import com.acme.inventory.api.StockLevel;
import com.acme.shipping.api.ShipLine;
import com.acme.shipping.api.Shipment;
import com.acme.shipping.api.ShippingApi;
import com.acme.shipping.api.ShippingQuote;
import com.acme.shipping.api.UndeliverableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The shipping module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class ShippingModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "shipping");
    }

    @MockitoBean
    CustomersApi customers;

    @MockitoBean
    InventoryApi inventory;

    @Autowired
    ShippingApi shipping;

    private void customerIn(String country) {
        when(customers.getCustomer("c")).thenReturn(new Customer("c", "N", "n@example.com", Tier.STANDARD, new Address("1 St", "Town", country)));
    }

    @Test
    void quoteUsesTheAddressAndTheWarehouse() {
        customerIn("IE");
        when(inventory.getStock("sku-a")).thenReturn(new StockLevel("sku-a", 10, "wh-7"));
        ShippingQuote quote = shipping.quote("c", List.of(new ShipLine("sku-a", 3)));
        assertThat(quote).isEqualTo(new ShippingQuote("c", "wh-7", 500 + 3 * 100));
        customerIn("FR");
        assertThat(shipping.quote("c", List.of(new ShipLine("sku-a", 1))).costCents()).isEqualTo(500 + 100 + 1000);
    }

    @Test
    void shipmentsAreIdempotentAndUndeliverableSkusFail() {
        customerIn("IE");
        when(inventory.getReservation("res-ok")).thenReturn(new Reservation("res-ok", "o", ReservationStatus.RESERVED, List.of(new ReserveLine("sku-widget", 1))));
        when(inventory.getReservation("res-bad")).thenReturn(new Reservation("res-bad", "o", ReservationStatus.RESERVED, List.of(new ReserveLine("sku-undeliverable", 1))));
        Shipment shipment = shipping.createShipment("s-1", "o", "c", "res-ok");
        assertThat(shipment.destination()).isEqualTo("1 St, Town, IE");
        assertThat(shipping.createShipment("s-1", "o", "c", "res-ok")).isEqualTo(shipment);
        assertThatThrownBy(() -> shipping.createShipment("s-2", "o", "c", "res-bad")).isExactlyInstanceOf(UndeliverableException.class);
    }
}
