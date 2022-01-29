package org.jedkos.ceptic.stream;

import java.util.HashMap;
import java.util.Map;

public enum StreamFrameInfo {
    CONTINUE("0"),
    END("1");

    public final String value;
    private static final Map<String, StreamFrameInfo> BY_VALUE = new HashMap<>();

    static {
        for (StreamFrameInfo info: values()) {
            BY_VALUE.put(info.value, info);
        }
    }

    StreamFrameInfo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StreamFrameInfo fromValue(String value) {
        return BY_VALUE.getOrDefault(value, null);
    }

    public static String fromInfo(StreamFrameInfo type) {
        return type.value;
    }

}
