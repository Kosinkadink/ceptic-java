package ceptic.server;

public class ServerSettingsBuilder {

    private int _port = 9000;
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
    private int _handlerMaxCount = 0;
    private int _requestQueueSize = 10;
    private boolean _verbose = false;

    public ServerSettingsBuilder() { }

    public ServerSettings buildSettings() {
        // TODO: add verification for settings
        return new ServerSettings(_port, _version, _headersMinSize, _headersMaxSize, _frameMinSize, _frameMaxSize,
                _bodyMax, _streamMinTimeout, _streamTimeout, _sendBufferSize, _readBufferSize, _handlerMaxCount,
                _requestQueueSize, _verbose);
    }

    public ServerSettingsBuilder port(int _port) {
        this._port = _port;
        return this;
    }

    public ServerSettingsBuilder version(String _version) {
        this._version = _version;
        return this;
    }

    public ServerSettingsBuilder headersMinSize(int _headersMinSize) {
        this._headersMinSize = _headersMinSize;
        return this;
    }

    public ServerSettingsBuilder headersMaxSize(int _headersMaxSize) {
        this._headersMaxSize = _headersMaxSize;
        return this;
    }

    public ServerSettingsBuilder frameMinSize(int _frameMinSize) {
        this._frameMinSize = _frameMinSize;
        return this;
    }

    public ServerSettingsBuilder frameMaxSize(int _frameMaxSize) {
        this._frameMaxSize = _frameMaxSize;
        return this;
    }

    public ServerSettingsBuilder bodyMax(int _bodyMax) {
        this._bodyMax = _bodyMax;
        return this;
    }

    public ServerSettingsBuilder streamMinTimeout(int _streamMinTimeout) {
        this._streamMinTimeout = _streamMinTimeout;
        return this;
    }

    public ServerSettingsBuilder streamTimeout(int _streamTimeout) {
        this._streamTimeout = _streamTimeout;
        return this;
    }

    public ServerSettingsBuilder sendBufferSize(int _sendBufferSize) {
        this._sendBufferSize = _sendBufferSize;
        return this;
    }

    public ServerSettingsBuilder readBufferSize(int _readBufferSize) {
        this._readBufferSize = _readBufferSize;
        return this;
    }

    public ServerSettingsBuilder handlerMaxCount(int _handlerMaxCount) {
        this._handlerMaxCount = _handlerMaxCount;
        return this;
    }

    public ServerSettingsBuilder requestQueueSize(int _requestQueueSize) {
        this._requestQueueSize = _requestQueueSize;
        return this;
    }

    public ServerSettingsBuilder verbose(boolean _verbose) {
        this._verbose = _verbose;
        return this;
    }
}
