package org.jedkos.ceptic.stream;

import java.util.HashMap;
import java.util.Map;

public enum StreamFrameType {
    DATA("0"),
    HEADER("1"),
    RESPONSE("2"),
    KEEP_ALIVE("3"),
    CLOSE("4"),
    CLOSE_ALL("5");

    public final String value;
    private static final Map<String, StreamFrameType> BY_VALUE = new HashMap<>();

    static {
        for (StreamFrameType type: values()) {
            BY_VALUE.put(type.value, type);
        }
    }

    StreamFrameType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StreamFrameType fromValue(String value) {
        return BY_VALUE.getOrDefault(value, null);
    }

    public static String fromType(StreamFrameType type) {
        return type.value;
    }

}
