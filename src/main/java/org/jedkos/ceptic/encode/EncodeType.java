package org.jedkos.ceptic.encode;

import org.jedkos.ceptic.encode.encoders.EncodeBase64;
import org.jedkos.ceptic.encode.encoders.EncodeGzip;
import org.jedkos.ceptic.encode.encoders.EncodeNone;

import java.util.HashMap;
import java.util.Map;

public enum EncodeType {
    none("none", new EncodeNone()),
    base64("base64", new EncodeBase64()),
    gzip("gzip", new EncodeGzip());

    private final String value;
    private final EncodeObject encodeObject;

    private static final Map<String, EncodeType> BY_STRING = new HashMap<>();

    static {
        for (EncodeType type : values()) {
            BY_STRING.put(type.value, type);
        }
    }

    EncodeType(String value, EncodeObject encodeObject) {
        this.value = value;
        this.encodeObject = encodeObject;
    }

    public String getValue() {
        return value;
    }

    public EncodeObject getEncoder() {
        return encodeObject;
    }

    public static EncodeType fromValue(String value) {
        return BY_STRING.getOrDefault(value, null);
    }

}
