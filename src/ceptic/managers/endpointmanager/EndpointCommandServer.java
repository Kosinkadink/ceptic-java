package ceptic.managers.endpointmanager;

import ceptic.net.SocketCeptic;

import java.util.Map;

public interface EndpointCommandServer extends EndpointCommand {
    Map function(SocketCeptic s, Object data);
}
