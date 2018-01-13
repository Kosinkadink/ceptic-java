package ceptic.managers.endpointmanager;

import java.util.HashMap;
import java.util.Map;

public class EndpointManagerClient implements EndpointManager {

    private Map<String, EndpointCommand> endpointMap = new HashMap<>();

    EndpointManagerClient() {}

    public void addCommand(String command, EndpointCommand function) {
        endpointMap.put(command, function);
    }

    // Get a command from endpoint map, returns null if does not exist
    public EndpointCommand getCommand(String command) {
        return endpointMap.getOrDefault(command, null);
    }

    // Removes command from endpoint map
    public void removeCommand(String command) {
        endpointMap.remove(command);
    }

}
