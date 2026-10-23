package com.acme.customers.api;

public record PaymentMethod(String customerId, String token, String brand) {
}
