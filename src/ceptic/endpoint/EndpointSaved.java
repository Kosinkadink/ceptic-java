package ceptic.endpoint;

import java.util.ArrayList;
import java.util.List;

public class EndpointSaved {

    private final EndpointEntry entry;
    private final List<String> variables;
    private final CommandSettings settings;

    public EndpointSaved(EndpointEntry entry, List<String> variables, CommandSettings settings) {
        this.entry = entry;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new ArrayList<>();
        }
        this.settings = settings;
    }

    public EndpointEntry getEntry() {
        return entry;
    }

    public List<String> getVariables() {
        return variables;
    }

    public CommandSettings getSettings() {
        return settings;
    }
}
