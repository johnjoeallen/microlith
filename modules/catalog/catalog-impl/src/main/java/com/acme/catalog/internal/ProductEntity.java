package com.acme.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
class ProductEntity {

    @Id
    @Column(name = "sku")
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "price_cents")
    private long priceCents;

    protected ProductEntity() {
    }

    ProductEntity(String sku, String name, long priceCents) {
        this.sku = sku;
        this.name = name;
        this.priceCents = priceCents;
    }

    String getSku() {
        return sku;
    }

    void setSku(String sku) {
        this.sku = sku;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    long getPriceCents() {
        return priceCents;
    }

    void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }
}
