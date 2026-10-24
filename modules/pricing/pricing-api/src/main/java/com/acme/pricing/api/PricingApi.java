package com.acme.pricing.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RpcApi("pricing")
public interface PricingApi {

    @Idempotent
    @Operation(summary = "Prices the lines for a customer")
    Quote quote(String customerId, List<QuoteLine> lines);
}
