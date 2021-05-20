package ceptic.common;

import ceptic.stream.StreamHandler;
import ceptic.stream.StreamManager;
import ceptic.stream.exceptions.StreamException;

import java.util.UUID;

public interface RemovableManagers {

    StreamManager removeManager(UUID managerId);
    void handleNewConnection(StreamHandler stream) throws StreamException;

}
