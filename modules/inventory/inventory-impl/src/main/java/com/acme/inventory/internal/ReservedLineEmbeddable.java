package com.acme.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class ReservedLineEmbeddable {

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReservedLineEmbeddable() {
    }

    ReservedLineEmbeddable(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    String getSku() {
        return sku;
    }

    int getQuantity() {
        return quantity;
    }
}
