package com.acme.pricing.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import java.util.List;

@RpcApi("pricing")
public interface PricingApi {

    @Idempotent
    Quote quote(String customerId, List<QuoteLine> lines);
}
