package com.acme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
class ModulithGuardConfiguration {

    @Bean
    static ModulithGuard modulithGuard(Environment environment) {
        return new ModulithGuard(environment);
    }
}
