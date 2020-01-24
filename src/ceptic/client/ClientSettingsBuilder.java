package ceptic.client;


public class ClientSettingsBuilder {

    private String _version = "1.0.0";
    private int _headersMinSize = 1024000;
    private int _headersMaxSize = 1024000;
    private int _frameMinSize = 1024000;
    private int _frameMaxSize = 1024000;
    private int _bodyMax = 102400000;
    private int _streamMinTimeout = 1;
    private int _streamTimeout = 5;
    private int _sendBufferSize = 102400000;
    private int _readBufferSize = 102400000;

    public ClientSettingsBuilder() { }

    public ClientSettings buildSettings() {
        // TODO: add verification for settings
        return new ClientSettings(_version, _headersMinSize, _headersMaxSize, _frameMinSize, _frameMaxSize,
                _bodyMax, _streamMinTimeout, _streamTimeout, _sendBufferSize, _readBufferSize, 9000);
    }

    public ClientSettingsBuilder version(String _version) {
        this._version = _version;
        return this;
    }

    public ClientSettingsBuilder headersMinSize(int _headersMinSize) {
        this._headersMinSize = _headersMinSize;
        return this;
    }

    public ClientSettingsBuilder headersMaxSize(int _headersMaxSize) {
        this._headersMaxSize = _headersMaxSize;
        return this;
    }

    public ClientSettingsBuilder frameMinSize(int _frameMinSize) {
        this._frameMinSize = _frameMinSize;
        return this;
    }

    public ClientSettingsBuilder frameMaxSize(int _frameMaxSize) {
        this._frameMaxSize = _frameMaxSize;
        return this;
    }

    public ClientSettingsBuilder bodyMax(int _bodyMax) {
        this._bodyMax = _bodyMax;
        return this;
    }

    public ClientSettingsBuilder streamMinTimeout(int _streamMinTimeout) {
        this._streamMinTimeout = _streamMinTimeout;
        return this;
    }

    public ClientSettingsBuilder streamTimeout(int _streamTimeout) {
        this._streamTimeout = _streamTimeout;
        return this;
    }

    public ClientSettingsBuilder sendBufferSize(int _sendBufferSize) {
        this._sendBufferSize = _sendBufferSize;
        return this;
    }

    public ClientSettingsBuilder readBufferSize(int _readBufferSize) {
        this._readBufferSize = _readBufferSize;
        return this;
    }

}
