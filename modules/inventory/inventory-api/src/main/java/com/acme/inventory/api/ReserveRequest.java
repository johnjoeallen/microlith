package com.acme.inventory.api;

public record ReserveRequest(String requestId, String orderId, String sku, int quantity) {
}
