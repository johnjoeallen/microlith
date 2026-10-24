package com.acme.customers.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;

@RpcApi("customers")
public interface CustomersApi {

    @Idempotent
    @Operation(summary = "Gets a customer")
    Customer getCustomer(String customerId);

    @Idempotent
    @Operation(summary = "Gets the customer's payment method")
    PaymentMethod getPaymentMethod(String customerId);
}
