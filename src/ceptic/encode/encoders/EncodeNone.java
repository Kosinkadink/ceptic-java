package ceptic.encode.encoders;

import ceptic.encode.EncodeObject;

public class EncodeNone implements EncodeObject {

    @Override
    public byte[] encode(byte[] data) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data) {
        return data;
    }
}
