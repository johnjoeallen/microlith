package io.macroservice.platform.support;

import io.macroservice.platform.rpc.RpcApi;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/** Finds {@link RpcApi} interfaces on the classpath. */
public final class RpcApiScanner {

    private RpcApiScanner() {
    }

    public static Set<Class<?>> scan(List<String> basePackages, ClassLoader classLoader) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isInterface();
            }
        };
        provider.addIncludeFilter(new AnnotationTypeFilter(RpcApi.class));
        Set<Class<?>> result = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : provider.findCandidateComponents(basePackage)) {
                try {
                    result.add(ClassUtils.forName(candidate.getBeanClassName(), classLoader));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load @RpcApi interface " + candidate.getBeanClassName(), e);
                }
            }
        }
        return result;
    }
}
