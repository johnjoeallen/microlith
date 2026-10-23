package com.acme.pricing.internal;

import com.acme.catalog.api.CatalogApi;
import com.acme.catalog.api.Product;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.pricing.api.PricedLine;
import com.acme.pricing.api.PricingApi;
import com.acme.pricing.api.Quote;
import com.acme.pricing.api.QuoteLine;
import io.macroservice.platform.support.ModuleService;
import java.util.ArrayList;
import java.util.List;

@ModuleService(module = "pricing")
class PricingService implements PricingApi {

    private final CatalogApi catalog;
    private final CustomersApi customers;
    private final TierDiscountRepository discounts;

    PricingService(CatalogApi catalog, CustomersApi customers, TierDiscountRepository discounts) {
        this.catalog = catalog;
        this.customers = customers;
        this.discounts = discounts;
    }

    @Override
    public Quote quote(String customerId, List<QuoteLine> lines) {
        Customer customer = customers.getCustomer(customerId);
        int percent = discounts.findById(customer.tier().name()).map(TierDiscountEntity::getPercent).orElse(0);
        List<PricedLine> priced = new ArrayList<>();
        long subtotal = 0;
        for (QuoteLine line : lines) {
            Product product = catalog.getProduct(line.sku());
            long lineTotal = product.priceCents() * line.quantity();
            priced.add(new PricedLine(line.sku(), line.quantity(), product.priceCents(), lineTotal));
            subtotal += lineTotal;
        }
        long discount = subtotal * percent / 100;
        return new Quote(customerId, priced, subtotal, discount, subtotal - discount);
    }
}
