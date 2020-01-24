package ceptic.client;

public class ClientSettings {

    public String version;
    public int headersMinSize;
    public int headersMaxSize;
    public int frameMinSize;
    public int frameMaxSize;
    public int bodyMax;
    public int streamMinTimeout;
    public int streamTimeout;
    public int sendBufferSize;
    public int readBufferSize;
    public int defaultPort;


    public ClientSettings(String version, int headersMinSize, int headersMaxSize, int frameMinSize,
                          int frameMaxSize, int bodyMax, int streamMinTimeout, int streamTimeout,
                          int sendBufferSize, int readBufferSize, int defaultPort) {
        this.version = version;
        this.headersMinSize = headersMinSize;
        this.headersMaxSize = headersMaxSize;
        this.frameMinSize = frameMinSize;
        this.frameMaxSize = frameMaxSize;
        this.bodyMax = bodyMax;
        this.streamMinTimeout = streamMinTimeout;
        this.streamTimeout = streamTimeout;
        this.sendBufferSize = sendBufferSize;
        this.readBufferSize = readBufferSize;
        this.defaultPort = defaultPort;
    }
}
