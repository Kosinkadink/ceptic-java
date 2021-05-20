package ceptic.common;

import java.util.HashMap;
import java.util.Map;

public class CepticStatusCode {
//    OK(200),
//    CREATED(201),
//    NO_CONTENT(204),
//    NOT_MODIFIED(304),
//    BAD_REQUEST(400),
//    UNAUTHORIZED(401),
//    FORBIDDEN(403),
//    NOT_FOUND(404),
//    CONFLICT(409),
//    INTERNAL_SERVER_ERROR(500),
//    LOCAL_ERROR(600);
    public static final CepticStatusCode OK = new CepticStatusCode(200);
    public static final CepticStatusCode CREATED = new CepticStatusCode(201);
    public static final CepticStatusCode NO_CONTENT = new CepticStatusCode(204);
    public static final CepticStatusCode NOT_MODIFIED = new CepticStatusCode(304);
    public static final CepticStatusCode BAD_REQUEST = new CepticStatusCode(400);
    public static final CepticStatusCode UNAUTHORIZED = new CepticStatusCode(401);
    public static final CepticStatusCode FORBIDDEN = new CepticStatusCode(403);
    public static final CepticStatusCode NOT_FOUND = new CepticStatusCode(404);
    public static final CepticStatusCode CONFLICT = new CepticStatusCode(409);
    public static final CepticStatusCode INTERNAL_SERVER_ERROR = new CepticStatusCode(500);
    public static final CepticStatusCode LOCAL_ERROR = new CepticStatusCode(600);

    private final int valueInt;
    private final String valueString;


    CepticStatusCode(int valueInt) {
        this.valueInt = valueInt;
        this.valueString = String.format("%3s", valueInt);
    }

    public CepticStatusCode create(int valueInt) {
        return new CepticStatusCode(valueInt);
    }

    public int getValueInt() {
        return valueInt;
    }

    public String getValueString() {
        return valueString;
    }

    public static CepticStatusCode fromValue(Integer value) {
        if (value >= 100 && value <= 999) {
            return new CepticStatusCode(value);
        }
        return null;
    }

    public static CepticStatusCode fromValue(String value) {
        try {
            int valueInt = Integer.parseInt(value);
            return fromValue(valueInt);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int getValueIntFromStatus(CepticStatusCode statusCode) {
        return statusCode.valueInt;
    }

    public static String getValueStringFromStatus(CepticStatusCode statusCode) {
        return statusCode.valueString;
    }

    public boolean isSuccess() {
        return 200 <= valueInt && valueInt <= 399;
    }

    public boolean isError() {
        return 400 <= valueInt && valueInt <= 499;
    }

    public boolean isClientError() {
        return 400 <= valueInt && valueInt <= 499;
    }

    public boolean isServerError() {
        return 500 <= valueInt && valueInt <= 599;
    }

    public boolean isLocalError() {
        return 600 <= valueInt && valueInt <= 699;
    }

}
