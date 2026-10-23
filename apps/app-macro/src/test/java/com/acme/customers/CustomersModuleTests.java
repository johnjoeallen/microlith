package com.acme.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomerNotFoundException;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.PaymentMethod;
import com.acme.customers.api.Tier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The customers module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class CustomersModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "customers");
    }

    @Autowired
    CustomersApi customers;

    @Test
    void customersCarryTierAddressAndPaymentMethod() {
        Customer gold = customers.getCustomer("cust-gold");
        assertThat(gold.tier()).isEqualTo(Tier.GOLD);
        assertThat(gold.address().country()).isEqualTo("IE");
        assertThat(customers.getCustomer("cust-standard").tier()).isEqualTo(Tier.STANDARD);
        assertThat(customers.getPaymentMethod("cust-declined")).isEqualTo(new PaymentMethod("cust-declined", "tok-declined", "VISA"));
    }

    @Test
    void unknownCustomersAreABusinessError() {
        assertThatThrownBy(() -> customers.getCustomer("nobody")).isExactlyInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> customers.getPaymentMethod("nobody")).isExactlyInstanceOf(CustomerNotFoundException.class);
    }
}
