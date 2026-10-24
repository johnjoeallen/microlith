package io.macroservice.platform.rpcdocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ApiDocsTests {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private final ApiDocs docs = new ApiDocs(MAPPER, "1.2.3");

    public enum Color { RED, GREEN }

    @io.swagger.v3.oas.annotations.media.Schema(description = "A line of stock")
    public record Line(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Stock keeping unit") String sku,
            int quantity, Color color, List<String> tags, Map<String, Long> counts, Line next) {
    }

    public record LineV2(String sku) {
    }

    @RpcApi("sample")
    public interface SampleV1 {

        @Idempotent
        @Operation(summary = "Gets a line", description = "Longer text")
        Line get(@Parameter(description = "the key") String sku);

        void ping();

        List<Line> all(String owner, int limit);
    }

    @RpcApi("sample")
    public interface SampleV2 extends SampleV1 {

        long count();
    }

    @RpcApi("sample")
    public interface Overloaded {

        void a();

        void a(int x);
    }

    @RpcApi("sample")
    public interface NotARecord {

        Thread get();
    }

    @Test
    void openApiHasOneOperationPerMethodWithAliasPathAndOperationId() {
        JsonNode doc = docs.openApi("Sample", List.of(SampleV1.class));

        assertThat(doc.at("/openapi").asString()).isEqualTo("3.1.0");
        assertThat(doc.at("/info/version").asString()).isEqualTo("1.2.3");
        assertThat(names(doc.get("paths"))).containsExactly("/rpc/sample/all", "/rpc/sample/get", "/rpc/sample/ping");
        JsonNode get = doc.at("/paths/~1rpc~1sample~1get/post");
        assertThat(get.get("operationId").asString()).isEqualTo("sample.get");
        assertThat(get.get("summary").asString()).isEqualTo("Gets a line");
        assertThat(get.get("description").asString()).isEqualTo("Longer text");
        assertThat(get.get("x-idempotent").asBoolean()).isTrue();
        assertThat(get.at("/requestBody/content/application~1json/schema/properties/sku/description").asString()).isEqualTo("the key");
        assertThat(get.at("/requestBody/content/application~1json/schema/required").toString()).isEqualTo("[\"sku\"]");
        assertThat(get.at("/responses/200/content/application~1json/schema/$ref").asString()).isEqualTo("#/components/schemas/Line");
        assertThat(doc.at("/paths/~1rpc~1sample~1ping/post/requestBody").isMissingNode()).isTrue();
        assertThat(doc.at("/paths/~1rpc~1sample~1ping/post/responses/200/content/application~1json/schema/type").asString()).isEqualTo("null");
    }

    @Test
    void recordSchemasFollowTheRecordComponents() {
        JsonNode line = docs.openApi("Sample", List.of(SampleV1.class)).at("/components/schemas/Line");

        assertThat(line.get("description").asString()).isEqualTo("A line of stock");
        assertThat(names(line.get("properties"))).containsExactly("sku", "quantity", "color", "tags", "counts", "next");
        assertThat(line.at("/properties/sku/description").asString()).isEqualTo("Stock keeping unit");
        assertThat(line.at("/properties/quantity/format").asString()).isEqualTo("int32");
        assertThat(line.at("/properties/tags/items/type").asString()).isEqualTo("string");
        assertThat(line.at("/properties/counts/additionalProperties/format").asString()).isEqualTo("int64");
        assertThat(line.at("/properties/next/$ref").asString()).isEqualTo("#/components/schemas/Line");
        assertThat(line.at("/properties/color/$ref").asString()).isEqualTo("#/components/schemas/Color");
        assertThat(line.get("required")).hasSize(6);
        JsonNode color = docs.openApi("Sample", List.of(SampleV1.class)).at("/components/schemas/Color");
        assertThat(color.get("enum").toString()).isEqualTo("[\"RED\",\"GREEN\"]");
    }

    @Test
    void addingAMethodChangesBothDocuments() {
        assertThat(docs.openApi("Sample", List.of(SampleV2.class))).isNotEqualTo(docs.openApi("Sample", List.of(SampleV1.class)));
        assertThat(docs.openRpc("Sample", List.of(SampleV2.class))).isNotEqualTo(docs.openRpc("Sample", List.of(SampleV1.class)));
        assertThat(docs.openApi("Sample", List.of(SampleV2.class)).at("/paths/~1rpc~1sample~1count").isObject()).isTrue();
    }

    @Test
    void addingARecordFieldChangesBothDocuments() {
        JsonNode before = docs.openApi("Sample", List.of(SampleV1.class));
        JsonNode lineV2Schema = new SchemaRegistry2().schemaOfLineV2();
        assertThat(lineV2Schema.at("/properties").size()).isEqualTo(1);
        assertThat(before.at("/components/schemas/Line/properties").size()).isEqualTo(6);
        assertThat(before.at("/components/schemas/Line/properties")).isNotEqualTo(lineV2Schema.at("/properties"));
    }

    @Test
    void openRpcDescribesMethodsWithNamedParams() {
        JsonNode doc = docs.openRpc("Sample", List.of(SampleV1.class));

        assertThat(doc.get("openrpc").asString()).isEqualTo("1.3.2");
        JsonNode all = doc.at("/methods/0");
        assertThat(all.get("name").asString()).isEqualTo("sample.all");
        assertThat(all.get("paramStructure").asString()).isEqualTo("by-name");
        assertThat(all.at("/params/0/name").asString()).isEqualTo("owner");
        assertThat(all.at("/params/1/schema/format").asString()).isEqualTo("int32");
        assertThat(all.at("/result/schema/type").asString()).isEqualTo("array");
        assertThat(doc.at("/methods/1/name").asString()).isEqualTo("sample.get");
        assertThat(doc.at("/methods/1/summary").asString()).isEqualTo("Gets a line");
        assertThat(doc.at("/methods/1/params/0/description").asString()).isEqualTo("the key");
        assertThat(doc.at("/methods/2/result/schema/type").asString()).isEqualTo("null");
    }

    @Test
    void openApiDocumentIsValidAgainstTheOpenApi31Schema() {
        JsonNode doc = docs.openApi("Sample", List.of(SampleV1.class, SampleV2.class));
        assertThat(OpenApiValidator.validate(doc)).isEmpty();

        JsonNode broken = doc.deepCopy();
        ((tools.jackson.databind.node.ObjectNode) broken).remove("info");
        assertThat(OpenApiValidator.validate(broken)).isNotEmpty();
    }

    @Test
    void rejectsOverloadsAndNonRecordTypes() {
        assertThatThrownBy(() -> docs.openApi("x", List.of(Overloaded.class))).hasMessageContaining("overloaded");
        assertThatThrownBy(() -> docs.openApi("x", List.of(NotARecord.class))).hasMessageContaining("is not usable");
    }

    private static List<String> names(JsonNode object) {
        return object.propertyNames().stream().toList();
    }

    /** Builds the schema of {@link LineV2} through the public API by wrapping it in a throw-away interface. */
    private final class SchemaRegistry2 {
        JsonNode schemaOfLineV2() {
            return docs.openApi("x", List.of(Holder.class)).at("/components/schemas/LineV2");
        }
    }

    @RpcApi("holder")
    public interface Holder {
        LineV2 get();
    }
}
