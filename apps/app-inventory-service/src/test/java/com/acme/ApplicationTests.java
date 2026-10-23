package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest(properties = "spring.main.web-application-type=none")
class ApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void modulesVerify() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();
        assertThat(modules.stream().map(m -> m.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(Stream.of("inventory").toList());
    }
}
