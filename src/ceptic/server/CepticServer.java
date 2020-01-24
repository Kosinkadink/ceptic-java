package ceptic.server;

public class CepticServer {

    private ServerSettings settings;

    public CepticServer(ServerSettings settings, String certfile, String keyfile, String cafile, boolean secure) {
        this.settings = settings;
    }

}
