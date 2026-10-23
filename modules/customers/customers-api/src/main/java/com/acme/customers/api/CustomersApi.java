package com.acme.customers.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;

@RpcApi("customers")
public interface CustomersApi {

    @Idempotent
    Customer getCustomer(String customerId);

    @Idempotent
    PaymentMethod getPaymentMethod(String customerId);
}
