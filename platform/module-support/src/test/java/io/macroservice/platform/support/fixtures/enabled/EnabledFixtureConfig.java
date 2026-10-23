package io.macroservice.platform.support.fixtures.enabled;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage
@ComponentScan
public class EnabledFixtureConfig {
}
