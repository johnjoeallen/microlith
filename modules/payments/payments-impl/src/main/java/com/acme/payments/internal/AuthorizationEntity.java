package com.acme.payments.internal;

import com.acme.payments.api.AuthorizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "payment_authorization")
class AuthorizationEntity {

    @Id
    @Column(name = "auth_id")
    private String authId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "amount_cents")
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AuthorizationStatus status;

    protected AuthorizationEntity() {
    }

    AuthorizationEntity(String authId, String requestId, String customerId, long amountCents, AuthorizationStatus status) {
        this.authId = authId;
        this.requestId = requestId;
        this.customerId = customerId;
        this.amountCents = amountCents;
        this.status = status;
    }

    String getAuthId() {
        return authId;
    }

    void setAuthId(String authId) {
        this.authId = authId;
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

    long getAmountCents() {
        return amountCents;
    }

    void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    AuthorizationStatus getStatus() {
        return status;
    }

    void setStatus(AuthorizationStatus status) {
        this.status = status;
    }
}
