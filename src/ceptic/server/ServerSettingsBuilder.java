package ceptic.server;

public class ServerSettingsBuilder {

    private int port = 9000;
    private String version = "1.0.0";
    private int headersMinSize = 1024000;
    private int headersMaxSize = 1024000;
    private int frameMinSize = 1024000;
    private int frameMaxSize = 1024000;
    private int bodyMax = 102400000;
    private int streamMinTimeout = 1;
    private int streamTimeout = 5;
    private int sendBufferSize = 102400000;
    private int readBufferSize = 102400000;
    private int handlerMaxCount = 0;
    private int requestQueueSize = 10;
    private boolean verbose = false;
    private boolean daemon = false;

    public ServerSettingsBuilder() { }

    public ServerSettings build() {
        // TODO: add verification for settings
        return new ServerSettings(port, version, headersMinSize, headersMaxSize, frameMinSize, frameMaxSize,
                bodyMax, streamMinTimeout, streamTimeout, sendBufferSize, readBufferSize, handlerMaxCount,
                requestQueueSize, verbose, daemon);
    }

    public ServerSettingsBuilder port(int port) {
        this.port = port;
        return this;
    }

    public ServerSettingsBuilder version(String version) {
        this.version = version;
        return this;
    }

    public ServerSettingsBuilder headersMinSize(int headersMinSize) {
        this.headersMinSize = headersMinSize;
        return this;
    }

    public ServerSettingsBuilder headersMaxSize(int headersMaxSize) {
        this.headersMaxSize = headersMaxSize;
        return this;
    }

    public ServerSettingsBuilder frameMinSize(int frameMinSize) {
        this.frameMinSize = frameMinSize;
        return this;
    }

    public ServerSettingsBuilder frameMaxSize(int frameMaxSize) {
        this.frameMaxSize = frameMaxSize;
        return this;
    }

    public ServerSettingsBuilder bodyMax(int bodyMax) {
        this.bodyMax = bodyMax;
        return this;
    }

    public ServerSettingsBuilder streamMinTimeout(int streamMinTimeout) {
        this.streamMinTimeout = streamMinTimeout;
        return this;
    }

    public ServerSettingsBuilder streamTimeout(int streamTimeout) {
        this.streamTimeout = streamTimeout;
        return this;
    }

    public ServerSettingsBuilder sendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public ServerSettingsBuilder readBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public ServerSettingsBuilder handlerMaxCount(int handlerMaxCount) {
        this.handlerMaxCount = handlerMaxCount;
        return this;
    }

    public ServerSettingsBuilder requestQueueSize(int requestQueueSize) {
        this.requestQueueSize = requestQueueSize;
        return this;
    }

    public ServerSettingsBuilder verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public ServerSettingsBuilder daemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }
}
