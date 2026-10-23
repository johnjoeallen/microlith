package io.macroservice.platform.support;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Matches when {@code modules.<name>.enabled} is true. The default is false. */
public class ModuleEnabledCondition implements Condition {

    private static final List<Class<? extends Annotation>> STEREOTYPES =
            List.of(ModuleService.class, ModuleComponent.class, ModuleConfiguration.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        for (Class<? extends Annotation> stereotype : STEREOTYPES) {
            Map<String, Object> attributes = metadata.getAnnotationAttributes(stereotype.getName());
            if (attributes != null) {
                return isEnabled(context.getEnvironment(), (String) attributes.get("module"));
            }
        }
        throw new IllegalStateException("@Conditional(ModuleEnabledCondition) used without a @Module* stereotype");
    }

    public static boolean isEnabled(Environment environment, String module) {
        return environment.getProperty("modules." + module + ".enabled", Boolean.class, false);
    }
}
