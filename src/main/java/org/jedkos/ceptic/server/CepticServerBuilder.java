package org.jedkos.ceptic.server;

public class CepticServerBuilder {

    private ServerSettings _settings = null;
    private String _certfile;
    private String _keyfile;
    private String _cafile;
    private boolean _secure = true;

    public CepticServerBuilder() { }

    public CepticServer build() {
        if (_settings == null) {
            _settings = new ServerSettingsBuilder().build();
        }
        return new CepticServer(_settings, _certfile, _keyfile, _cafile, _secure);
    }

    public CepticServerBuilder settings(ServerSettings _settings) {
        this._settings = _settings;
        return this;
    }

    public CepticServerBuilder certfile(String _certfile) {
        this._certfile = _certfile;
        return this;
    }

    public CepticServerBuilder keyfile(String _keyfile) {
        this._keyfile = _keyfile;
        return this;
    }

    public CepticServerBuilder cafile(String _cafile) {
        this._cafile = _cafile;
        return this;
    }

    public CepticServerBuilder secure(boolean _secure) {
        this._secure = _secure;
        return this;
    }

}
