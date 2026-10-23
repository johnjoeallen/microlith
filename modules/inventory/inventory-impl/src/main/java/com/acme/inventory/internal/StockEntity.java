package com.acme.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
class StockEntity {

    @Id
    @Column(name = "sku")
    private String sku;

    @Column(name = "available", nullable = false)
    private int available;

    protected StockEntity() {
    }

    StockEntity(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    String getSku() {
        return sku;
    }

    int getAvailable() {
        return available;
    }

    void setAvailable(int available) {
        this.available = available;
    }
}
