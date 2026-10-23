package com.acme.catalog.internal;

import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.Product;
import com.acme.catalog.api.ProductNotFoundException;
import io.macroservice.platform.support.ModuleService;

@ModuleService(module = "catalog")
class CatalogService implements CatalogApi {

    private final ProductRepository products;

    CatalogService(ProductRepository products) {
        this.products = products;
    }

    @Override
    public Product getProduct(String sku) {
        return products.findById(sku).map(p -> new Product(p.getSku(), p.getName(), p.getPriceCents()))
                .orElseThrow(() -> new ProductNotFoundException("Unknown product: " + sku));
    }

    @Override
    public boolean productExists(String sku) {
        return products.existsById(sku);
    }
}
