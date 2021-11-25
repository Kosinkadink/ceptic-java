package ceptic.encode.encoders;

import ceptic.encode.EncodeObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class EncodeGzip implements EncodeObject {

    @Override
    public byte[] encode(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(byteStream);
            gzip.write(data);
            gzip.flush();
            gzip.close();
            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return data;
        }
    }

    @Override
    public byte[] decode(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPInputStream gzip;
        try {
            gzip = new GZIPInputStream(byteInputStream);
            int res = 0;
            byte[] buffer = new byte[10240];
            while (res >= 0) {
                res = gzip.read(buffer, 0, buffer.length);
                if (res > 0) {
                    byteStream.write(buffer, 0, res);
                }
            }
            gzip.close();
            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return data;
        }
    }

}
