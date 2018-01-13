package ceptic.managers.endpointmanager;

public interface EndpointManager {

    void addCommand(String command, EndpointCommand function);
    EndpointCommand getCommand(String command);
    void removeCommand(String command);

}
