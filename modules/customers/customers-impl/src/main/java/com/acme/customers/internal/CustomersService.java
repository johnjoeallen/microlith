package com.acme.customers.internal;

import com.acme.customers.api.Address;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomerNotFoundException;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.PaymentMethod;
import io.macroservice.platform.support.ModuleService;

@ModuleService(module = "customers")
class CustomersService implements CustomersApi {

    private final CustomerRepository customers;

    CustomersService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public Customer getCustomer(String customerId) {
        CustomerEntity c = find(customerId);
        return new Customer(c.getCustomerId(), c.getName(), c.getEmail(), c.getTier(), new Address(c.getLine1(), c.getCity(), c.getCountry()));
    }

    @Override
    public PaymentMethod getPaymentMethod(String customerId) {
        CustomerEntity c = find(customerId);
        return new PaymentMethod(c.getCustomerId(), c.getPaymentToken(), c.getPaymentBrand());
    }

    private CustomerEntity find(String customerId) {
        return customers.findById(customerId).orElseThrow(() -> new CustomerNotFoundException("Unknown customer: " + customerId));
    }
}
