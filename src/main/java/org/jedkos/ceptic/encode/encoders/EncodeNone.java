package org.jedkos.ceptic.encode.encoders;

import org.jedkos.ceptic.encode.EncodeObject;

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
