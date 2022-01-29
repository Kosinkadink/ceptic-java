package org.jedkos.ceptic.common;

import org.jedkos.ceptic.common.exceptions.CepticRequestVerifyException;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.nio.charset.StandardCharsets;

public class CepticRequest extends CepticHeaders {

    private final String command;
    private String endpoint;
    private byte[] body;
    private String url = "";

    private StreamHandler stream;

    private String host;
    private int port = Constants.DEFAULT_PORT;

    public CepticRequest(String command, String url) {
        this.command = command;
        this.url = url;
    }

    public CepticRequest(String command, String url, byte[] body) {
        this(command, url);
        setBody(body);
    }

    protected CepticRequest(String command, String endpoint, JSONObject headers) {
        this.command = command;
        this.endpoint = endpoint;
        if (headers != null) {
            this.headers = headers;
        }
    }

    protected CepticRequest(String command, String endpoint, JSONObject headers, byte[] body, String url) {
        this.command = command;
        this.endpoint = endpoint;
        if (headers != null) {
            this.headers = headers;
        }
        setBody(body);
        this.url = url;
    }

    public String getCommand() {
        return command;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        if (body != null) {
            this.body = body;
            setContentLength(body.length);
        }
    }

    public boolean hasStream() {
        return stream != null;
    }

    public StreamHandler getStream() {
        return stream;
    }

    public void setStream(StreamHandler stream) {
        this.stream = stream;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    //region Verify
    public void verifyAndPrepare() throws CepticRequestVerifyException {
        // check that command isn't null or empty
        if (command == null || command.isEmpty()) {
            throw new CepticRequestVerifyException("command cannot be null or empty");
        }
        // check that url isn't null or empty
        if (url == null || url.isEmpty()) {
            throw new CepticRequestVerifyException("url cannot be null or empty");
        }
        // don't redo verification if already satisfied
        if (host != null && !host.isEmpty() && endpoint != null && !endpoint.isEmpty()) {
            return;
        }
        // extract request components from url
        String[] components = url.split("/",2);
        // set endpoint
        if (components.length < 2 || components[1].isEmpty()) {
            endpoint = "/";
        } else {
            endpoint = components[1];
        }
        // extract host and port from first component
        String[] elements = components[0].split(":",2);
        host = elements[0];
        if (elements.length > 1) {
            try {
                port = Integer.parseInt(elements[1]);
            } catch (NumberFormatException e) {
                throw new CepticRequestVerifyException("port must be an integer, not " + elements[1]);
            }
        }
    }
    //endregion

    //region Data
    public byte[] getData() {
        return String.format("%s\r\n%s\r\n%s", command, endpoint, headers.toJSONString()).getBytes(StandardCharsets.UTF_8);
    }

    public static CepticRequest fromData(String data) {
        String[] values = data.split("\\r\\n");
        String command = values[0];
        String endpoint = "";
        JSONObject headers = null;
        if (values.length > 1) {
            endpoint = values[1];
        }
        if (values.length > 2) {
            headers = (JSONObject) JSONValue.parse(values[2]);
        }
        return new CepticRequest(command, endpoint, headers);
    }

    public static CepticRequest fromData(byte[] data) {
        return fromData(new String(data, StandardCharsets.UTF_8));
    }
    //endregion

    public StreamHandler beginExchange() {
        CepticResponse response = new CepticResponse(CepticStatusCode.EXCHANGE_START);
        response.setExchange(true);
        if (stream != null && !stream.isStopped()) {
            try {
                if (!getExchange()) {
                    stream.sendResponse(new CepticResponse(CepticStatusCode.MISSING_EXCHANGE));
                    if (stream.getSettings().verbose)
                        System.out.println("Request did not have required Exchange header");
                    return null;
                }
                stream.sendResponse(response);
            } catch (StreamException e) {
                if (stream.getSettings().verbose)
                    System.out.printf("StreamException type %s raised while trying to beginExchange: %s\n",
                            e.getClass().toString(), e);
                return null;
            }
            return stream;
        }
        return null;
    }

}
