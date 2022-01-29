package org.jedkos.ceptic.common;

public class CepticStatusCode {
    public static final CepticStatusCode OK = new CepticStatusCode(200);
    public static final CepticStatusCode CREATED = new CepticStatusCode(201);
    public static final CepticStatusCode NO_CONTENT = new CepticStatusCode(204);
    public static final CepticStatusCode EXCHANGE_START = new CepticStatusCode(250);
    public static final CepticStatusCode EXCHANGE_END = new CepticStatusCode(251);
    public static final CepticStatusCode NOT_MODIFIED = new CepticStatusCode(304);
    public static final CepticStatusCode BAD_REQUEST = new CepticStatusCode(400);
    public static final CepticStatusCode UNAUTHORIZED = new CepticStatusCode(401);
    public static final CepticStatusCode FORBIDDEN = new CepticStatusCode(403);
    public static final CepticStatusCode NOT_FOUND = new CepticStatusCode(404);
    public static final CepticStatusCode CONFLICT = new CepticStatusCode(409);
    public static final CepticStatusCode UNEXPECTED_END = new CepticStatusCode(460);
    public static final CepticStatusCode MISSING_EXCHANGE = new CepticStatusCode(461);
    public static final CepticStatusCode INTERNAL_SERVER_ERROR = new CepticStatusCode(500);

    private final int value;


    CepticStatusCode(int value) {
        this.value = value;
    }

    public CepticStatusCode create(int valueInt) {
        return new CepticStatusCode(valueInt);
    }

    public int getValue() {
        return value;
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

    @Override
    public boolean equals(Object o) {
        // true if refers to this object
        if (this == o) {
            return true;
        }
        // false is object is null or not of same class
        if (o == null || getClass() != o.getClass())
            return false;
        CepticStatusCode fromO = (CepticStatusCode)o;
        // true if status codes match
        return this.value == fromO.value;
    }

    public boolean isSuccess() {
        return 200 <= value && value <= 399;
    }

    public boolean isError() {
        return 400 <= value && value <= 599;
    }

    public boolean isClientError() {
        return 400 <= value && value <= 499;
    }

    public boolean isServerError() {
        return 500 <= value && value <= 599;
    }

}
