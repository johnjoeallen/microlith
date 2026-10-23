package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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
        SharedPostgres.register(registry, "catalog", "customers", "pricing", "inventory", "payments", "shipping", "notifications", "orders");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void modulesVerify() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();
        assertThat(modules.stream().map(m -> m.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(Stream.of("catalog", "customers", "inventory", "notifications", "orders", "payments", "pricing", "shipping").toList());
    }
}
