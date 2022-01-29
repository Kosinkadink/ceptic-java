package org.jedkos.ceptic.endpoint;

import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;

import java.util.HashMap;

public interface EndpointEntry {

    CepticResponse perform(CepticRequest request, HashMap<String,String> values);

}
