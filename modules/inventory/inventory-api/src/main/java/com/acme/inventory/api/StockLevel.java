package com.acme.inventory.api;

public record StockLevel(String sku, int available, String warehouse) {
}
