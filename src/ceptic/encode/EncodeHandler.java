package ceptic.encode;

import java.util.List;
import java.util.ListIterator;

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
        // decode in reverse order
        for(int i = encoderList.size()-1; i >= 0; i--) {
            data = encoderList.get(i).decode(data);
        }
        return data;
    }

}
