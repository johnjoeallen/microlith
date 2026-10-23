package com.acme.pricing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tier_discount")
class TierDiscountEntity {

    @Id
    @Column(name = "tier")
    private String tier;

    @Column(name = "percent")
    private int percent;

    protected TierDiscountEntity() {
    }

    TierDiscountEntity(String tier, int percent) {
        this.tier = tier;
        this.percent = percent;
    }

    String getTier() {
        return tier;
    }

    void setTier(String tier) {
        this.tier = tier;
    }

    int getPercent() {
        return percent;
    }

    void setPercent(int percent) {
        this.percent = percent;
    }
}
