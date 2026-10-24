package com.acme.payments.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;

@RpcApi("payments")
public interface PaymentsApi {

    /** Idempotent by requestId. Throws {@link PaymentDeclinedException}. */
    @Idempotent
    @Operation(summary = "Authorizes an amount for a customer")
    Authorization authorize(String requestId, String customerId, long amountCents);

    @Idempotent
    @Operation(summary = "Captures an authorized payment")
    Authorization capture(String requestId, String authId);

    /** The design calls this "void", which Java does not allow as a method name. */
    @Idempotent
    @Operation(summary = "Voids an authorization (the design name is \"void\")")
    Authorization voidAuthorization(String requestId, String authId);
}
