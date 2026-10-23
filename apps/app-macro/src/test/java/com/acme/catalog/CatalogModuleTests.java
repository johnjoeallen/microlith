package com.acme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.Product;
import com.acme.catalog.api.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The catalog module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class CatalogModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "catalog");
    }

    @Autowired
    CatalogApi catalog;

    @Test
    void seededProductsAreFound() {
        assertThat(catalog.getProduct("sku-widget")).isEqualTo(new Product("sku-widget", "Widget", 1000));
        assertThat(catalog.productExists("sku-gadget")).isTrue();
        assertThat(catalog.productExists("sku-nope")).isFalse();
    }

    @Test
    void unknownProductsAreABusinessError() {
        assertThatThrownBy(() -> catalog.getProduct("sku-nope")).isExactlyInstanceOf(ProductNotFoundException.class).hasMessage("Unknown product: sku-nope");
    }
}
