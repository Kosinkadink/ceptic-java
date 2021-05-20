package ceptic.endpoint;

import ceptic.common.CepticRequest;
import ceptic.common.CepticResponse;

import java.util.HashMap;

public class EndpointValue {

    private final EndpointEntry entry;
    private final HashMap<String,String> values;

    public EndpointValue(EndpointEntry entry, HashMap<String,String> values) {
        this.entry = entry;
        if (values != null) {
            this.values = values;
        } else {
            this.values = new HashMap<>();
        }
    }

    public EndpointEntry getEntry() {
        return entry;
    }

    public HashMap<String, String> getValues() {
        return values;
    }

    public CepticResponse executeEndpointEntry(CepticRequest request) {
        return entry.perform(request, values);
    }
}
