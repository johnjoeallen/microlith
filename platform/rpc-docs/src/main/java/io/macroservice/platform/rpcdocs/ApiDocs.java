package io.macroservice.platform.rpcdocs;

import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.RpcErrorCodes;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generates an OpenAPI 3.1 document and an OpenRPC document from {@link RpcApi} interfaces. Nothing is stored: the
 * documents are a function of the interfaces, so they cannot drift from the code.
 */
public final class ApiDocs {

    static final String OPENAPI_VERSION = "3.1.0";
    static final String OPENRPC_VERSION = "1.3.2";

    private final JsonMapper mapper;
    private final String version;

    public ApiDocs(JsonMapper mapper, String version) {
        this.mapper = mapper;
        this.version = version;
    }

    /** One operation per method at {@code POST /rpc/{module}/{method}} with {@code operationId = module.method}. */
    public ObjectNode openApi(String title, Collection<Class<?>> apis) {
        SchemaRegistry registry = new SchemaRegistry(mapper, "#/components/schemas/");
        ObjectNode root = mapper.createObjectNode();
        root.put("openapi", OPENAPI_VERSION);
        ObjectNode info = root.putObject("info");
        info.put("title", title);
        info.put("version", version);
        info.put("description", "Convenience alias of the JSON-RPC 2.0 API: the body is the bare params object, the response "
                + "is the bare result. The protocol itself is the JSON-RPC envelope on POST /rpc/{module}.");
        ArrayNode tags = root.putArray("tags");
        ObjectNode paths = root.putObject("paths");
        for (Class<?> api : sorted(apis)) {
            String module = moduleOf(api);
            ObjectNode tag = tags.addObject().put("name", module);
            String moduleDescription = Annotations.text(api, Annotations.SCHEMA, "description");
            if (moduleDescription != null) {
                tag.put("description", moduleDescription);
            }
            methodsOf(api).forEach((name, method) -> {
                ObjectNode operation = paths.putObject("/rpc/" + module + "/" + name).putObject("post");
                operation.putArray("tags").add(module);
                operation.put("operationId", module + "." + name);
                describe(operation, method);
                if (method.isAnnotationPresent(Idempotent.class)) {
                    operation.put("x-idempotent", true);
                }
                if (method.getParameterCount() > 0) {
                    ObjectNode body = operation.putObject("requestBody");
                    body.put("required", true);
                    body.putObject("content").putObject("application/json").set("schema", paramsSchema(registry, method));
                }
                ObjectNode responses = operation.putObject("responses");
                ObjectNode ok = responses.putObject("200");
                ok.put("description", "The result");
                ok.putObject("content").putObject("application/json").set("schema", resultSchema(registry, method));
                failure(responses, "400", "Parse error, invalid request or invalid params");
                failure(responses, "404", "Method not found");
                failure(responses, "422", "The method failed with a declared error; code and message say which");
                failure(responses, "503", "The module is unavailable or overloaded");
                failure(responses, "500", "Internal error");
            });
        }
        ObjectNode schemas = root.putObject("components").putObject("schemas");
        registry.schemas().forEach(schemas::set);
        schemas.set("RpcError", errorSchema());
        return root;
    }

    /** The native description of the JSON-RPC methods; params are by name, as the server expects. */
    public ObjectNode openRpc(String title, Collection<Class<?>> apis) {
        SchemaRegistry registry = new SchemaRegistry(mapper, "#/components/schemas/");
        ObjectNode root = mapper.createObjectNode();
        root.put("openrpc", OPENRPC_VERSION);
        ObjectNode info = root.putObject("info");
        info.put("title", title);
        info.put("version", version);
        ArrayNode methods = root.putArray("methods");
        for (Class<?> api : sorted(apis)) {
            String module = moduleOf(api);
            methodsOf(api).forEach((name, method) -> {
                ObjectNode entry = methods.addObject();
                entry.put("name", module + "." + name);
                describe(entry, method);
                entry.putArray("tags").addObject().put("name", module);
                entry.put("paramStructure", "by-name");
                ArrayNode params = entry.putArray("params");
                for (Parameter parameter : method.getParameters()) {
                    ObjectNode param = params.addObject();
                    param.put("name", parameter.getName());
                    param.put("required", true);
                    String description = Annotations.text(parameter, Annotations.PARAMETER, "description");
                    if (description != null) {
                        param.put("description", description);
                    }
                    param.set("schema", registry.schemaOf(parameter.getParameterizedType()));
                }
                entry.putObject("result").put("name", "result").set("schema", resultSchema(registry, method));
                ArrayNode errors = entry.putArray("errors");
                errors.addObject().put("code", RpcErrorCodes.INVALID_PARAMS).put("message", "Invalid params");
                errors.addObject().put("code", RpcErrorCodes.UNAVAILABLE).put("message", "Module unavailable");
                errors.addObject().put("code", RpcErrorCodes.OVERLOADED).put("message", "Module overloaded");
                errors.addObject().put("code", RpcErrorCodes.INTERNAL_ERROR).put("message", "Internal error");
                if (method.isAnnotationPresent(Idempotent.class)) {
                    entry.put("x-idempotent", true);
                }
            });
        }
        ObjectNode schemas = root.putObject("components").putObject("schemas");
        registry.schemas().forEach(schemas::set);
        schemas.set("RpcError", errorSchema());
        return root;
    }

