package org.jedkos.ceptic.server;

import org.jedkos.ceptic.common.*;
import org.jedkos.ceptic.encode.EncodeGetter;
import org.jedkos.ceptic.encode.exceptions.UnknownEncodingException;
import org.jedkos.ceptic.endpoint.CommandSettings;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.endpoint.EndpointManager;
import org.jedkos.ceptic.endpoint.EndpointValue;
import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.jedkos.ceptic.net.SocketCeptic;
import org.jedkos.ceptic.net.exceptions.SocketCepticException;
import org.jedkos.ceptic.stream.IStreamManager;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.StreamManager;
import org.jedkos.ceptic.stream.StreamSettings;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.jedkos.ceptic.stream.exceptions.StreamTotalDataSizeException;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CepticServer extends Thread implements RemovableManagers {

    protected final ServerSettings settings;
    private final String certFile;
    private final String keyFile;
    private final String caFile;
    private final boolean secure;

    private ServerSocket serverSocket;

    private boolean shouldStop = false;
    private boolean stopped = false;

    protected final EndpointManager endpointManager;

    protected final ConcurrentHashMap<UUID, IStreamManager> managers = new ConcurrentHashMap<>();

    protected final ThreadPoolExecutor executor;

    protected CepticServer(ServerSettings settings, String certFile, String keyFile, String caFile, boolean secure) {
        this.settings = settings;
        this.certFile = certFile;
        this.keyFile = keyFile;
        this.caFile = caFile;
        this.secure = secure;
        // set daemon based on settings
        setDaemon(settings.daemon);
        // create executor
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        // create endpoint manager
        endpointManager = new EndpointManager(this.settings);
    }

    //region Getters
    public ServerSettings getSettings() {
        return settings;
    }

    public String getCertFile() {
        return certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public String getCaFile() {
        return caFile;
    }

    public boolean isSecure() {
        return secure;
    }
    //endregion

    //region Start
    public void run() {
        if (settings.verbose)
            System.out.printf("ceptic server started - version %s on port %d (secure: %b)\n",
                    settings.version, settings.port, secure);
        // create server socket
        try {
            serverSocket = new ServerSocket(settings.port, settings.requestQueueSize);
        } catch (IOException e) {
            if (settings.verbose)
                System.out.println("Issue creating ServerSocket: " + e);
            stopRunning();
            return;
        }
        // set timeout
        try {
            serverSocket.setSoTimeout(5000);
        } catch (SocketException e) {
            if (settings.verbose)
                System.out.println("Issue setting ServerSocket timeout: " + e);
            stopRunning();
            return;
        }
        while (!shouldStop) {
            Socket socket;
            // accept socket
            try {
                socket = serverSocket.accept();
            }
            catch (SocketTimeoutException e) {
                continue;
            }
            catch (IOException e) {
                if (!shouldStop && settings.verbose)
                    System.out.println("Issue accepting Socket: " + e);
                stopRunning();
                continue;
            }
            // set socket timeout
            try {
                socket.setSoTimeout(5000);
            } catch (SocketException e) {
                if (settings.verbose)
                    System.out.println("Issue setting timeout of accepted socket: " + e);
                stopRunning();
                continue;
            }
            // handle accepted socket
            executor.execute(() -> {
                try {
                    createNewManager(socket);
                } catch (SocketCepticException e) {
                    if (settings.verbose)
                        System.out.println("Issue with createNewManager: " + e);
                }
            });
        }
        // server is closing
        // close socket
        try {
            serverSocket.close();
        } catch (IOException e) {
            if (settings.verbose)
                System.out.println("Issue closing ServerSocket: " + e);
        }
        // shut down all managers
        removeAllManagers();
        // shut down executor
        executor.shutdownNow();
        // done stopping
        stopped = true;
    }
    //endregion

    //region Stop
    public void stopRunning() {
        shouldStop = true;
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException ignored) { }
    }

    public boolean isStopped() {
        return stopped;
    }
    //endregion

    //region Add Behavior
    public void addCommand(String command) {
        endpointManager.addCommand(command);
    }

    public void addCommand(String command, CommandSettings settings) {
        endpointManager.addCommand(command, settings);
    }

    public void addRoute(String command, String endpoint, EndpointEntry entry) throws EndpointManagerException {
        endpointManager.addEndpoint(command, endpoint, entry);
    }

    public void addRoute(String command, String endpoint, EndpointEntry entry, CommandSettings settings) throws EndpointManagerException {
        endpointManager.addEndpoint(command, endpoint, entry, settings);
    }
    //endregion

    //region Connection
    public void handleNewConnection(StreamHandler stream) throws StreamException {
        // store errors in request
        JSONArray errors = new JSONArray();
        // get request from request data
        CepticRequest request = CepticRequest.fromData(stream.readHeaderDataRaw());
        // begin checking validity of request
        // check that command and endpoint are of valid length
        if (request.getCommand().length() > Constants.COMMAND_LENGTH) {
            errors.add(String.format("command too long; should be no more than %d but was %d",
                    Constants.COMMAND_LENGTH, request.getCommand().length()));
        }
        if (request.getEndpoint().length() > Constants.ENDPOINT_LENGTH) {
            errors.add(String.format("endpoint too long; should be no more than %d but was %d",
                    Constants.ENDPOINT_LENGTH, request.getEndpoint().length()));
        }
        // if no errors yet, get endpoint from endpoint manager
        EndpointValue endpointValue = null;
        if (errors.isEmpty()) {
            try {
                // get endpoint value from endpoint manager
                endpointValue = endpointManager.getEndpoint(request.getCommand(), request.getEndpoint());
                // check that headers are valid
                errors.addAll(checkNewConnectionHeaders(request));
            } catch (EndpointManagerException e) {
                errors.add(e.toString());
            }
        }
        // if errors or no endpointValue found, send CepticResponse with BadRequest
        if (!errors.isEmpty() || endpointValue == null) {
            stream.sendResponse(new CepticResponse(CepticStatusCode.BAD_REQUEST, errors));
            stream.sendClose();
            return;
        }
        // send positive response and continue with endpoint function
        stream.sendResponse(new CepticResponse(CepticStatusCode.OK));
        // set stream encoding, based on request header
        try {
            stream.setEncode(request.getEncoding());
        } catch (UnknownEncodingException e) {
            stream.sendClose(e.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        // get body if content length header is present
        if (request.hasContentLength()) {
            // TODO: add file transfer functionality (maybe?)
            try {
                request.setBody(stream.readDataRaw(request.getContentLength()));
            } catch (StreamTotalDataSizeException e) {
                stream.sendClose("body received is greater than reported Content-Length".getBytes(StandardCharsets.UTF_8));
                return;
            }
        }
        // set request stream
        request.setStream(stream);
        // perform endpoint function and get back response
        CepticResponse response = endpointValue.executeEndpointEntry(request);
        // send response
        stream.sendResponse(response);
        // send body if content length header present
        if (response.hasContentLength()) {
            // TODO: add file transfer functionality (maybe?)
            try {
                stream.sendData(response.getBody());
            } catch (StreamException e) {
                stream.sendClose(String.format("SERVER STREAM EXCEPTION: %s", e).getBytes(StandardCharsets.UTF_8));
                if (settings.verbose)
                    System.out.printf("StreamException type %s raised while sending response body: %s\n",
                            e.getClass().toString(), e);
                return;
            }
        }
        // close connection
        stream.sendClose("SERVER COMMAND COMPLETE".getBytes(StandardCharsets.UTF_8));
    }
    //endregion

    //region Managers
    protected void createNewManager(Socket rawSocket) throws SocketCepticException {
        if (settings.verbose)
            System.out.println("Got a connection from " + rawSocket.getRemoteSocketAddress());
        // TODO: wrap with SSL to get SSLSocket
        // wrap as SocketCeptic
        SocketCeptic socket = new SocketCeptic(rawSocket);

        // get client version
        String clientVersion = socket.recvRawString(16).trim();
        // get client frameMinSize
        String clientFrameMinSizeString = socket.recvRawString(16).trim();
        // get client frameMaxSize
        String clientFrameMaxSizeString = socket.recvRawString(16).trim();
        // get client headersMinSize
        String clientHeadersMinSizeString = socket.recvRawString(16).trim();
        // get client headersMaxSize
        String clientHeadersMaxSizeString = socket.recvRawString(16).trim();
        // get client streamMinTimeout
        String clientStreamMinTimeoutString = socket.recvRawString(4).trim();
        // get client streamTimeout
        String clientStreamTimeoutString = socket.recvRawString(4).trim();

        // see if values are acceptable
        StringBuilder errors = new StringBuilder();
        // TODO: add version checking
        // convert to int
        StreamSettings streamSettings = null;
        int clientFrameMinSize;
        int clientFrameMaxSize;
        int clientHeadersMinSize;
        int clientHeadersMaxSize;
        int clientStreamMinTimeout;
        int clientStreamTimeout;
        try {
            clientFrameMinSize = Integer.parseInt(clientFrameMinSizeString);
            clientFrameMaxSize = Integer.parseInt(clientFrameMaxSizeString);
            clientHeadersMinSize = Integer.parseInt(clientHeadersMinSizeString);
            clientHeadersMaxSize = Integer.parseInt(clientHeadersMaxSizeString);
            clientStreamMinTimeout = Integer.parseInt(clientStreamMinTimeoutString);
            clientStreamTimeout = Integer.parseInt(clientStreamTimeoutString);
            // check value bounds
            SettingBoundedResult frameMaxSize = checkIfSettingBounded(clientFrameMinSize, clientFrameMaxSize,
                    settings.frameMinSize, settings.frameMaxSize, "frame size");
            SettingBoundedResult headersMaxSize = checkIfSettingBounded(clientHeadersMinSize, clientHeadersMaxSize,
                    settings.headersMinSize, settings.headersMaxSize, "header size");
            SettingBoundedResult streamTimeout = checkIfSettingBounded(clientStreamMinTimeout, clientStreamTimeout,
                    settings.streamMinTimeout, settings.streamTimeout, "stream timeout");
            // add errors, if applicable
            if (frameMaxSize.hasError())
                errors.append(frameMaxSize.getError());
            if (headersMaxSize.hasError())
                errors.append(headersMaxSize.getError());
            if (streamTimeout.hasError())
                errors.append(streamTimeout.getError());

            // create stream settings
            streamSettings = new StreamSettings(settings.sendBufferSize, settings.readBufferSize,
                    frameMaxSize.getValue(), headersMaxSize.getValue(), streamTimeout.getValue(),
                    settings.handlerMaxCount);
            streamSettings.verbose = settings.verbose;
        } catch (NumberFormatException e) {
            errors.append(String.format("Client's thresholds were not all integers: %s,%s,%s,%s,%s,%s",
                    clientFrameMinSizeString, clientFrameMaxSizeString,
                    clientHeadersMinSizeString, clientHeadersMaxSizeString,
                    clientStreamMinTimeoutString, clientStreamTimeoutString));
        }

        // send response
        // if errors present, send negative response with explanation
        if (errors.length() > 0 || streamSettings == null) {
            socket.sendRaw("n");
            socket.send(errors.substring(0, Math.max(1024, errors.length())));
            if (settings.verbose)
                System.out.println("Client not compatible with server settings, connection terminated.");
            socket.close();
            return;
        }
        // otherwise send positive response along with decided values
        socket.sendRaw("y");
        socket.sendRaw(String.format("%16s", streamSettings.frameMaxSize));
        socket.sendRaw(String.format("%16s", streamSettings.headersMaxSize));
        socket.sendRaw(String.format("%4s", streamSettings.streamTimeout));
        socket.sendRaw(String.format("%4s", streamSettings.handlerMaxCount));
        // create manager
        StreamManager manager = new StreamManager(socket, UUID.randomUUID(), "manager", streamSettings,
                this, true);
        // add and start manager
        addManager(manager);
        manager.start();
    }

    protected void addManager(IStreamManager manager) {
        // add manager to map
        managers.put(manager.getManagerId(), manager);
    }

    protected IStreamManager getManager(UUID managerId) {
        return managers.get(managerId);
    }

    @Override
    public IStreamManager removeManager(UUID managerId) {
        // remove manager from managers map
        IStreamManager manager = managers.remove(managerId);
        if (manager != null) {
            manager.stopRunning("removed by CepticServer");
        }
        return manager;
    }

    protected List<IStreamManager> removeAllManagers() {
        List<IStreamManager> removedManagers = new ArrayList<>();
        for (UUID managerId : managers.keySet()) {
            removedManagers.add(removeManager(managerId));
        }
        return removedManagers;
    }
    //endregion

    //region Helper Methods
    protected SettingBoundedResult checkIfSettingBounded(int clientMin, int clientMax, int serverMin, int serverMax,
                                                         String settingName) {
        String error = "";
        int value = -1;
        if (clientMax <= serverMax) {
            if (clientMax < serverMin)
                error = String.format("client max %1$s (%2$s) is greater than server's max %1$s (%3$s)",
                        settingName, clientMax, serverMax);
            else
                value = clientMax;
        }
        else {
            // since client max is greater than server max, check if server max is appropriate
            if (clientMin > serverMax)
                // client min greater than server max, so not compatible
                error = String.format("client min %1$s (%2$s) is greater than server's max %1$s (%3$s)",
                        settingName, clientMin, serverMax);
            // otherwise use server max
            else
                value = serverMax;
        }
        return new SettingBoundedResult(error, value);
    }

    protected JSONArray checkNewConnectionHeaders(CepticRequest request) {
        JSONArray errors = new JSONArray();
        // check that content length is of allowed length
        if (request.hasContentLength()) {
            // if content length is longer than set max body length, invalid
            if (request.getContentLength() > settings.bodyMax) {
                errors.add(String.format("Content-Length (%d) exceeds server's allowed max body length of %d",
                        request.getContentLength(), settings.bodyMax));
            }
        }
        // check that encoding is recognized and valid
        if (request.hasEncoding()) {
            try {
                EncodeGetter.get(request.getEncoding());
            } catch (UnknownEncodingException e) {
                errors.add(e.toString());
            }
        }
        return errors;
    }
    //endregion

}
