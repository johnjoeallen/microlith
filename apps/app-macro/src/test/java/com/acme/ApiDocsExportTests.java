package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import io.macroservice.platform.rpcdocs.ApiDocs;
import io.macroservice.platform.rpcdocs.OpenApiValidator;
import io.macroservice.platform.support.RpcApiScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the generated documents of every {@code @RpcApi} on the classpath (the compiled -api artifacts) to
 * {@code target/api-docs/} so that CI can publish them. Nothing generated is committed; {@code target/} is ignored.
 * {@code mvn -pl apps/app-macro -am verify} produces them.
 */
class ApiDocsExportTests {

    @Test
    void writesTheDocumentsOfAllModulesToTarget() throws IOException {
        JsonMapper mapper = JsonMapper.builder().build();
        ApiDocs docs = new ApiDocs(mapper, "0.1.0-SNAPSHOT");
        List<Class<?>> apis = List.copyOf(RpcApiScanner.scan(List.of("com.acme"), getClass().getClassLoader()));
        assertThat(apis).hasSize(TestApps.MODULES.size());

        Path out = Path.of("target", "api-docs");
        write(mapper, out.resolve("openapi.json"), docs.openApi("Macro service API", apis));
        write(mapper, out.resolve("openrpc.json"), docs.openRpc("Macro service API", apis));
        for (Class<?> api : apis) {
            String module = ApiDocs.moduleOf(api);
            write(mapper, out.resolve(module).resolve("openapi.json"), docs.openApi(module + " API", List.of(api)));
            write(mapper, out.resolve(module).resolve("openrpc.json"), docs.openRpc(module + " API", List.of(api)));
        }

        assertThat(OpenApiValidator.validate(mapper.readTree(Files.readString(out.resolve("openapi.json"))))).isEmpty();
        assertThat(out.resolve("inventory").resolve("openrpc.json")).exists();
    }

    private static void write(JsonMapper mapper, Path file, Object document) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document));
    }
}
