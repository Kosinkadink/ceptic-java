package ceptic.client;

public class CepticClientBuilder {

    private ClientSettings _settings = null;
    private String _certfile;
    private String _keyfile;
    private String _cafile;
    private boolean _checkHostname = true;
    private boolean _secure = true;

    public CepticClientBuilder() { }

    public CepticClient build() {
        if (_settings == null) {
            _settings = new ClientSettingsBuilder().build();
        }
        return new CepticClient(_settings, _certfile, _keyfile, _cafile, _checkHostname, _secure);
    }

    public CepticClientBuilder settings(ClientSettings _settings) {
        this._settings = _settings;
        return this;
    }

    public CepticClientBuilder certfile(String _certfile) {
        this._certfile = _certfile;
        return this;
    }

    public CepticClientBuilder keyfile(String _keyfile) {
        this._keyfile = _keyfile;
        return this;
    }

    public CepticClientBuilder cafile(String _cafile) {
        this._cafile = _cafile;
        return this;
    }

    public CepticClientBuilder checkHostname(boolean _checkHostname) {
        this._checkHostname = _checkHostname;
        return this;
    }

    public CepticClientBuilder secure(boolean _secure) {
        this._secure = _secure;
        return this;
    }

}
