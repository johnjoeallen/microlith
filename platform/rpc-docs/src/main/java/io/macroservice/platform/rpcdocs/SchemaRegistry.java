package io.macroservice.platform.rpcdocs;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds JSON Schema (2020-12, as used by OpenAPI 3.1 and OpenRPC) for the types of an API. Records become named
 * schemas under {@code components/schemas} and are referenced; everything else is inlined. Records are described the
 * way Jackson writes them: every component is present, named after the component. A component is nullable only if
 * annotated with an annotation called {@code Nullable}.
 */
final class SchemaRegistry {

    private final JsonMapper mapper;
    private final String refPrefix;
    private final Map<String, ObjectNode> schemas = new TreeMap<>();
    private final Map<String, Class<?>> owners = new HashMap<>();

    SchemaRegistry(JsonMapper mapper, String refPrefix) {
        this.mapper = mapper;
        this.refPrefix = refPrefix;
    }

    Map<String, ObjectNode> schemas() {
        return schemas;
    }

    ObjectNode schemaOf(Type type) {
        ObjectNode node = mapper.createObjectNode();
        if (type instanceof Class<?> c) {
            return ofClass(c, node);
        }
        if (type instanceof ParameterizedType p) {
            Class<?> raw = (Class<?>) p.getRawType();
            Type[] args = p.getActualTypeArguments();
            if (Collection.class.isAssignableFrom(raw)) {
                node.put("type", "array");
                node.set("items", schemaOf(args[0]));
                if (java.util.Set.class.isAssignableFrom(raw)) {
                    node.put("uniqueItems", true);
                }
                return node;
            }
            if (Map.class.isAssignableFrom(raw)) {
                if (args[0] != String.class) {
                    throw new IllegalArgumentException("Map keys must be String in an @RpcApi: " + type);
                }
                node.put("type", "object");
                node.set("additionalProperties", schemaOf(args[1]));
                return node;
            }
            throw new IllegalArgumentException("Generic type is not supported in an @RpcApi: " + type);
        }
        if (type instanceof GenericArrayType g) {
            node.put("type", "array");
            node.set("items", schemaOf(g.getGenericComponentType()));
            return node;
        }
        throw new IllegalArgumentException("Unsupported type in an @RpcApi: " + type);
    }

    private ObjectNode ofClass(Class<?> c, ObjectNode node) {
        if (c == String.class || c == CharSequence.class || c == char.class || c == Character.class) {
            return node.put("type", "string");
        }
        if (c == boolean.class || c == Boolean.class) {
            return node.put("type", "boolean");
        }
        if (c == int.class || c == Integer.class || c == short.class || c == Short.class || c == byte.class || c == Byte.class) {
            return node.put("type", "integer").put("format", "int32");
        }
        if (c == long.class || c == Long.class) {
            return node.put("type", "integer").put("format", "int64");
        }
        if (c == BigInteger.class) {
            return node.put("type", "integer");
        }
        if (c == float.class || c == Float.class) {
            return node.put("type", "number").put("format", "float");
        }
        if (c == double.class || c == Double.class) {
            return node.put("type", "number").put("format", "double");
        }
        if (c == BigDecimal.class) {
            return node.put("type", "number");
        }
        if (c == Instant.class || c == OffsetDateTime.class || c == ZonedDateTime.class || c == LocalDateTime.class) {
            return node.put("type", "string").put("format", "date-time");
        }
        if (c == LocalDate.class) {
            return node.put("type", "string").put("format", "date");
        }
        if (c == LocalTime.class) {
            return node.put("type", "string").put("format", "time");
        }
        if (c == Duration.class) {
            return node.put("type", "string").put("format", "duration");
        }
        if (c == UUID.class) {
            return node.put("type", "string").put("format", "uuid");
        }
        if (c == URI.class) {
            return node.put("type", "string").put("format", "uri");
        }
        if (c == byte[].class) {
            return node.put("type", "string").put("format", "byte");
        }
        if (c == Object.class || c == tools.jackson.databind.JsonNode.class) {
            return node;
        }
        if (c.isArray()) {
            node.put("type", "array");
            node.set("items", schemaOf(c.getComponentType()));
            return node;
        }
        if (Collection.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("Raw collection type is not supported in an @RpcApi: " + c.getName());
        }
        if (c.isEnum()) {
            ObjectNode named = named(c);
            ObjectNode schema = schemas.get(nameOf(c));
            if (schema.isEmpty()) {
                schema.put("type", "string");
                ArrayNode values = schema.putArray("enum");
                for (Object constant : c.getEnumConstants()) {
                    values.add(((Enum<?>) constant).name());
                }
                describe(schema, c);
            }
            return named;
        }
        if (c.isRecord()) {
            ObjectNode named = named(c);
            ObjectNode schema = schemas.get(nameOf(c));
            if (schema.isEmpty()) {
                fillRecord(c, schema);
            }
            return named;
        }
        throw new IllegalArgumentException("%s is not usable in an @RpcApi: use a record, an enum, a scalar, a List or a Map"
                .formatted(c.getName()));
    }

    /** Reserves the schema name (so recursive records terminate) and returns a $ref node. */
    private ObjectNode named(Class<?> c) {
        String name = nameOf(c);
        schemas.computeIfAbsent(name, n -> mapper.createObjectNode());
        return mapper.createObjectNode().put("$ref", refPrefix + name);
    }

    private String nameOf(Class<?> c) {
        String simple = c.getSimpleName();
        Class<?> owner = owners.putIfAbsent(simple, c);
        if (owner == null || owner == c) {
            return simple;
        }
        String qualified = c.getName();
        owners.putIfAbsent(qualified, c);
        return qualified;
    }

    private void fillRecord(Class<?> c, ObjectNode schema) {
        schema.put("type", "object");
        describe(schema, c);
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = mapper.createArrayNode();
        for (RecordComponent component : c.getRecordComponents()) {
            Field field = fieldOf(c, component.getName());
            Method accessor = component.getAccessor();
            ObjectNode property = schemaOf(component.getGenericType());
            boolean nullable = Annotations.nullable(component, field, accessor);
            if (nullable) {
                property = nullable(property);
            }
            String description = firstNonNull(Annotations.text(component, Annotations.SCHEMA, "description"),
                    Annotations.text(field, Annotations.SCHEMA, "description"),
                    Annotations.text(accessor, Annotations.SCHEMA, "description"));
            if (description != null) {
                property = withDescription(property, description);
            }
            properties.set(component.getName(), property);
            required.add(component.getName());
        }
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
    }

    private ObjectNode nullable(ObjectNode property) {
        ObjectNode wrapper = mapper.createObjectNode();
        ArrayNode anyOf = wrapper.putArray("anyOf");
        anyOf.add(property);
        anyOf.addObject().put("type", "null");
        return wrapper;
    }

    /** A $ref cannot carry siblings portably in every consumer, so a described reference is wrapped in allOf. */
    private ObjectNode withDescription(ObjectNode property, String description) {
        if (property.has("$ref")) {
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.putArray("allOf").add(property);
            wrapper.put("description", description);
            return wrapper;
        }
        property.put("description", description);
        return property;
    }

    private void describe(ObjectNode schema, Class<?> c) {
        String description = Annotations.text(c, Annotations.SCHEMA, "description");
        if (description != null) {
            schema.put("description", description);
        }
    }

    private static Field fieldOf(Class<?> c, String name) {
        try {
            return c.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
