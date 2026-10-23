package com.acme.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation")
class ReservationEntity {

    @Id
    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReservationEntity() {
    }

    ReservationEntity(String reservationId, String requestId, String orderId, String sku, int quantity) {
        this.reservationId = reservationId;
        this.requestId = requestId;
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
    }

    String getReservationId() {
        return reservationId;
    }

    String getOrderId() {
        return orderId;
    }

    String getSku() {
        return sku;
    }

    int getQuantity() {
        return quantity;
    }
}
