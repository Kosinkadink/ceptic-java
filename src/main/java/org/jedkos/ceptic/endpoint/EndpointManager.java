package org.jedkos.ceptic.endpoint;

import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.jedkos.ceptic.server.ServerSettings;

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
        addEndpoint(command, endpoint, entry, null);
    }

    public void addEndpoint(String command, String endpoint, EndpointEntry entry, CommandSettings settings) throws EndpointManagerException {
        CommandEntry commandEntry = getCommand(command);
        if (commandEntry != null) {
            commandEntry.addEndpoint(endpoint, entry, settings);
        } else {
            throw new EndpointManagerException(String.format("command '%s' not found", command));
        }
    }

    public EndpointSaved removeEndpoint(String command, String endpoint) {
        CommandEntry commandEntry = getCommand(command);
        if (commandEntry != null) {
            return commandEntry.removeEndpoint(endpoint);
        }
        return null;
    }

}
