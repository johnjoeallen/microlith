package com.acme.orders.api;

public record PlaceOrderRequest(String requestId, String customerId, String sku, int quantity) {
}
