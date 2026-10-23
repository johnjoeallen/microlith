package io.macroservice.platform.support.arch;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Each rule passes on the good fixtures and fails on the bad ones (negative tests). */
class ModuleArchitectureRulesTests {

    private static final String BASE = "io.macroservice.platform.support.arch.fixtures.";
    private static final String GOOD = BASE + "good";
    private static final String BAD = BASE + "bad";

    private final JavaClasses good = new ClassFileImporter().importPackages(GOOD);
    private final JavaClasses bad = new ClassFileImporter().importPackages(BAD);

    @Test
    void plainStereotypesAreRejected() {
        assertRule(ModuleArchitectureRules::noPlainSpringStereotypes, "PlainServiceBean", "PlainConfiguration");
    }

    @Test
    void foreignAndApiEventTypesAreRejected() {
        assertRule(ModuleArchitectureRules::noCrossModuleEventListeners,
                "ForeignEventListener", "takes event type", "ApiEventListener", "from an api package");
    }

    @Test
    void reachingIntoAnotherModulesInternalsIsRejected() {
        assertRule(ModuleArchitectureRules::crossModuleAccessOnlyThroughApi, "ReachesIntoInternals", "Hidden");
    }

    private void assertRule(Function<String, ArchRule> rule, String... expectedInViolations) {
        assertThat(rule.apply(GOOD).evaluate(good).hasViolation()).as("good fixtures").isFalse();
        var result = rule.apply(BAD).evaluate(bad);
        assertThat(result.hasViolation()).as("bad fixtures").isTrue();
        String details = String.join("\n", result.getFailureReport().getDetails());
        assertThat(details).contains(expectedInViolations);
    }
}
