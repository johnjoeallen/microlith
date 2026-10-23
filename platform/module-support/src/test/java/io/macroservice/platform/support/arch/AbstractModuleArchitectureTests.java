package io.macroservice.platform.support.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Extend this in every app. Override {@link #rootPackage()} if the application root is not {@code com.acme}. */
public abstract class AbstractModuleArchitectureTests {

    protected String rootPackage() {
        return "com.acme";
    }

    @Test
    void modulesUseModuleStereotypes() {
        ModuleArchitectureRules.noPlainSpringStereotypes(rootPackage()).check(classes());
    }

    @Test
    void eventListenersDoNotCrossModules() {
        ModuleArchitectureRules.noCrossModuleEventListeners(rootPackage()).check(classes());
    }

    @Test
    void modulesTalkToEachOtherOnlyThroughApi() {
        ModuleArchitectureRules.crossModuleAccessOnlyThroughApi(rootPackage()).check(classes());
    }

    private JavaClasses classes() {
        return new ClassFileImporter().importPackages(rootPackage());
    }
}
