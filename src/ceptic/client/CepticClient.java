package ceptic.client;

import ceptic.common.*;
import ceptic.common.exceptions.CepticException;
import ceptic.common.exceptions.CepticIOException;
import ceptic.encode.exceptions.UnknownEncodingException;
import ceptic.net.SocketCeptic;
import ceptic.net.exceptions.SocketCepticException;
import ceptic.stream.StreamData;
import ceptic.stream.StreamHandler;
import ceptic.stream.StreamManager;
import ceptic.stream.StreamSettings;
import ceptic.stream.exceptions.StreamException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CepticClient implements RemovableManagers {

    private final ClientSettings settings;
    private final String certFile;
    private final String keyFile;
    private final String caFile;
    private final boolean checkHostname;
    private final boolean secure;

    private final ConcurrentHashMap<UUID, StreamManager> managers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UUID,Boolean>> destinationMap = new ConcurrentHashMap<>();

    protected CepticClient(ClientSettings settings, String certFile, String keyFile, String caFile,
                           boolean checkHostname, boolean secure) {
        this.settings = settings;
        this.certFile = certFile;
        this.keyFile = keyFile;
        this.caFile = caFile;
        this.checkHostname = checkHostname;
        this.secure = secure;
    }

    //region Getters
    public ClientSettings getSettings() {
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

    public boolean isCheckHostname() {
        return checkHostname;
    }

    public boolean isSecure() {
        return secure;
    }
    //endregion

    //region Stop
    public void stop() {
        removeAllManagers();
    }
    //endregion

    //region Connection
    public CepticResponse connect(CepticRequest request) throws CepticException {
        return connect(request, SpreadType.Normal);
    }

    public CepticResponse connectNew(CepticRequest request) throws CepticException {
        return connect(request, SpreadType.New);
    }

    public CepticResponse connectStandalone(CepticRequest request) throws CepticException {
        return connect(request, SpreadType.Standalone);
    }

    protected CepticResponse connect(CepticRequest request, SpreadType spread) throws CepticException {
        // verify and prepare request
        request.verifyAndPrepare();
        // create destination based off of host and port
        String destination = String.format("%s:%d", request.getHost(), request.getPort());

        StreamManager manager = null;
        if (spread == SpreadType.Normal) {
            manager = getAvailableManagerForDestination(destination);
            if (manager != null) {
                StreamHandler handler = manager.createHandler();
                // connect to server with this handler, returning CepticResponse
                return connectWithHandler(handler, request);
            }
        }
        // if Standalone, make marked destination be random UUID to avoid reuse
        else if (spread == SpreadType.Standalone) {
            destination = UUID.randomUUID().toString();
        }
        // create new manager
        manager = createNewManager(request, destination);
        StreamHandler handler = manager.createHandler();
        // connect to server with this handler, returning CepticResponse
        return connectWithHandler(handler, request);
    }

    protected CepticResponse connectWithHandler(StreamHandler stream, CepticRequest request) throws StreamException, UnknownEncodingException {
        try {
            // create frames from request and send
            stream.sendRequest(request);
            // wait for response
            StreamData streamData = stream.readData(stream.getSettings().frameMaxSize);
            if (!streamData.isResponse()) {
                throw new StreamException("No CepticResponse found in response");
            }
            CepticResponse response = streamData.getResponse();
            // if not success status code, close stream and return response
            if (!response.getStatusCode().isSuccess()) {
                stream.sendClose();
                return response;
            }
            // set stream encoding based on request header
            stream.setEncode(request.getEncoding());
            // send body if content length header present and greater than 0
            if (request.hasContentLength()) {
                // TODO: add file transfer functionality (maybe)
                stream.sendData(request.getBody());
            }
            // get response
            streamData = stream.readData(stream.getSettings().frameMaxSize);
            if (!streamData.isResponse()) {
                throw new StreamException("No CepticResponse found in post-body response");
            }
            response = streamData.getResponse();
            response.setStream(stream);
            // if content length header present, receive response body
            if (response.hasContentLength()) {
                // TODO: add file transfer functionality (maybe)
                // check that content length is within limit
                if (response.getContentLength() > settings.bodyMax) {
                    throw new StreamException(String.format("Response content length (%d) is greater than client allows (%d)", response.getContentLength(), settings.bodyMax));
                }
                // receive body
                response.setBody(stream.readDataRaw(response.getContentLength()));
            }
            // close stream if no Exchange header on response or request
            if (!response.getExchange() || !request.getExchange()) {
                stream.sendClose();
            }
            return response;
        }
        catch (CepticException exception) {
            stream.sendClose();
            throw exception;
        }
    }
    //endregion

    //region Managers
    protected StreamManager createNewManager(CepticRequest request, String destination) throws CepticIOException, SocketCepticException {
        Socket rawSocket;
        try {
            // TODO: replace with SSLSocket
            rawSocket = new Socket(request.getHost(), request.getPort());
        } catch (IOException e) {
            throw new CepticIOException("Issue creating raw Socket: " + e);
        }
        try {
            rawSocket.setSoTimeout(5000);
        } catch (SocketException e) {
            throw new CepticIOException("Issue setting Socket timeout: " + e);
        }
        // wrap as SocketCeptic
        SocketCeptic socket = new SocketCeptic(rawSocket);
        // send version
        socket.sendRaw(String.format("%16s", settings.version));
        // send frameMinSize
        socket.sendRaw(String.format("%16s", settings.frameMinSize));
        // send frameMaxSize
        socket.sendRaw(String.format("%16s", settings.frameMaxSize));
        // send headersMinSize
        socket.sendRaw(String.format("%16s", settings.headersMinSize));
        // send headersMaxSize
        socket.sendRaw(String.format("%16s", settings.headersMaxSize));
        // send streamMinTimeout
        socket.sendRaw(String.format("%4s", settings.streamMinTimeout));
        // send streamTimeout
        socket.sendRaw(String.format("%4s", settings.streamTimeout));
        // get response
        byte[] response = socket.recvRaw(1);
        // if not positive, get additional info and raise exception
        if (response[0] != 'y') {
            String errorString = socket.recvString(1024);
            throw new CepticIOException("Client settings not compatible with server settings: " + errorString);
        }
        // otherwise received decided values
        String serverFrameMaxSizeStr = socket.recvRawString(16).trim();
        String serverHeaderMaxSizeStr = socket.recvRawString(16).trim();
        String serverStreamTimeoutStr = socket.recvRawString(4).trim();
        String serverHandlerMaxCountStr = socket.recvRawString(4).trim();
        // attempt to convert to integers
        int frameMaxSize;
        int headersMaxSize;
        int streamTimeout;
        int handlerMaxCount;
        try {
            frameMaxSize = Integer.parseInt(serverFrameMaxSizeStr);
            headersMaxSize = Integer.parseInt(serverHeaderMaxSizeStr);
            streamTimeout = Integer.parseInt(serverStreamTimeoutStr);
            handlerMaxCount = Integer.parseInt(serverHandlerMaxCountStr);
        } catch (NumberFormatException e) {
            throw new CepticIOException(String.format("Server's values were not all integers, " +
                    "could not proceed: %s,%s,%s,%s",
                    serverFrameMaxSizeStr, serverHeaderMaxSizeStr, serverStreamTimeoutStr, serverHandlerMaxCountStr));
        }
        // verify server's chosen values are valid for client
        // TODO: expand checks to check lower bounds
        StreamSettings streamSettings = new StreamSettings(settings.sendBufferSize, settings.readBufferSize,
                frameMaxSize, headersMaxSize, streamTimeout, handlerMaxCount);
        if (streamSettings.frameMaxSize > settings.frameMaxSize) {
            throw new CepticIOException(String.format("Server chose frameMaxSize (%d) higher than client's (%d)",
                    streamSettings.frameMaxSize, settings.frameMaxSize));
        }
        if (streamSettings.headersMaxSize > settings.headersMaxSize) {
            throw new CepticIOException(String.format("Server chose headersMaxSize (%d) higher than client's (%d)",
                    streamSettings.headersMaxSize, settings.headersMaxSize));
        }
        if (streamSettings.streamTimeout > settings.streamTimeout) {
            throw new CepticIOException(String.format("Server chose streamTimeout (%d) higher than client's (%d)",
                    streamSettings.streamTimeout, settings.streamTimeout));
        }
        // create manager
        StreamManager manager = new StreamManager(socket, UUID.randomUUID(), destination, streamSettings,
                this,false);
        // add and start manager
        addManager(manager);
        manager.start();
        return manager;
    }

    protected void addManager(StreamManager manager) {
        ConcurrentHashMap.KeySetView<UUID,Boolean> managerSet = destinationMap.get(manager.getDestination());
        // if manager set already exists for this destination, add manager to that set
        if (managerSet != null) {
            managerSet.add(manager.getManagerId());
        }
        // otherwise create new set and add to destination map
        else {
            managerSet = ConcurrentHashMap.newKeySet();
            managerSet.add(manager.getManagerId());
            destinationMap.put(manager.getDestination(), managerSet);
        }
        // add manager to map
        managers.put(manager.getManagerId(), manager);
    }

    protected StreamManager getManager(UUID managerId) {
        return managers.get(managerId);
    }

    protected StreamManager getAvailableManagerForDestination(String destination) {
        ConcurrentHashMap.KeySetView<UUID,Boolean> managerSet = destinationMap.get(destination);
        // if manager set exists, try to get first manager that isn't saturated with handlers
        if (managerSet != null) {
            for (UUID managerId : managerSet) {
                StreamManager manager = getManager(managerId);
                if (manager != null) {
                    if (!manager.isFullyStopped() && !manager.isHandlerLimitReached()) {
                        return manager;
                    }
                }
            }
        }
        // otherwise return null
        return null;
    }

    protected boolean isManagerSaturated(UUID managerId) {
        StreamManager manager = getManager(managerId);
        if (manager != null) {
            return manager.isHandlerLimitReached();
        }
        return false;
    }

    @Override
    public StreamManager removeManager(UUID managerId) {
        // remove manager from managers map
        StreamManager manager = managers.remove(managerId);
        if (manager != null) {
            manager.stopRunning("removed by CepticClient");
            // remove managerId from managerSet in destination map
            ConcurrentHashMap.KeySetView<UUID,Boolean> managerSet = destinationMap.get(manager.getDestination());
            if (managerSet != null) {
                managerSet.remove(manager.getManagerId());
            }
        }
        return manager;
    }

    @Override
    public void handleNewConnection(StreamHandler stream) {
        throw new NotImplementedException();
    }

    protected List<StreamManager> removeAllManagers() {
        List<StreamManager> removedManagers = new ArrayList<>();
        for (UUID managerId : managers.keySet()) {
            removedManagers.add(removeManager(managerId));
        }
        return removedManagers;
    }
    //endregion
}
