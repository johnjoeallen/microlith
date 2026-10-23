package com.acme.shipping.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment")
class ShipmentEntity {

    @Id
    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "destination")
    private String destination;

    protected ShipmentEntity() {
    }

    ShipmentEntity(String shipmentId, String requestId, String orderId, String customerId, String reservationId, String destination) {
        this.shipmentId = shipmentId;
        this.requestId = requestId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.reservationId = reservationId;
        this.destination = destination;
    }

    String getShipmentId() {
        return shipmentId;
    }

    void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    String getOrderId() {
        return orderId;
    }

    void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    String getCustomerId() {
        return customerId;
    }

    void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    String getReservationId() {
        return reservationId;
    }

    void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    String getDestination() {
        return destination;
    }

    void setDestination(String destination) {
        this.destination = destination;
    }
}
