package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the module structure and writes the module canvases and component diagrams (PlantUML) to
 * {@code docs/generated/}. They are generated on every build and never committed (the folder is git-ignored).
 */
class DocumentationTests {

    private static final Path OUTPUT = Path.of("..", "..", "docs", "generated").toAbsolutePath().normalize();

    @Test
    void verifiesTheModulesAndWritesCanvasesAndDiagrams() throws IOException {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify();

        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(OUTPUT.toString())).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml().writeModuleCanvases();

        try (Stream<Path> files = Files.list(OUTPUT)) {
            assertThat(files.map(f -> f.getFileName().toString()).toList())
                    .contains("components.puml", "module-orders.adoc", "module-inventory.adoc", "module-orders.puml");
        }
    }
}
