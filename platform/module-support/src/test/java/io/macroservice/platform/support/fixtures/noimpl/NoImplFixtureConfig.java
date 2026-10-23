package io.macroservice.platform.support.fixtures.noimpl;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage
public class NoImplFixtureConfig {
}
