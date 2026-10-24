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
@Table(name = "outbox")
class OutboxEntryEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "target")
    private String target;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "status")
    private String status;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at")
    private Instant createdAt;

    protected OutboxEntryEntity() {
    }

    OutboxEntryEntity(String id, String orderId, String target, String requestId, String payload, String status, int attempts, Instant nextAttemptAt, String lastError, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.target = target;
        this.requestId = requestId;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getOrderId() {
        return orderId;
    }

    void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    String getTarget() {
        return target;
    }

    void setTarget(String target) {
        this.target = target;
    }

    String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    String getPayload() {
        return payload;
    }

    void setPayload(String payload) {
        this.payload = payload;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    int getAttempts() {
        return attempts;
    }

    void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    String getLastError() {
        return lastError;
    }

    void setLastError(String lastError) {
        this.lastError = lastError;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
