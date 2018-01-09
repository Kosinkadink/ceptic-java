package ceptic.managers.terminalmanager;

import java.util.HashMap;
import java.util.Map;

public class TerminalManager {

    private Map<String, CommandMethod> endpointMap = new HashMap<>();

    public TerminalManager() {}

    // interface
    interface CommandMethod {
        Map method(Object data);
    }

    public void addCommand(String command, CommandMethod function) {
        endpointMap.put(command, function);
    }

    public CommandMethod getCommand(String command) {
        return endpointMap.getOrDefault(command, null);
    }

    public void removeCommand(String command) {
        endpointMap.remove(command);
    }

}
