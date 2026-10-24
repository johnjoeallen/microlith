package com.acme.orders;

import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The order saga with follow-ups delivered by the outbox relay ({@code modules.orders.async=outbox}, the default). */
@ApplicationModuleTest
@ActiveProfiles("demo")
class OutboxOrderSagaTests extends OrderSagaTests {

    @DynamicPropertySource
    static void mode(DynamicPropertyRegistry registry) {
        registry.add("modules.orders.async", () -> "outbox");
    }

    @Override
    String followUpStatus(String requestId) throws Exception {
        return scalar("select status from outbox where request_id = ?", requestId);
    }

    @Override
    String deliveredStatus() {
        return "DONE";
    }

    @Override
    String followUpCount(String orderRequestId) throws Exception {
        return scalar("select count(*) from outbox where order_id = (select order_id from orders where request_id = ?)", orderRequestId);
    }
}
