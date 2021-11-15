package ceptic.common;

import ceptic.stream.IStreamManager;
import ceptic.stream.StreamHandler;
import ceptic.stream.exceptions.StreamException;

import java.util.UUID;

public interface RemovableManagers {

    IStreamManager removeManager(UUID managerId);
    void handleNewConnection(StreamHandler stream) throws StreamException;

}
