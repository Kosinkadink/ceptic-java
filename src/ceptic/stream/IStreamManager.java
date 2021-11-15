package ceptic.stream;

import java.util.UUID;

public interface IStreamManager {

    UUID getManagerId();
    String getDestination();
    boolean isHandlerLimitReached();
    int getHandlerCount();
    StreamHandler createHandler();
    StreamHandler createHandler(UUID streamId);
    void stopRunning();
    void stopRunning(String reason);
    boolean isFullyStopped();
    String getStopReason();
    boolean isTimedOut();


}
