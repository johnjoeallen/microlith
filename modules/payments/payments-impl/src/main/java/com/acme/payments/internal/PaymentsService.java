package com.acme.payments.internal;

import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.PaymentMethod;
import com.acme.payments.api.Authorization;
import com.acme.payments.api.AuthorizationNotFoundException;
import com.acme.payments.api.AuthorizationStatus;
import com.acme.payments.api.InvalidPaymentStateException;
import com.acme.payments.api.PaymentDeclinedException;
import com.acme.payments.api.PaymentsApi;
import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.ModuleService;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** authorize calls customers, so it is not transactional; capture and void only touch this module's data. */
@ModuleService(module = "payments")
class PaymentsService implements PaymentsApi {

    private final AuthorizationRepository authorizations;
    private final CustomersApi customers;
    private final ObjectProvider<FaultControl> faults;

    PaymentsService(AuthorizationRepository authorizations, CustomersApi customers, ObjectProvider<FaultControl> faults) {
        this.authorizations = authorizations;
        this.customers = customers;
        this.faults = faults;
    }

    @Override
    public Authorization authorize(String requestId, String customerId, long amountCents) {
        var existing = authorizations.findByRequestId(requestId);
        if (existing.isPresent()) {
            return toApi(existing.get());
        }
        PaymentMethod method = customers.getPaymentMethod(customerId);
        boolean forced = faults.getIfAvailable() != null && faults.getObject().flag("payments.decline");
        if (forced || method.token().startsWith("tok-declined")) {
            throw new PaymentDeclinedException("Payment declined for customer " + customerId);
        }
        return toApi(authorizations.save(new AuthorizationEntity(UUID.randomUUID().toString(), requestId, customerId, amountCents,
                AuthorizationStatus.AUTHORIZED)));
    }

    @Override
    @Transactional("paymentsTm")
    public Authorization capture(String requestId, String authId) {
        AuthorizationEntity auth = find(authId);
        if (auth.getStatus() == AuthorizationStatus.VOIDED) {
            throw new InvalidPaymentStateException("Authorization " + authId + " was voided and cannot be captured");
        }
        auth.setStatus(AuthorizationStatus.CAPTURED);
        return toApi(auth);
    }

    @Override
    @Transactional("paymentsTm")
    public Authorization voidAuthorization(String requestId, String authId) {
        AuthorizationEntity auth = find(authId);
        if (auth.getStatus() == AuthorizationStatus.CAPTURED) {
            throw new InvalidPaymentStateException("Authorization " + authId + " was captured and cannot be voided");
        }
        auth.setStatus(AuthorizationStatus.VOIDED);
        return toApi(auth);
    }

    private AuthorizationEntity find(String authId) {
        return authorizations.findById(authId).orElseThrow(() -> new AuthorizationNotFoundException("Unknown authorization: " + authId));
    }

    private static Authorization toApi(AuthorizationEntity e) {
        return new Authorization(e.getAuthId(), e.getCustomerId(), e.getAmountCents(), e.getStatus());
    }
}
