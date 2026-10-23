package com.acme.shipping.api;

public record Shipment(String shipmentId, String orderId, String customerId, String reservationId, String destination) {
}
