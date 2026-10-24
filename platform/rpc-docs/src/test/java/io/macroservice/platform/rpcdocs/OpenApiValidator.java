package io.macroservice.platform.rpcdocs;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Validates a document against the official OpenAPI 3.1 JSON Schema (published in this module's test-jar). */
public final class OpenApiValidator {

    private static final Schema SCHEMA = load();

    private OpenApiValidator() {
    }

    /** The validation messages; empty when the document is a valid OpenAPI 3.1 document. */
    public static List<String> validate(JsonNode document) {
        return SCHEMA.validate(document).stream().map(Object::toString).toList();
    }

    private static Schema load() {
        try (InputStream in = OpenApiValidator.class.getResourceAsStream("/oas-3.1-schema.json")) {
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
