package ceptic.stream;

import ceptic.encode.EncodeHandler;
import ceptic.net.SocketCeptic;
import ceptic.net.exceptions.SocketCepticException;
import ceptic.stream.exceptions.StreamFrameSizeException;

import java.util.UUID;

public class StreamFrame {

    static UUID nullId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final UUID streamId;
    private final StreamFrameType type;
    private final StreamFrameInfo info;
    private byte[] data;

    public StreamFrame(UUID streamId, StreamFrameType type, StreamFrameInfo info, byte[] data) {
        this.streamId = streamId;
        this.type = type;
        this.info = info;
        this.data = data;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public StreamFrameType getType() {
        return type;
    }

    public StreamFrameInfo getInfo() {
        return info;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return 38 + data.length;
    }

    public void encodeData(EncodeHandler encodeHandler) {
        data = encodeHandler.encode(data);
    }

    public void decodeData(EncodeHandler encodeHandler) {
        data = encodeHandler.decode(data);
    }

    public void send(SocketCeptic s) throws SocketCepticException {
        // send stream id
        s.sendRaw(streamId.toString().getBytes());
        // send type
        s.sendRaw(type.getValue().getBytes());
        // send info
        s.sendRaw(info.getValue().getBytes());
        // send data if data is set
        if (data.length > 0) {
            s.send(data);
        } else {
            s.sendRaw(String.format("%16s", "0").getBytes());
        }
    }

    public static StreamFrame fromSocket(SocketCeptic s, long maxDataLength) throws SocketCepticException, StreamFrameSizeException {
        // get stream id
        String rawStreamId = s.recvRawString(36);
        UUID streamId;
        try {
            streamId = UUID.fromString(rawStreamId);
        } catch (IllegalArgumentException e) {
            throw new StreamFrameSizeException("received rawStreamId that could not be parsed to UUID: " + rawStreamId);
        }
        // get type
        StreamFrameType type = StreamFrameType.fromValue(s.recvRawString(1));
        // get info
        StreamFrameInfo info = StreamFrameInfo.fromValue(s.recvRawString(1));
        // get data length
        String rawDataLength = s.recvRawString(16);
        int dataLength;
        try {
            dataLength = Integer.parseInt(rawDataLength.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw new StreamFrameSizeException(String.format("received dataLength could not be parsed to int: " +
                    "%s,%s,%s,%s",
                    streamId, type, info, rawDataLength));
        }
        // if data length is greater than max length, raise exception
        if (dataLength > maxDataLength) {
            throw new StreamFrameSizeException(String.format("dataLength (%d) greater than allowed max length of " +
                    "%d",
                    dataLength, maxDataLength));
        }
        // if data length not zero, get data
        byte[] data = new byte[0];
        if (dataLength > 0) {
            data = s.recvRaw(dataLength);
        }
        return new StreamFrame(streamId, type, info, data);
    }

    public boolean isHeader() {
        return type == StreamFrameType.HEADER;
    }

    public boolean isResponse() {
        return type == StreamFrameType.RESPONSE;
    }

    public boolean isData() {
        return type == StreamFrameType.DATA;
    }

    public boolean isKeepAlive() {
        return type == StreamFrameType.KEEP_ALIVE;
    }

    public boolean isClose() {
        return type == StreamFrameType.CLOSE;
    }

    public boolean isCloseAll() {
        return type == StreamFrameType.CLOSE_ALL;
    }

    public boolean isLast() {
        return info == StreamFrameInfo.END;
    }

    public boolean isContinued() {
        return info == StreamFrameInfo.CONTINUE;
    }

    public boolean isDataLast() {
        return isData() && isLast();
    }

    public boolean isDataContinued() {
        return isData() && isContinued();
    }

}
