package io.macroservice.platform.support;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/** Runs {@link ModuleConfigurationValidator} once all singletons exist and fails startup on any problem. */
class ModuleConfigurationValidation implements SmartInitializingSingleton {

    private final ConfigurableListableBeanFactory beanFactory;
    private final ModuleConfigurationValidator validator;
    private final List<String> basePackages;

    ModuleConfigurationValidation(ConfigurableListableBeanFactory beanFactory, ModuleConfigurationValidator validator,
            List<String> basePackages) {
        this.beanFactory = beanFactory;
        this.validator = validator;
        this.basePackages = basePackages;
    }

    private boolean hasLocalImplementation(Class<?> api) {
        for (String name : beanFactory.getBeanNamesForType(api, true, false)) {
            boolean proxy = beanFactory.containsBeanDefinition(name)
                    && beanFactory.getBeanDefinition(name).hasAttribute(ModuleBeans.CLIENT_PROXY_ATTRIBUTE);
            if (!proxy) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Set<Class<?>> apis = RpcApiScanner.scan(basePackages, beanFactory.getBeanClassLoader());
        List<String> problems = validator.validate(apis, this::hasLocalImplementation);
        if (!problems.isEmpty()) {
            throw new ModuleConfigurationException(String.join(System.lineSeparator(), problems));
        }
    }
}
