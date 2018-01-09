package ceptic.managers.endpointmanager;

import ceptic.net.SocketCeptic;

import java.util.HashMap;
import java.util.Map;

public class EndpointManagerClient implements EndpointManager {

    private Map<String, CommandMethod> endpointMap = new HashMap<>();

    EndpointManagerClient() {}

    //interface for returning a method
    interface CommandMethod {
        Map method(SocketCeptic s, Object data, Object dataToStore);
    }

    public void addCommand(String command, CommandMethod function) {
        endpointMap.put(command, function);
    }

    // Get a command from endpoint map, returns null if does not exist
    public CommandMethod getCommand(String command) {
        return endpointMap.getOrDefault(command, null);
    }

    // Removes command from endpoint map
    public void removeCommand(String command) {
        endpointMap.remove(command);
    }

}
