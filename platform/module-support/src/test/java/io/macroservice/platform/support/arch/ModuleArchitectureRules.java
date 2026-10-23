package io.macroservice.platform.support.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/**
 * Architecture rules for a root package whose direct sub-packages are modules ({@code <root>.<module>...}).
 * Published in the module-support test-jar; every app runs them through {@link AbstractModuleArchitectureTests}.
 */
public final class ModuleArchitectureRules {

    private static final List<String> LISTENER_ANNOTATIONS = List.of(
            "org.springframework.modulith.events.ApplicationModuleListener",
            "org.springframework.context.event.EventListener",
            "org.springframework.transaction.event.TransactionalEventListener");

    private ModuleArchitectureRules() {
    }

    /** Module beans must use the @Module* stereotypes, which carry the enablement condition. */
    public static ArchRule noPlainSpringStereotypes(String root) {
        return noClasses().that(inAModule(root)).should().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
                .as("classes in module packages must use @ModuleService, @ModuleComponent or @ModuleConfiguration "
                        + "instead of plain @Service, @Component or @Configuration")
                .allowEmptyShould(true);
    }

    /** Events never cross a module boundary, and event types are not part of any api package. */
    public static ArchRule noCrossModuleEventListeners(String root) {
        DescribedPredicate<JavaMethod> listener = new DescribedPredicate<>("event listener methods in modules") {
            @Override
            public boolean test(JavaMethod method) {
                return moduleOf(method.getOwner(), root) != null
                        && LISTENER_ANNOTATIONS.stream().anyMatch(method::isAnnotatedWith);
            }
        };
        return methods().that(listener).should(new ArchCondition<JavaMethod>(
                "take no parameter type from another module and none from an api package") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String own = moduleOf(method.getOwner(), root);
                for (JavaClass type : method.getRawParameterTypes()) {
                    String module = moduleOf(type.getBaseComponentType(), root);
                    if (module != null && isApi(type.getBaseComponentType(), root, module)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName()
                                + " takes event type " + type.getName() + " from an api package"));
                    } else if (module != null && !module.equals(own)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName()
                                + " takes event type " + type.getName() + " of module '" + module + "'"));
                    }
                }
            }
        }).as("event listener methods may not take parameter types from another module or from an api package")
                .allowEmptyShould(true);
    }

    /** The only thing a module may use of another module is its api package. */
    public static ArchRule crossModuleAccessOnlyThroughApi(String root) {
        return classes().that(inAModule(root)).should(new ArchCondition<JavaClass>(
                "depend on other modules only through their api package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String own = moduleOf(origin, root);
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    String module = moduleOf(target, root);
                    if (module != null && !module.equals(own) && !isApi(target, root, module)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        }).as("modules may use another module only through the types in its api package")
                .allowEmptyShould(true);
    }

    static DescribedPredicate<JavaClass> inAModule(String root) {
        return new DescribedPredicate<>("reside in a module of " + root) {
            @Override
            public boolean test(JavaClass type) {
                return moduleOf(type, root) != null;
            }
        };
    }

    /** The module a class belongs to, or null when it is outside {@code root} or directly in it. */
    static String moduleOf(JavaClass type, String root) {
        String pkg = type.getPackageName();
        if (!pkg.startsWith(root + ".")) {
            return null;
        }
        String rest = pkg.substring(root.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    static boolean isApi(JavaClass type, String root, String module) {
        String api = root + "." + module + ".api";
        return type.getPackageName().equals(api) || type.getPackageName().startsWith(api + ".");
    }
}
