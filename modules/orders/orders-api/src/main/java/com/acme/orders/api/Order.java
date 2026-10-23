package com.acme.orders.api;

import java.util.List;

public record Order(String orderId, String requestId, String customerId, OrderStatus status, List<OrderLine> lines,
        long subtotalCents, long discountCents, long shippingCents, long totalCents, String failureReason) {
}
