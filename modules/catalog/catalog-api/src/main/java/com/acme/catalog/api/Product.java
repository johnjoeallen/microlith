package com.acme.catalog.api;

public record Product(String sku, String name, long priceCents) {
}
