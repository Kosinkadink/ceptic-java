package ceptic.encode.encoders;

import ceptic.encode.EncodeObject;

import java.util.Base64;

public class EncodeBase64 extends EncodeObject {

    @Override
    public byte[] encode(byte[] data) {
        return Base64.getEncoder().encode(data);
    }

    @Override
    public byte[] decode(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

}
