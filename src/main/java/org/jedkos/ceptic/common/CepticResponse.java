package org.jedkos.ceptic.common;

import org.jedkos.ceptic.stream.StreamHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.nio.charset.StandardCharsets;

public class CepticResponse extends CepticHeaders {

    private final CepticStatusCode statusCode;
    private byte[] body;

    private StreamHandler stream;


    public CepticResponse(CepticStatusCode statusCode, byte[] body, JSONObject headers, JSONArray errors, StreamHandler stream) {
        this.statusCode = statusCode;
        setBody(body);
        if (headers != null) {
            this.headers = headers;
        }
        if (errors != null) {
            setErrors(errors);
        }
        this.stream = stream;
    }

    public CepticResponse(CepticStatusCode statusCode, JSONObject headers) {
        this(statusCode, null, headers, null,null);
    }

    public CepticResponse(CepticStatusCode statusCode, JSONArray errors) {
        this(statusCode, null, null, errors, null);
    }

    public CepticResponse(CepticStatusCode statusCode, byte[] body, JSONObject headers) {
        this(statusCode, body, headers, null, null);
    }

    public CepticResponse(CepticStatusCode statusCode, byte[] body) {
        this(statusCode, body, null, null, null);
    }

    public CepticResponse(CepticStatusCode statusCode) {
        this(statusCode, null, null, null, null);
    }

    public StreamHandler getStream() {
        return stream;
    }

    public void setStream(StreamHandler stream) {
        this.stream = stream;
    }

    public CepticStatusCode getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        if (body == null) {
            return new byte[0];
        }
        return body;
    }

    public void setBody(byte[] body) {
        if (body != null) {
            this.body = body;
            setContentLength(body.length);
        }
    }

    //region Data
    public byte[] getData() {
        return String.format("%3s\r\n%s", statusCode.getValue(), headers.toJSONString()).getBytes(StandardCharsets.UTF_8);
    }

    public static CepticResponse fromData(String data) {
        String[] values = data.split("\\r\\n");
        CepticStatusCode statusCode = CepticStatusCode.fromValue(values[0]);
        if (values.length == 2) {
            JSONObject headers = (JSONObject) JSONValue.parse(values[1]);
            return new CepticResponse(statusCode, headers);
        } else {
            return new CepticResponse(statusCode, (JSONObject) null);
        }
    }

    public static CepticResponse fromData(byte[] data) {
        return CepticResponse.fromData(new String(data, StandardCharsets.UTF_8));
    }
    //endregion

}
