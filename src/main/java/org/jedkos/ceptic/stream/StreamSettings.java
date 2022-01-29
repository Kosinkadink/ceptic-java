package org.jedkos.ceptic.stream;

public class StreamSettings {

    public final int sendBufferSize;
    public final int readBufferSize;
    public final int frameMaxSize;
    public final int headersMaxSize;
    public final long streamTimeout;
    public final int handlerMaxCount;
    public boolean verbose = false;


    public StreamSettings(int sendBufferSize, int readBufferSize, int frameMaxSize, int headersMaxSize,
                          int streamTimeout, int handlerMaxCount) {
        this.sendBufferSize = sendBufferSize;
        this.readBufferSize = readBufferSize;
        this.frameMaxSize = frameMaxSize;
        this.headersMaxSize = headersMaxSize;
        this.streamTimeout = streamTimeout;
        this.handlerMaxCount = handlerMaxCount;
    }

}
