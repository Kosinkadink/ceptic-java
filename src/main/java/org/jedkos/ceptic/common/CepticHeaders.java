package org.jedkos.ceptic.common;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class CepticHeaders {

    protected JSONObject headers = new JSONObject();

    // Header set/get
    public JSONArray getErrors() {
        return (JSONArray) headers.get(HeaderType.Errors);
    }

    public void setErrors(JSONArray errors) {
        headers.put(HeaderType.Errors, errors);
    }

    public Long getContentLength() {
        return (Long) headers.get(HeaderType.ContentLength);
    }

    public void setContentLength(long contentLength) {
        headers.put(HeaderType.ContentLength, contentLength);
    }

    public boolean hasContentLength() {
        Long contentLength = getContentLength();
        return contentLength != null && contentLength > 0;
    }

    public String getContentType() {
        return (String) headers.get(HeaderType.ContentType);
    }

    public void setContentType(String contentType) {
        headers.put(HeaderType.ContentType, contentType);
    }

    public boolean hasEncoding() {
        String encoding = getEncoding();
        return encoding != null && !encoding.isEmpty();
    }

    public String getEncoding() {
        return (String) headers.get(HeaderType.Encoding);
    }

    public void setEncoding(String encoding) {
        headers.put(HeaderType.Encoding, encoding);
    }

    public String getAuthorization() {
        return (String) headers.get(HeaderType.Authorization);
    }

    public void setAuthorization(String authorization) {
        headers.put(HeaderType.Authorization, authorization);
    }

    public boolean getExchange() {
        Boolean exchange = (Boolean) headers.get(HeaderType.Exchange);
        return exchange != null && exchange;
    }

    public void setExchange(boolean exchange) {
        headers.put(HeaderType.Exchange, exchange);
    }

    public JSONArray getFiles() {
        return (JSONArray) headers.get(HeaderType.Files);
    }

    public void setFiles(JSONArray files) {
        headers.put(HeaderType.Files, files);
    }

    public JSONObject getHeaders() {
        return headers;
    }

}
