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

    @Override
    public void afterSingletonsInstantiated() {
        Set<Class<?>> apis = RpcApiScanner.scan(basePackages, beanFactory.getBeanClassLoader());
        List<String> problems = validator.validate(apis, api -> beanFactory.getBeanNamesForType(api, true, false).length > 0);
        if (!problems.isEmpty()) {
            throw new ModuleConfigurationException(String.join(System.lineSeparator(), problems));
        }
    }
}
