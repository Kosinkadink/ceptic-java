package ceptic.encode;

import java.util.List;

public class EncodeHandler {

    private final List<EncodeObject> encoderList;

    public EncodeHandler(List<EncodeObject> encoderList) {
        this.encoderList = encoderList;
    }

    public byte[] encode(byte[] data) {
        for(EncodeObject encoder : encoderList) {
            data = encoder.encode(data);
        }
        return data;
    }

    public byte[] decode(byte[] data) {
        for(EncodeObject encoder : encoderList) {
            data = encoder.decode(data);
        }
        return data;
    }

}
