package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import com.acme.inventory.api.InventoryApi;
import com.acme.inventory.api.OutOfStockException;
import com.acme.inventory.api.ReserveRequest;
import com.acme.orders.api.OrderStatus;
import com.acme.orders.api.OrdersApi;
import com.acme.orders.api.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.macroservice.platform.support.testing.SharedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest(properties = "spring.main.web-application-type=none")
@ActiveProfiles("demo")
class ApplicationTests {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry, "orders", "inventory");
    }

    @Autowired
    OrdersApi orders;

    @Autowired
    InventoryApi inventory;

    @Test
    void stubImplementationsAreWired() {
        var order = orders.placeOrder(new PlaceOrderRequest("r-1", "cust-1", "sku-1", 2));
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.placeOrder(new PlaceOrderRequest("r-1", "cust-1", "sku-1", 2))).isEqualTo(order);
        assertThat(orders.getOrder(order.orderId())).isEqualTo(order);
        assertThat(inventory.getStock("sku-1").available()).isEqualTo(98);
        assertThatThrownBy(() -> inventory.reserve(new ReserveRequest("r-2", "o-2", "sku-2", 50)))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void modulesVerify() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();
        assertThat(modules.stream().map(m -> m.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(Stream.of("inventory", "orders").toList());
    }
}
