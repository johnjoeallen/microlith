package com.acme.shipping.api;

public record ShippingQuote(String customerId, String warehouse, long costCents) {
}
