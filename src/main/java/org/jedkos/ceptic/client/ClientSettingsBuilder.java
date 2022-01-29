package org.jedkos.ceptic.client;


public class ClientSettingsBuilder {

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

    public ClientSettingsBuilder() { }

    public ClientSettings build() {
        // TODO: add verification for settings
        return new ClientSettings(version, headersMinSize, headersMaxSize, frameMinSize, frameMaxSize,
                bodyMax, streamMinTimeout, streamTimeout, sendBufferSize, readBufferSize, 9000);
    }

    public ClientSettingsBuilder version(String version) {
        this.version = version;
        return this;
    }

    public ClientSettingsBuilder headersMinSize(int headersMinSize) {
        this.headersMinSize = headersMinSize;
        return this;
    }

    public ClientSettingsBuilder headersMaxSize(int headersMaxSize) {
        this.headersMaxSize = headersMaxSize;
        return this;
    }

    public ClientSettingsBuilder frameMinSize(int frameMinSize) {
        this.frameMinSize = frameMinSize;
        return this;
    }

    public ClientSettingsBuilder frameMaxSize(int frameMaxSize) {
        this.frameMaxSize = frameMaxSize;
        return this;
    }

    public ClientSettingsBuilder bodyMax(int bodyMax) {
        this.bodyMax = bodyMax;
        return this;
    }

    public ClientSettingsBuilder streamMinTimeout(int streamMinTimeout) {
        this.streamMinTimeout = streamMinTimeout;
        return this;
    }

    public ClientSettingsBuilder streamTimeout(int streamTimeout) {
        this.streamTimeout = streamTimeout;
        return this;
    }

    public ClientSettingsBuilder sendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public ClientSettingsBuilder readBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

}
