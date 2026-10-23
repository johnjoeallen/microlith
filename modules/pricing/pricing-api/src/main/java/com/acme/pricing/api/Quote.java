package com.acme.pricing.api;

import java.util.List;

public record Quote(String customerId, List<PricedLine> lines, long subtotalCents, long discountCents, long totalCents) {
}
