package com.acme.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.Product;
import com.acme.customers.api.Address;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.Tier;
import com.acme.pricing.api.PricingApi;
import com.acme.pricing.api.Quote;
import com.acme.pricing.api.QuoteLine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The pricing module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class PricingModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "pricing");
    }

    @MockitoBean
    CatalogApi catalog;

    @MockitoBean
    CustomersApi customers;

    @Autowired
    PricingApi pricing;

    private static Customer customer(Tier tier) {
        return new Customer("c", "Name", "n@example.com", tier, new Address("1 St", "Dublin", "IE"));
    }

    @Test
    void goldCustomersGetTheTierDiscountFromPricingsOwnTable() {
        when(customers.getCustomer("c")).thenReturn(customer(Tier.GOLD));
        when(catalog.getProduct("sku-a")).thenReturn(new Product("sku-a", "A", 1000));
        Quote quote = pricing.quote("c", List.of(new QuoteLine("sku-a", 3)));
        assertThat(quote.subtotalCents()).isEqualTo(3000);
        assertThat(quote.discountCents()).isEqualTo(300);
        assertThat(quote.totalCents()).isEqualTo(2700);
        assertThat(quote.lines()).singleElement().satisfies(line -> assertThat(line.lineTotalCents()).isEqualTo(3000));
        verify(catalog).getProduct("sku-a");
    }

    @Test
    void standardCustomersPayFullPrice() {
        when(customers.getCustomer("c")).thenReturn(customer(Tier.STANDARD));
        when(catalog.getProduct("sku-a")).thenReturn(new Product("sku-a", "A", 250));
        Quote quote = pricing.quote("c", List.of(new QuoteLine("sku-a", 4)));
        assertThat(quote.discountCents()).isZero();
        assertThat(quote.totalCents()).isEqualTo(1000);
    }
}
