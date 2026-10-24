package io.macroservice.platform.rpcstarter;

/**
 * How the JSON-RPC envelope is put on an HTTP request or response: plain JSON (the default), gzip-compressed JSON, BSON, or gzip-compressed BSON.
 * Only the HTTP transport uses it; in-process calls and the in-memory codec binding never do. The server accepts every format whatever it sends.
 */
public enum WireFormat {

    JSON("json", false, false),
    GZIP("gzip", true, false),
    BSON("bson", false, true),
    BSON_GZIP("bson-gzip", true, true);

    public static final String JSON_TYPE = "application/json";
    public static final String BSON_TYPE = "application/bson";

    private final String label;
    private final boolean gzip;
    private final boolean bson;

    WireFormat(String label, boolean gzip, boolean bson) {
        this.label = label;
        this.gzip = gzip;
        this.bson = bson;
    }

    public boolean gzip() {
        return gzip;
    }

    public boolean bson() {
        return bson;
    }

    public String mediaType() {
        return bson ? BSON_TYPE : JSON_TYPE;
    }

    public String label() {
        return label;
    }

    /** {@code json}, {@code gzip}, {@code bson} or {@code bson-gzip} (also accepts {@code bson+gzip}); blank means json. */
    public static WireFormat parse(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        String normalized = value.trim().toLowerCase().replace('+', '-').replace('_', '-');
        for (WireFormat format : values()) {
            if (format.label.equals(normalized)) {
                return format;
            }
        }
        throw new IllegalArgumentException("rpc.client.wire must be json, gzip, bson or bson-gzip but was '%s'".formatted(value));
    }
}
