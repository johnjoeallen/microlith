package com.acme.inventory.api;

public record Reservation(String reservationId, String orderId, String sku, int quantity) {
}
