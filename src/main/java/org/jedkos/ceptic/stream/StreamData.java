package org.jedkos.ceptic.stream;

import org.jedkos.ceptic.common.CepticResponse;

public class StreamData {

    private CepticResponse response;
    private byte[] data;

    public StreamData(CepticResponse response) {
        this.response = response;
    }

    public StreamData(byte[] data) {
        this.data = data;
    }

    public boolean isResponse() {
        return response != null;
    }

    public boolean isData() {
        return data != null;
    }

    public boolean isEmpty() {
        return !isData() && !isResponse();
    }

    public CepticResponse getResponse() {
        return response;
    }

    public byte[] getData() {
        return data;
    }

}
