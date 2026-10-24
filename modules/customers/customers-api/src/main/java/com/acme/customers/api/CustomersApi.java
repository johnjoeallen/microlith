package com.acme.customers.api;

import io.macroservice.platform.rpc.Cached;
import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;
import io.swagger.v3.oas.annotations.Operation;

@RpcApi("customers")
public interface CustomersApi {

    @Idempotent
    @Cached
    @Operation(summary = "Gets a customer")
    Customer getCustomer(String customerId);

    @Idempotent
    @Cached
    @Operation(summary = "Gets the customer's payment method")
    PaymentMethod getPaymentMethod(String customerId);
}
