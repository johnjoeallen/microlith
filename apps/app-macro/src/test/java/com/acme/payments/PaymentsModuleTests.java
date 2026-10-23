package com.acme.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.PaymentMethod;
import com.acme.payments.api.Authorization;
import com.acme.payments.api.AuthorizationStatus;
import com.acme.payments.api.InvalidPaymentStateException;
import com.acme.payments.api.PaymentDeclinedException;
import com.acme.payments.api.PaymentsApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The payments module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class PaymentsModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "payments");
    }

    @MockitoBean
    CustomersApi customers;

    @Autowired
    PaymentsApi payments;

    @Test
    void authorizeIsIdempotentAndCaptureAndVoidFollowTheStateMachine() {
        when(customers.getPaymentMethod("c")).thenReturn(new PaymentMethod("c", "tok-ok", "VISA"));
        Authorization auth = payments.authorize("a-1", "c", 2700);
        assertThat(auth.status()).isEqualTo(AuthorizationStatus.AUTHORIZED);
        assertThat(payments.authorize("a-1", "c", 2700)).isEqualTo(auth);

        assertThat(payments.capture("c-1", auth.authId()).status()).isEqualTo(AuthorizationStatus.CAPTURED);
        assertThat(payments.capture("c-1", auth.authId()).status()).isEqualTo(AuthorizationStatus.CAPTURED);
        assertThatThrownBy(() -> payments.voidAuthorization("v-1", auth.authId())).isExactlyInstanceOf(InvalidPaymentStateException.class);

        Authorization other = payments.authorize("a-2", "c", 100);
        assertThat(payments.voidAuthorization("v-2", other.authId()).status()).isEqualTo(AuthorizationStatus.VOIDED);
        assertThat(payments.voidAuthorization("v-2", other.authId()).status()).isEqualTo(AuthorizationStatus.VOIDED);
        assertThatThrownBy(() -> payments.capture("c-2", other.authId())).isExactlyInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void aDeclinedCardIsABusinessError() {
        when(customers.getPaymentMethod("d")).thenReturn(new PaymentMethod("d", "tok-declined", "VISA"));
        assertThatThrownBy(() -> payments.authorize("a-3", "d", 100)).isExactlyInstanceOf(PaymentDeclinedException.class);
    }
}
