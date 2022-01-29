package org.jedkos.ceptic.stream;

import org.jedkos.ceptic.common.RemovableManagers;
import org.jedkos.ceptic.common.Stopwatch;
import org.jedkos.ceptic.net.SocketCeptic;
import org.jedkos.ceptic.net.exceptions.SocketCepticException;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.jedkos.ceptic.stream.exceptions.StreamFrameSizeException;
import org.jedkos.ceptic.stream.exceptions.StreamHandlerStoppedException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class StreamManager extends Thread implements IStreamManager {

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

    private final Thread cleanThread;
    private final Thread receiveThread;
    private final long cleanThreadWaitTimeout = 100;

    private final ThreadPoolExecutor executor;

    private final LinkedBlockingDeque<StreamFrame> sendingDeque = new LinkedBlockingDeque<>();
    private final long sendingWaitTimeout = 100;

    private final ConcurrentHashMap<UUID, StreamHandler> streams = new ConcurrentHashMap<>();

    public StreamManager(SocketCeptic socket, UUID managerId, String destination, StreamSettings settings, RemovableManagers removable, boolean isServer) {
        this.socket = socket;
        this.managerId = managerId;
        this.destination = destination;
        this.settings = settings;
        this.removable = removable;
        this.isServer = isServer;
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        cleanThread = new Thread(this::cleanHandlers);
        cleanThread.setDaemon(true);
        receiveThread = new Thread(this::receiveFrames);
        receiveThread.setDaemon(true);
    }

    @Override
    public UUID getManagerId() {
        return managerId;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    //region Handler Management
    @Override
    public boolean isHandlerLimitReached() {
        if (settings.handlerMaxCount > 0) {
            return streams.size() >= settings.handlerMaxCount;
        }
        return false;
    }

    @Override
    public int getHandlerCount() {
        return streams.size();
    }

    private void stopHandler(UUID streamId) {
        StreamHandler handler = streams.remove(streamId);
        if (handler != null) {
            handler.stop();
        }
    }

    private void stopAllHandlers() {
        for (StreamHandler handler : streams.values()) {
            handler.stop();
        }
    }

    @Override
    public StreamHandler createHandler() {
        return createHandler(UUID.randomUUID());
    }

    @Override
    public StreamHandler createHandler(UUID streamId) {
        StreamHandler handler = new StreamHandler(streamId, settings, sendingDeque);
        streams.put(handler.getStreamId(), handler);
        return handler;
    }
    //endregion

    //region Stop Running
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
    //endregion

    public void run() {
        // start timers
        startTimers();
        // start threads
        cleanThread.start();
        receiveThread.start();
        try {
            while (!shouldStop) {
                StreamFrame frame = null;
                try {
                    frame = sendingDeque.pollFirst(sendingWaitTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) { }

                if (frame != null) {
                    // if close_all frame, send and then immediately stop manager
                    if (frame.isCloseAll()) {
                        // update keep alive; close_all frame about to be sent, so stream must be active
                        updateKeepAlive();
                        // try to send frame
                        try {
                            frame.send(socket);
                        } catch (SocketCepticException e) {
                            // trigger manager to stop if problem with socket
                            stopRunning(String.format("SocketCepticException: %s", e));
                            break;
                        }
                        stopRunning("sending close_all from handler " + frame.getStreamId());
                        break;
                    }
                    // get requesting stream
                    StreamHandler handler = streams.get(frame.getStreamId());
                    if (handler != null) {
                        // update keep alive; frame from valid handler, so stream must be active
                        updateKeepAlive();
                        // decrement size of handler's send buffer
                        handler.decrementSendBuffer(frame);
                        // try to send frame
                        try {
                            frame.send(socket);
                        } catch (SocketCepticException e) {
                            // trigger manager to stop if problem with socket
                            stopRunning(String.format("SocketCepticException: %s", e));
                            break;
                        }
                        // if sent close frame, close handler
                        if (frame.isClose()) {
                            handler.stop();
                        }
                    }
                }
            }
        } catch (Exception e) {
            stopRunning("Unexpected exception: " + e);
        }
        // manager is closing now
        // stop any remaining handlers
        stopAllHandlers();
        // close socket
        try {
            socket.close();
        } catch (SocketCepticException ignored) { }
        while (true) {
            try {
                cleanThread.join();
                receiveThread.join();
                break;
            } catch (InterruptedException ignored) { }
        }

        executor.shutdownNow();
        fullyStopped = true;
        removable.removeManager(managerId);
    }

    private void startTimers() {
        existenceTimer.start();
        keepAliveTimer.start();
    }

    /**
     * Clean Handler action for clean thread
     */
    private void cleanHandlers() {
        while (!shouldStop) {
            List<UUID> streamsToRemove = new ArrayList<>();
            for (StreamHandler handler : streams.values()) {
                if (handler.isTimedOut()) {
                    streamsToRemove.add(handler.getStreamId());
                }
            }
            // remove timed out streams
            for (UUID streamId : streamsToRemove) {
                stopHandler(streamId);
            }
            // check if manager is timed out, and if so stop running
            if (isTimedOut()) {
                stopRunning("manager timed out");
                break;
            }
            try {
                Thread.sleep(cleanThreadWaitTimeout);
            } catch (InterruptedException ignored) { }
        }
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
                StreamHandler handler = streams.get(frame.getStreamId());
                if (handler != null) {
                    handler.updateKeepAlive();
                }
            }
            // if handler is to be closed, add frame and it will take care of itself
            else if (frame.isClose()) {
                StreamHandler handler = streams.get(frame.getStreamId());
                if (handler != null) {
                    try {
                        handler.addToRead(frame);
                    } catch (StreamHandlerStoppedException ignored) { }
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
                executor.execute(() -> {
                    try {
                        removable.handleNewConnection(handler);
                    } catch (StreamException exception) {
                        handler.sendClose();
                    }
                });
            }
            // otherwise try to pass frame to appropriate handler
            else {
                StreamHandler handler = streams.get(frame.getStreamId());
                if (handler != null) {
                    try {
                        handler.addToRead(frame);
                    } catch (StreamHandlerStoppedException ignored) { }
                }
            }

        }
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

}
