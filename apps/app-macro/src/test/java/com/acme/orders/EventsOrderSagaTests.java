package com.acme.orders;

import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The order saga with follow-ups delivered as Spring Modulith events ({@code modules.orders.async=events}). */
@ApplicationModuleTest
@ActiveProfiles("demo")
class EventsOrderSagaTests extends OrderSagaTests {

    @DynamicPropertySource
    static void mode(DynamicPropertyRegistry registry) {
        registry.add("modules.orders.async", () -> "events");
    }

    @Override
    String followUpStatus(String requestId) throws Exception {
        return scalar("select status from event_publication where serialized_event like ?", "%\"requestId\":\"" + requestId + "\"%");
    }

    @Override
    String deliveredStatus() {
        return "COMPLETED";
    }

    @Override
    String followUpCount(String orderRequestId) throws Exception {
        return scalar("select count(*) from event_publication where serialized_event like ?", "%" + orderRequestId + "%");
    }
}
