package ceptic.endpoint;

import ceptic.common.CepticRequest;
import ceptic.common.CepticResponse;

import java.util.HashMap;

public interface EndpointEntry {

    CepticResponse perform(CepticRequest request, HashMap<String,String> values);

}
