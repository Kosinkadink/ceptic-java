package ceptic.client;

public class CepticClientBuilder {

    private ClientSettings clientSettings = null;
    private String certfile;
    private String keyfile;
    private String cafile;
    private boolean checkHostname = true;
    private boolean secure = true;

    public CepticClientBuilder() { }

    public CepticClient build() {
        if (clientSettings == null) {
            clientSettings = new ClientSettingsBuilder().build();
        }
        return new CepticClient(clientSettings, certfile, keyfile, cafile, checkHostname, secure);
    }

    public CepticClientBuilder settings(ClientSettings settings) {
        this.clientSettings = settings;
        return this;
    }

    public CepticClientBuilder certfile(String certfile) {
        this.certfile = certfile;
        return this;
    }

    public CepticClientBuilder keyfile(String keyfile) {
        this.keyfile = keyfile;
        return this;
    }

    public CepticClientBuilder cafile(String cafile) {
        this.cafile = cafile;
        return this;
    }

    public CepticClientBuilder checkHostname(boolean checkHostname) {
        this.checkHostname = checkHostname;
        return this;
    }

    public CepticClientBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

}
