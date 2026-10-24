package io.macroservice.platform.rpcstarter;

import java.nio.ByteBuffer;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonType;
import org.bson.io.BasicOutputBuffer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Converts a JSON-RPC envelope (a JSON object tree) to BSON and back. Strings, integers, longs, doubles, booleans, null, objects and arrays. */
final class BsonJson {

    private BsonJson() {
    }

    static byte[] toBson(JsonNode root) {
        if (!root.isObject()) {
            throw new IllegalArgumentException("A BSON document must be a JSON object");
        }
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            writeObject(writer, root);
        }
        return buffer.toByteArray();
    }

    private static void writeObject(BsonBinaryWriter writer, JsonNode object) {
        writer.writeStartDocument();
        for (var field : object.properties()) {
            writer.writeName(field.getKey());
            writeValue(writer, field.getValue());
        }
        writer.writeEndDocument();
    }

    private static void writeValue(BsonBinaryWriter writer, JsonNode node) {
        if (node.isObject()) {
            writeObject(writer, node);
        } else if (node.isArray()) {
            writer.writeStartArray();
            for (int i = 0; i < node.size(); i++) {
                writeValue(writer, node.get(i));
            }
            writer.writeEndArray();
        } else if (node.isString()) {
            writer.writeString(node.stringValue());
        } else if (node.isBoolean()) {
            writer.writeBoolean(node.booleanValue());
        } else if (node.isNull()) {
            writer.writeNull();
        } else if (node.isInt() || node.isShort()) {
            writer.writeInt32(node.intValue());
        } else if (node.isLong()) {
            writer.writeInt64(node.longValue());
        } else if (node.isFloatingPointNumber()) {
            writer.writeDouble(node.doubleValue());
        } else if (node.isIntegralNumber()) {
            writer.writeString(node.bigIntegerValue().toString());      // beyond long: kept exact as text
        } else {
            throw new IllegalArgumentException("Cannot write " + node.getNodeType() + " as BSON");
        }
    }

    static JsonNode fromBson(byte[] bytes, JsonMapper mapper) {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bytes))) {
            ObjectNode root = mapper.createObjectNode();
            readObject(reader, root, mapper);
            return root;
        }
    }

    private static void readObject(BsonBinaryReader reader, ObjectNode into, JsonMapper mapper) {
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            switch (reader.getCurrentBsonType()) {
                case STRING -> into.put(name, reader.readString());
                case INT32 -> into.put(name, reader.readInt32());
                case INT64 -> into.put(name, reader.readInt64());
                case DOUBLE -> into.put(name, reader.readDouble());
                case BOOLEAN -> into.put(name, reader.readBoolean());
                case NULL -> {
                    reader.readNull();
                    into.putNull(name);
                }
                case DOCUMENT -> {
                    ObjectNode child = mapper.createObjectNode();
                    readObject(reader, child, mapper);
                    into.set(name, child);
                }
                case ARRAY -> {
                    ArrayNode child = mapper.createArrayNode();
                    readArray(reader, child, mapper);
                    into.set(name, child);
                }
                default -> throw new IllegalArgumentException("Unsupported BSON type " + reader.getCurrentBsonType() + " in field " + name);
            }
        }
        reader.readEndDocument();
    }

    private static void readArray(BsonBinaryReader reader, ArrayNode into, JsonMapper mapper) {
        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.getCurrentBsonType()) {
                case STRING -> into.add(reader.readString());
                case INT32 -> into.add(reader.readInt32());
                case INT64 -> into.add(reader.readInt64());
                case DOUBLE -> into.add(reader.readDouble());
                case BOOLEAN -> into.add(reader.readBoolean());
                case NULL -> {
                    reader.readNull();
                    into.addNull();
                }
                case DOCUMENT -> {
                    ObjectNode child = mapper.createObjectNode();
                    readObject(reader, child, mapper);
                    into.add(child);
                }
                case ARRAY -> {
                    ArrayNode child = mapper.createArrayNode();
                    readArray(reader, child, mapper);
                    into.add(child);
                }
                default -> throw new IllegalArgumentException("Unsupported BSON type " + reader.getCurrentBsonType() + " in an array");
            }
        }
        reader.readEndArray();
    }
}
