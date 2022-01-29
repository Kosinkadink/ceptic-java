package org.jedkos.ceptic.common;

import org.jedkos.ceptic.stream.IStreamManager;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.exceptions.StreamException;

import java.util.UUID;

public interface RemovableManagers {

    IStreamManager removeManager(UUID managerId);
    void handleNewConnection(StreamHandler stream) throws StreamException;

}
