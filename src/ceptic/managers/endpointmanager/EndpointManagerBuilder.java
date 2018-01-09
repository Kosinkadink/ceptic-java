package ceptic.managers.endpointmanager;

public class EndpointManagerBuilder {

    public EndpointManagerBuilder() { }

    public EndpointManager buildClientManager() {
        return new EndpointManagerClient();
    }

    public EndpointManager buildServerManager() {
        return new EndpointManagerServer();
    }

}
