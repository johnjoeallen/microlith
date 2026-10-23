package com.acme.payments.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;

@RpcApi("payments")
public interface PaymentsApi {

    /** Idempotent by requestId. Throws {@link PaymentDeclinedException}. */
    @Idempotent
    Authorization authorize(String requestId, String customerId, long amountCents);

    @Idempotent
    Authorization capture(String requestId, String authId);

    /** The design calls this "void", which Java does not allow as a method name. */
    @Idempotent
    Authorization voidAuthorization(String requestId, String authId);
}
