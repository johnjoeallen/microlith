package com.acme.customers.internal;

import com.acme.customers.api.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "customer")
class CustomerEntity {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private Tier tier;

    @Column(name = "line1")
    private String line1;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "payment_token")
    private String paymentToken;

    @Column(name = "payment_brand")
    private String paymentBrand;

    protected CustomerEntity() {
    }

    CustomerEntity(String customerId, String name, String email, Tier tier, String line1, String city, String country, String paymentToken, String paymentBrand) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.line1 = line1;
        this.city = city;
        this.country = country;
        this.paymentToken = paymentToken;
        this.paymentBrand = paymentBrand;
    }

    String getCustomerId() {
        return customerId;
    }

    void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    Tier getTier() {
        return tier;
    }

    void setTier(Tier tier) {
        this.tier = tier;
    }

    String getLine1() {
        return line1;
    }

    void setLine1(String line1) {
        this.line1 = line1;
    }

    String getCity() {
        return city;
    }

    void setCity(String city) {
        this.city = city;
    }

    String getCountry() {
        return country;
    }

    void setCountry(String country) {
        this.country = country;
    }

    String getPaymentToken() {
        return paymentToken;
    }

    void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }

    String getPaymentBrand() {
        return paymentBrand;
    }

    void setPaymentBrand(String paymentBrand) {
        this.paymentBrand = paymentBrand;
    }
}
