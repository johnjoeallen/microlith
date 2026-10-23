package com.acme;

import com.acme.inventory.api.InventoryApi;
import com.acme.orders.api.OrdersApi;
import io.macroservice.demo.scenarios.OrderScenarios;
import io.macroservice.platform.support.testing.SharedPostgres;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The whole order-fulfilment domain in one process (every call local), against a real PostgreSQL. */
@SpringBootTest(properties = "spring.main.web-application-type=none")
@ActiveProfiles("demo")
class FullDomainTests {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry, "catalog", "customers", "pricing", "inventory", "payments", "shipping", "notifications", "orders");
    }

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    OrdersApi orders;

    @Autowired
    InventoryApi inventory;

    private OrderScenarios scenarios() {
        return new OrderScenarios(orders, inventory, ScenarioSupport.probe(List.of(context)), ScenarioSupport.faults(List.of(context)));
    }

    @Test
    void happyPath() {
        scenarios().happyPath();
    }

    @Test
    void tierDiscountThroughTheChain() {
        scenarios().tierDiscountThroughTheChain();
    }

    @Test
    void outOfStock() {
        scenarios().outOfStock();
    }

    @Test
    void paymentDeclinedWithCompensation() {
        scenarios().paymentDeclinedWithCompensation();
    }

    @Test
    void shippingFailureWithTwoCompensations() {
        scenarios().shippingFailureWithTwoCompensations();
    }

    @Test
    void dependencyDown() {
        scenarios().dependencyDown();
    }

    @Test
    void idempotentRetry() {
        scenarios().idempotentRetry();
    }
}
