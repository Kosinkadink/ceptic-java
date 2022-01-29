package org.jedkos.ceptic.stream;

import java.util.UUID;

public class StreamFrameHelper {

    // create header frames
    public static StreamFrame createHeader(UUID streamId, byte[] data, StreamFrameInfo info) {
        return new StreamFrame(streamId, StreamFrameType.HEADER, info, data);
    }

    public static StreamFrame createHeaderLast(UUID streamId, byte[] data) {
        return StreamFrameHelper.createHeader(streamId, data, StreamFrameInfo.END);
    }

    public static StreamFrame createHeaderContinued(UUID streamId, byte[] data) {
        return StreamFrameHelper.createHeader(streamId, data, StreamFrameInfo.CONTINUE);
    }

    // create response frames
    public static StreamFrame createResponse(UUID streamId, byte[] data, StreamFrameInfo info) {
        return new StreamFrame(streamId, StreamFrameType.RESPONSE, info, data);
    }

    public static StreamFrame createResponseLast(UUID streamId, byte[] data) {
        return StreamFrameHelper.createResponse(streamId, data, StreamFrameInfo.END);
    }

    public static StreamFrame createResponseContinued(UUID streamId, byte[] data) {
        return StreamFrameHelper.createResponse(streamId, data, StreamFrameInfo.CONTINUE);
    }

    // create data frames
    public static StreamFrame createData(UUID streamId, byte[] data, StreamFrameInfo info) {
        return new StreamFrame(streamId, StreamFrameType.DATA, info, data);
    }

    public static StreamFrame createDataLast(UUID streamId, byte[] data) {
        return StreamFrameHelper.createData(streamId, data, StreamFrameInfo.END);
    }

    public static StreamFrame createDataContinued(UUID streamId, byte[] data) {
        return StreamFrameHelper.createData(streamId, data, StreamFrameInfo.CONTINUE);
    }

    // create keep alive frames
    public static StreamFrame createKeepAlive(UUID streamId) {
        return new StreamFrame(streamId, StreamFrameType.KEEP_ALIVE, StreamFrameInfo.END, new byte[0]);
    }

    // create close frames
    public static StreamFrame createClose(UUID streamId, byte[] data) {
        return new StreamFrame(streamId, StreamFrameType.CLOSE, StreamFrameInfo.END, data);
    }

    public static StreamFrame createClose(UUID streamId) {
        return StreamFrameHelper.createClose(streamId, new byte[0]);
    }

    public static StreamFrame createCloseAll(String streamId) {
        return new StreamFrame(StreamFrame.nullId, StreamFrameType.CLOSE_ALL, StreamFrameInfo.END, new byte[0]);
    }

}
