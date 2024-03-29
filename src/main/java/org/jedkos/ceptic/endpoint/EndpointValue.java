package org.jedkos.ceptic.endpoint;

import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;

import java.util.HashMap;

public class EndpointValue {

    private final EndpointEntry entry;
    private final HashMap<String,String> values;
    private final CommandSettings settings;

    public EndpointValue(EndpointEntry entry, HashMap<String,String> values, CommandSettings settings) {
        this.entry = entry;
        if (values != null) {
            this.values = values;
        } else {
            this.values = new HashMap<>();
        }
        this.settings = settings;
    }

    public EndpointEntry getEntry() {
        return entry;
    }

    public HashMap<String, String> getValues() {
        return values;
    }

    public CommandSettings getSettings() {
        return settings;
    }

    public CepticResponse executeEndpointEntry(CepticRequest request) {
        return entry.perform(request, values);
    }
}
