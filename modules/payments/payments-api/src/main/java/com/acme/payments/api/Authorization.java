package com.acme.payments.api;

public record Authorization(String authId, String customerId, long amountCents, AuthorizationStatus status) {
}
