package org.jedkos.ceptic.encode;

public interface EncodeObject {

    byte[] encode(byte[] data);
    byte[] decode(byte[] data);

}
