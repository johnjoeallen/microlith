package com.acme;

import io.macroservice.platform.support.ModuleConfigurationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * app-modulith is the plain modular monolith: every module runs here and every call is a direct call. Any module that is
 * disabled, reached by URL, or bound to a client proxy (remote, loopback-codec, loopback-http) is a configuration error;
 * use app-macro for those shapes. Runs before the RPC registrar so that the message is this one.
 */
class ModulithGuard implements BeanDefinitionRegistryPostProcessor, Ordered {

    private final Environment environment;

    ModulithGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        record Settings(Boolean enabled, String binding, String url) {
        }
        var modules = Binder.get(environment).bind("modules", Bindable.mapOf(String.class, Settings.class)).orElse(java.util.Map.of());
        List<String> problems = new ArrayList<>();
        modules.forEach((name, settings) -> {
            if (!Boolean.TRUE.equals(settings.enabled())) {
                problems.add("module '%s' is not enabled".formatted(name));
            }
            if (settings.binding() != null && !settings.binding().equalsIgnoreCase("local")) {
                problems.add("module '%s' has binding %s".formatted(name, settings.binding()));
            }
        });
        if (!problems.isEmpty()) {
            throw new ModuleConfigurationException("app-modulith runs every module locally with direct calls, but " + String.join("; ", problems)
                    + ". Use app-macro for any other shape.");
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
