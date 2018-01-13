package ceptic.managers.terminalmanager;

import java.util.HashMap;
import java.util.Map;

public class TerminalManager {

    private Map<String, TerminalCommand> endpointMap = new HashMap<>();

    public TerminalManager() {}

    public void addCommand(String command, TerminalCommand function) {
        endpointMap.put(command, function);
    }

    public TerminalCommand getCommand(String command) {
        return endpointMap.getOrDefault(command, null);
    }

    public void removeCommand(String command) {
        endpointMap.remove(command);
    }

    public Map performInput(String input) {
        if (input.isEmpty()) {
            System.out.println("no input given");
        }
        String[] user_input = input.split("\\s+");
        return endpointMap.get(user_input[0]).function(user_input);
    }

}
