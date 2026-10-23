package com.acme.inventory.api;

import java.util.List;

public record Reservation(String reservationId, String orderId, ReservationStatus status, List<ReserveLine> lines) {
}
