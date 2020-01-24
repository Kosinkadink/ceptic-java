package ceptic.client;

public class CepticClient {

    private ClientSettings settings;

    public CepticClient(ClientSettings settings, String certfile, String keyfile, String cafile,
                           boolean check_hostname, boolean secure) {
        this.settings = settings;
    }

}
