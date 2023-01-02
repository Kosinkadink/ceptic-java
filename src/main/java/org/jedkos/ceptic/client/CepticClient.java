package org.jedkos.ceptic.client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jedkos.ceptic.common.*;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.common.exceptions.CepticIOException;
import org.jedkos.ceptic.encode.exceptions.UnknownEncodingException;
import org.jedkos.ceptic.net.SocketCeptic;
import org.jedkos.ceptic.net.exceptions.SocketCepticException;
import org.jedkos.ceptic.security.CertificateHelper;
import org.jedkos.ceptic.security.SecuritySettings;
import org.jedkos.ceptic.security.exceptions.SecurityException;
import org.jedkos.ceptic.stream.StreamData;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.StreamManager;
import org.jedkos.ceptic.stream.StreamSettings;
import org.jedkos.ceptic.stream.exceptions.StreamException;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CepticClient implements RemovableManagers {

    protected final ClientSettings settings;
    protected final SecuritySettings security;
    protected SSLContext sslContext = null;

    private final ConcurrentHashMap<UUID, StreamManager> managers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<UUID,Boolean>> destinationMap = new ConcurrentHashMap<>();

    public CepticClient() throws SecurityException {
        this(null, null);
    }

    public CepticClient(SecuritySettings security) throws SecurityException {
        this(null, security);
    }

    public CepticClient(ClientSettings settings, SecuritySettings security) throws SecurityException {
        this.settings = (settings != null) ? settings : new ClientSettingsBuilder().build();
        this.security = (security != null) ? security : SecuritySettings.Client();
        SetupSecurity();
    }

    //region Getters
    public ClientSettings getSettings() {
        return settings;
    }

    public boolean isVerifyRemote() {
        return security.isVerifyRemote();
    }

    public boolean isSecure() {
        return security.isSecure();
    }
    //endregion

    //region Security
    protected void SetupSecurity() throws SecurityException {
        if (security.isSecure()) {
            KeyManager[] km = null;
            TrustManager[] tm = null;
            // if LocalCert is present, attempt to load client cert and key
            if (security.getLocalCert() != null) {
                // if no LocalKey, then assume LocalCert combines both certificate and key
                if (security.getLocalKey() == null) {
                    // try to load client certificate + key from combined file
                    km = CertificateHelper.generateFromCombined(security.getLocalCert(), security.getKeyPassword()).getKeyManagers();
                }
                // otherwise, assume LocalCert contains certificate and LocalKey contains key
                else {
                    // try to load client certificate + key from separate files
                    km = CertificateHelper.generateFromSeparate(security.getLocalCert(), security.getLocalKey(), security.getKeyPassword()).getKeyManagers();
                }
            }

            // if RemoteCert is present, attempt to load server cert
            if (security.getRemoteCert() != null) {
                tm = CertificateHelper.loadTrustManager(security.getRemoteCert()).getTrustManagers();
            }

            try {
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(km, tm, new SecureRandom());
                sslContext = context;
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new SecurityException(MessageFormat.format("Error creating SSLContext: {0}", e), e);
            }
        }
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
            // use SSLSocket if sslContext exists
            if (sslContext != null) {
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                rawSocket = sslSocketFactory.createSocket(request.getHost(), request.getPort());

            }
            else {
                rawSocket = new Socket(request.getHost(), request.getPort());
            }
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
        throw new UnsupportedOperationException();
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
