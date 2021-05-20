package ceptic.stream;

import ceptic.common.CepticResponse;

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

    public CepticResponse getResponse() {
        return response;
    }

    public byte[] getData() {
        return data;
    }

}