    /** The module name of an {@link RpcApi} interface. */
    public static String moduleOf(Class<?> api) {
        RpcApi annotation = api.getAnnotation(RpcApi.class);
        if (annotation == null) {
            throw new IllegalArgumentException(api.getName() + " is not annotated with @RpcApi");
        }
        return annotation.value();
    }

    private ObjectNode paramsSchema(SchemaRegistry registry, Method method) {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        for (Parameter parameter : method.getParameters()) {
            ObjectNode property = registry.schemaOf(parameter.getParameterizedType());
            String description = Annotations.text(parameter, Annotations.PARAMETER, "description");
            if (description != null) {
                if (property.has("$ref")) {
                    property = mapper.createObjectNode().put("description", description);
                    property.putArray("allOf").add(registry.schemaOf(parameter.getParameterizedType()));
                } else {
                    property.put("description", description);
                }
            }
            properties.set(parameter.getName(), property);
            required.add(parameter.getName());
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    private ObjectNode resultSchema(SchemaRegistry registry, Method method) {
        return method.getReturnType() == void.class
                ? mapper.createObjectNode().put("type", "null")
                : registry.schemaOf(method.getGenericReturnType());
    }

    private void failure(ObjectNode responses, String status, String description) {
        ObjectNode response = responses.putObject(status);
        response.put("description", description);
        response.putObject("content").putObject("application/json").putObject("schema")
                .put("$ref", "#/components/schemas/RpcError");
    }

    private ObjectNode errorSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("code").put("type", "integer").put("description", "JSON-RPC error code; -32768..-32000 are reserved");
        properties.putObject("message").put("type", "string");
        properties.putObject("data").put("description", "Optional structured details");
        schema.putArray("required").add("code").add("message");
        return schema;
    }

    private void describe(ObjectNode target, Method method) {
        String summary = Annotations.text(method, Annotations.OPERATION, "summary");
        String description = Annotations.text(method, Annotations.OPERATION, "description");
        if (summary != null) {
            target.put("summary", summary);
        }
        if (description != null) {
            target.put("description", description);
        }
    }

    private static List<Class<?>> sorted(Collection<Class<?>> apis) {
        return apis.stream().sorted(Comparator.comparing(ApiDocs::moduleOf)).toList();
    }

    /** Public instance methods by name; an overloaded name is rejected, as the dispatcher rejects it. */
    private static Map<String, Method> methodsOf(Class<?> api) {
        Map<String, Method> methods = new LinkedHashMap<>();
        Arrays.stream(api.getMethods()).filter(m -> !Modifier.isStatic(m.getModifiers()))
                .sorted(Comparator.comparing(Method::getName)).forEach(method -> {
                    if (methods.put(method.getName(), method) != null) {
                        throw new IllegalStateException("@RpcApi interface %s has an overloaded method '%s'"
                                .formatted(api.getName(), method.getName()));
                    }
                    for (Parameter parameter : method.getParameters()) {
                        if (!parameter.isNamePresent()) {
                            throw new IllegalStateException("Parameter names of %s.%s are not available; compile with -parameters"
                                    .formatted(api.getName(), method.getName()));
                        }
                    }
                });
        return methods;
    }
}
