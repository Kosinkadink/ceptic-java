package ceptic.endpoint;

import ceptic.endpoint.exceptions.EndpointManagerException;
import ceptic.server.ServerSettings;

import java.util.concurrent.ConcurrentHashMap;

public class EndpointManager {

    protected final ConcurrentHashMap<String, CommandEntry> commandMap = new ConcurrentHashMap<>();
    protected final ServerSettings serverSettings;

    public EndpointManager(ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
    }

    public void addCommand(String command) {
        commandMap.put(command, new CommandEntry(command, serverSettings));
    }

    public void addCommand(String command, CommandSettings settings) {
        commandMap.put(command, new CommandEntry(command, settings));
    }

    public CommandEntry getCommand(String command) {
        return commandMap.get(command);
    }

    public CommandEntry removeCommand(String command) {
        return commandMap.remove(command);
    }

    public EndpointValue getEndpoint(String command, String endpoint) throws EndpointManagerException {
        CommandEntry commandEntry = getCommand(command);
        if (commandEntry != null) {
            return commandEntry.getEndpoint(endpoint);
        } else {
            throw new EndpointManagerException(String.format("command '%s not found", command));
        }
    }

    public void addEndpoint(String command, String endpoint, EndpointEntry entry) throws EndpointManagerException {
        CommandEntry commandEntry = getCommand(command);
        if (commandEntry != null) {
            commandEntry.addEndpoint(endpoint, entry);
        } else {
            throw new EndpointManagerException(String.format("command '%s' not found", command));
        }
    }

}
