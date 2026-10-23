package com.acme.shipping.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "undeliverable_sku")
class UndeliverableSkuEntity {

    @Id
    @Column(name = "sku")
    private String sku;

    protected UndeliverableSkuEntity() {
    }

    UndeliverableSkuEntity(String sku) {
        this.sku = sku;
    }

    String getSku() {
        return sku;
    }

    void setSku(String sku) {
        this.sku = sku;
    }
}
