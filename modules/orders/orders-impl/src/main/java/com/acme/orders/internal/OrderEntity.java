package com.acme.orders.internal;

import com.acme.orders.api.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    protected OrderEntity() {
    }

    OrderEntity(String orderId, String requestId, String customerId, String sku, int quantity, OrderStatus status) {
        this.orderId = orderId;
        this.requestId = requestId;
        this.customerId = customerId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
    }

    String getOrderId() {
        return orderId;
    }

    String getRequestId() {
        return requestId;
    }

    String getCustomerId() {
        return customerId;
    }

    String getSku() {
        return sku;
    }

    int getQuantity() {
        return quantity;
    }

    OrderStatus getStatus() {
        return status;
    }

    void setStatus(OrderStatus status) {
        this.status = status;
    }
}
