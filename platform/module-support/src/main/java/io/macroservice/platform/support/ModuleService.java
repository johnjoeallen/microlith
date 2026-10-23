package io.macroservice.platform.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Service;

/**
 * Module-aware replacement for {@code @Service}: the bean exists only when {@code modules.<module>.enabled} is
 * true. Plain {@code @Service}, {@code @Component} and {@code @Configuration} are forbidden in module packages.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
@Conditional(ModuleEnabledCondition.class)
public @interface ModuleService {

    /** The module this bean belongs to. */
    String module();

    @AliasFor(annotation = Service.class, attribute = "value")
    String name() default "";
}
