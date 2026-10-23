package io.macroservice.platform.support.fixtures.enabled;

import io.macroservice.platform.support.ModuleConfiguration;
import org.springframework.context.annotation.Bean;

@ModuleConfiguration(module = "foo")
public class FooConfig {
    @Bean
    String fooMarker() {
        return "foo-config-active";
    }
}
