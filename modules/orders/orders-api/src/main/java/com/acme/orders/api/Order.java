package com.acme.orders.api;

public record Order(String orderId, String requestId, String customerId, String sku, int quantity, OrderStatus status) {
}
