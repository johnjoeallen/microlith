package com.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * The module graph of section 16.1, exactly: a new dependency (declared or not), a missing one, or a cycle fails the
 * build. Modulith's verify() rejects cycles and undeclared dependencies; the exact comparison also catches a
 * dependency that was declared in allowedDependencies but is not in the design.
 */
class DependencyGraphTests {

    private static final Map<String, Set<String>> DESIGN = Map.of(
            "orders", Set.of("customers", "pricing", "shipping", "inventory", "payments", "notifications"),
            "pricing", Set.of("catalog", "customers"),
            "inventory", Set.of("catalog"),
            "shipping", Set.of("inventory", "customers"),
            "payments", Set.of("customers"),
            "notifications", Set.of("customers"),
            "catalog", Set.of(),
            "customers", Set.of());

    @Test
    void theGraphMatchesTheDesignExactlyAndIsValid() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();
        for (ApplicationModule module : modules) {
            Set<String> actual = module.getDirectDependencies(modules).uniqueModules()
                    .map(m -> m.getIdentifier().toString()).collect(Collectors.toCollection(TreeSet::new));
            assertThat(actual).as("dependencies of %s", module.getIdentifier()).isEqualTo(new TreeSet<>(DESIGN.get(module.getIdentifier().toString())));
        }
    }

    @Test
    void aCycleBetweenModulesFailsVerification() {
        assertThatThrownBy(() -> ApplicationModules.of(io.macroservice.demo.fixtures.cycle.CycleApplication.class).verify())
                .hasMessageContaining("Cycle").hasMessageContaining("a").hasMessageContaining("b");
    }

    @Test
    void anUndeclaredDependencyFailsVerification() {
        assertThatThrownBy(() -> ApplicationModules.of(io.macroservice.demo.fixtures.undeclared.UndeclaredApplication.class).verify())
                .hasMessageContaining("Module 'x' depends on module 'y'");
    }
}
