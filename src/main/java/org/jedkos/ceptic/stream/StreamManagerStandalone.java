package org.jedkos.ceptic.stream;

import org.jedkos.ceptic.common.RemovableManagers;
import org.jedkos.ceptic.common.Stopwatch;
import org.jedkos.ceptic.net.SocketCeptic;
import org.jedkos.ceptic.net.exceptions.SocketCepticException;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.jedkos.ceptic.stream.exceptions.StreamFrameSizeException;
import org.jedkos.ceptic.stream.exceptions.StreamHandlerStoppedException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

public class StreamManagerStandalone extends Thread implements IStreamManager {

    private final SocketCeptic socket;
    private final UUID managerId;
    private final String destination;
    private final StreamSettings settings;
    private final RemovableManagers removable;
    private final boolean isServer;

    private final Stopwatch existenceTimer = new Stopwatch();
    private final Stopwatch keepAliveTimer = new Stopwatch();
    private boolean shouldStop = false;
    private boolean fullyStopped = false;
    private String stopReason = "";

    private final ConcurrentHashMap.KeySetView<UUID,Boolean> sendingSet = ConcurrentHashMap.newKeySet();
    private final LinkedBlockingDeque<StreamFrame> sendingDeque = new LinkedBlockingDeque<>();
    private final long sendingWaitTimeout = 100;

    private final ThreadPoolExecutor executor;

    private StreamHandler stream = null;

    public StreamManagerStandalone(SocketCeptic socket, UUID managerId, String destination, StreamSettings settings, RemovableManagers removable, boolean isServer) {
        this.socket = socket;
        this.managerId = managerId;
        this.destination = destination;
        this.settings = settings;
        this.removable = removable;
        this.isServer = isServer;
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public void run() {
        // start timers
        startTimers();
        // start threads
        executor.execute(this::receiveFrames);
        try {
            while (!shouldStop) {
//                StreamFrame frame
//                UUID requestStreamId = null;
//                try {
//                    frame = sendingDeque.pollFirst(sendingWaitTimeout, TimeUnit.MILLISECONDS);
//                } catch (InterruptedException ignored) {
//                }
//                // clear sending trigger
//                clearSending();
//                if (stream != null) {
//                    while (!shouldStop) {
//                        StreamFrame frame = stream.getNextSendBufferFrame();
//                        // if no frame to be read, break out of loop
//                        if (frame == null) {
//                            break;
//                        }
//                        // try to send frame
//                        try {
//                            frame.send(socket);
//                        } catch (SocketCepticException e) {
//                            // trigger manager to stop if problem with socket
//                            stopRunning(String.format("SocketCepticException: %s", e));
//                            break;
//                        }
//                        // if sent close frame, close handler
//                        if (frame.isClose()) {
//                            stream.stop();
//                        } else if (frame.isCloseAll()) {
//                            stopRunning("sending close_all from handler " + requestStreamId);
//                            break;
//                        }
//                        // update keep alive; frame sent, so stream must be active
//                        updateKeepAlive();
//                    }
//                }
                if (isTimedOut()) {
                    stopRunning("manager timed out");
                }
            }
        } catch (Exception e) {
            stopRunning("Unexpected exception: " + e);
        }
        // manager is closing now
        // stop handler
        stream.stop();
        // close socket
        try {
            socket.close();
        } catch (SocketCepticException ignored) { }
        fullyStopped = true;
        removable.removeManager(managerId);
    }

    /**
     * Receive Frames action for receive thread
     */
    private void receiveFrames() {
        while (!shouldStop) {
            // try to get frame
            StreamFrame frame;
            try {
                frame = StreamFrame.fromSocket(socket, settings.frameMaxSize);
            } catch (SocketCepticException | StreamFrameSizeException e) {
                stopRunning(String.format("receiveFrames encountered %s: %s", e.getClass().getName(), e));
                break;
            }
            // update keep alive timer; just received frame, so connection must be alive
            updateKeepAlive();
            // if keep alive, update keep alive on handler and keep processing; just there to keep connection alive
            if (frame.isKeepAlive()) {
                if (stream != null) {
                    stream.updateKeepAlive();
                }
            }
            // if handler is to be closed, add frame and it will take care of itself
            else if (frame.isClose()) {
                if (stream != null) {
                    if (frame.getStreamId().equals(stream.getStreamId())) {
                        try {
                            stream.addToRead(frame);
                        } catch (StreamHandlerStoppedException ignored) { }
                    }
                }
            }
            // if close all, stop manager
            else if (frame.isCloseAll()) {
                stopRunning("received closeAll addressed to handler " + frame.getStreamId());
                break;
            }
            // if server and header frame, create new handler and pass frame
            else if (isServer && frame.isHeader()) {
                boolean shouldDecline = isHandlerLimitReached();
                StreamHandler handler = createHandler(frame.getStreamId());
                if (shouldDecline) {
                    handler.sendClose("Handler limit reached".getBytes(StandardCharsets.UTF_8));
                    continue;
                }
                try {
                    handler.addToRead(frame);
                } catch (StreamHandlerStoppedException e) {
                    continue;
                }
                try {
                    removable.handleNewConnection(handler);
                } catch (StreamException exception) {
                    handler.sendClose();
                }
            }
            // otherwise try to pass frame to appropriate handler
            else {
                if (frame.getStreamId().equals(stream.getStreamId())) {
                    if (stream != null) {
                        try {
                            stream.addToRead(frame);
                        } catch (StreamHandlerStoppedException ignored) { }
                    }
                }
            }
        }
    }

    @Override
    public UUID getManagerId() {
        return managerId;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    @Override
    public boolean isHandlerLimitReached() {
        return stream != null;
    }

    @Override
    public int getHandlerCount() {
        return stream == null ? 0 : 1;
    }

    @Override
    public StreamHandler createHandler() {
        return createHandler(UUID.randomUUID());
    }

    @Override
    public StreamHandler createHandler(UUID streamId) {
        if (stream == null || stream.isStopped()) {
            stream = new StreamHandler(streamId, settings, sendingDeque);
            return stream;
        }
        return null;
    }

    @Override
    public void stopRunning() {
        stopRunning("");
    }

    @Override
    public void stopRunning(String reason) {
        if (!shouldStop && !reason.isEmpty()) {
            stopReason = reason;
        }
        shouldStop = true;
        try {
            socket.close();
        } catch (SocketCepticException ignored) { }
    }

    @Override
    public boolean isFullyStopped() {
        return fullyStopped;
    }

    @Override
    public String getStopReason() {
        return stopReason;
    }

    //region Keep Alive Timeout
    @Override
    public boolean isTimedOut() {
        return keepAliveTimer.getTimeSeconds() > settings.streamTimeout;
    }

    private void updateKeepAlive() {
        keepAliveTimer.update();
    }
    //endregion

    private void clearSending() {
        sendingSet.clear();
    }

    private void startTimers() {
        existenceTimer.start();
        keepAliveTimer.start();
    }
}
