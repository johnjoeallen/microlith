package com.acme.customers.api;

public record Customer(String customerId, String name, String email, Tier tier, Address address) {
}
