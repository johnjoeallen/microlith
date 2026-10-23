package io.macroservice.platform.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;
import org.springframework.context.annotation.Configuration;

/**
 * Module-aware replacement for {@code @Configuration}: the bean exists only when {@code modules.<module>.enabled} is
 * true. Plain {@code @Service}, {@code @Component} and {@code @Configuration} are forbidden in module packages.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@Conditional(ModuleEnabledCondition.class)
public @interface ModuleConfiguration {

    /** The module this bean belongs to. */
    String module();

    @AliasFor(annotation = Configuration.class, attribute = "value")
    String name() default "";

    @AliasFor(annotation = Configuration.class, attribute = "proxyBeanMethods")
    boolean proxyBeanMethods() default true;
}
