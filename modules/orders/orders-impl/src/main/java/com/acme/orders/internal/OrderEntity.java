package com.acme.orders.internal;

import com.acme.orders.api.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "lines")
    private String lines;

    @Column(name = "subtotal_cents")
    private long subtotalCents;

    @Column(name = "discount_cents")
    private long discountCents;

    @Column(name = "shipping_cents")
    private long shippingCents;

    @Column(name = "total_cents")
    private long totalCents;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "authorization_id")
    private String authorizationId;

    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason")
    private String failureReason;

    protected OrderEntity() {
    }

    OrderEntity(String orderId, String requestId, String customerId, OrderStatus status, String lines, long subtotalCents, long discountCents, long shippingCents, long totalCents, String reservationId, String authorizationId, String shipmentId, String failureCode, String failureReason) {
        this.orderId = orderId;
        this.requestId = requestId;
        this.customerId = customerId;
        this.status = status;
        this.lines = lines;
        this.subtotalCents = subtotalCents;
        this.discountCents = discountCents;
        this.shippingCents = shippingCents;
        this.totalCents = totalCents;
        this.reservationId = reservationId;
        this.authorizationId = authorizationId;
        this.shipmentId = shipmentId;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
    }

    String getOrderId() {
        return orderId;
    }

    void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    String getCustomerId() {
        return customerId;
    }

    void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    OrderStatus getStatus() {
        return status;
    }

    void setStatus(OrderStatus status) {
        this.status = status;
    }

    String getLines() {
        return lines;
    }

    void setLines(String lines) {
        this.lines = lines;
    }

    long getSubtotalCents() {
        return subtotalCents;
    }

    void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    long getDiscountCents() {
        return discountCents;
    }

    void setDiscountCents(long discountCents) {
        this.discountCents = discountCents;
    }

    long getShippingCents() {
        return shippingCents;
    }

    void setShippingCents(long shippingCents) {
        this.shippingCents = shippingCents;
    }

    long getTotalCents() {
        return totalCents;
    }

    void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    String getReservationId() {
        return reservationId;
    }

    void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    String getAuthorizationId() {
        return authorizationId;
    }

    void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    String getShipmentId() {
        return shipmentId;
    }

    void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    String getFailureCode() {
        return failureCode;
    }

    void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    String getFailureReason() {
        return failureReason;
    }

    void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
