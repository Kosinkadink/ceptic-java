package ceptic.managers.endpointmanager;

import ceptic.net.SocketCeptic;

import java.util.Map;

public interface EndpointCommandClient extends EndpointCommand {
    Map function(SocketCeptic s, Object data, Object dataToStore);
}
