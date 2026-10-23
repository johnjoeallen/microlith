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

    @Column(name = "available")
    private int available;

    @Column(name = "warehouse")
    private String warehouse;

    protected StockEntity() {
    }

    StockEntity(String sku, int available, String warehouse) {
        this.sku = sku;
        this.available = available;
        this.warehouse = warehouse;
    }

    String getSku() {
        return sku;
    }

    void setSku(String sku) {
        this.sku = sku;
    }

    int getAvailable() {
        return available;
    }

    void setAvailable(int available) {
        this.available = available;
    }

    String getWarehouse() {
        return warehouse;
    }

    void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
}
