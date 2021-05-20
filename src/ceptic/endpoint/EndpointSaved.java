package ceptic.endpoint;

import java.util.ArrayList;
import java.util.List;

public class EndpointSaved {

    private final EndpointEntry entry;
    private final List<String> variables;

    public EndpointSaved(EndpointEntry entry, List<String> variables) {
        this.entry = entry;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new ArrayList<>();
        }
    }

    public EndpointEntry getEntry() {
        return entry;
    }

    public List<String> getVariables() {
        return variables;
    }
}
