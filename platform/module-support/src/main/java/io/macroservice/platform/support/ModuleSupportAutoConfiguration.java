package io.macroservice.platform.support;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(ModuleProperties.class)
public class ModuleSupportAutoConfiguration {

    @Bean
    ModuleConfigurationValidator moduleConfigurationValidator(Environment environment, ModuleProperties properties) {
        return new ModuleConfigurationValidator(environment, properties);
    }

    @Bean
    @org.springframework.context.annotation.Profile("demo")
    FaultControl faultControl() {
        return new FaultControl();
    }

    @Bean
    ConnectionBudgetCheck connectionBudgetCheck(Environment environment, ModuleProperties properties) {
        return new ConnectionBudgetCheck(environment, properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.health.contributor.HealthIndicator.class)
    static class HealthConfiguration {

        /** Registered for the enabled modules only: a disabled module has no ModuleMigration bean. */
        @Bean
        ModulesHealthIndicator modulesHealthIndicator(ObjectProvider<ModuleMigration> migrations) {
            return new ModulesHealthIndicator(migrations.orderedStream().toList());
        }
    }

    @Bean
    static SmartInitializingSingleton moduleConfigurationValidation(ConfigurableListableBeanFactory beanFactory,
            ModuleConfigurationValidator validator) {
        List<String> packages = AutoConfigurationPackages.has((BeanFactory) beanFactory)
                ? AutoConfigurationPackages.get(beanFactory) : List.of();
        return new ModuleConfigurationValidation(beanFactory, validator, packages);
    }
}
