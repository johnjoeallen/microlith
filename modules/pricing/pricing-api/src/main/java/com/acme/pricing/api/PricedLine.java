package com.acme.pricing.api;

public record PricedLine(String sku, int quantity, long unitPriceCents, long lineTotalCents) {
}
