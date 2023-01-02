package org.jedkos.ceptic.helpers;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.client.ClientSettings;
import org.jedkos.ceptic.client.ClientSettingsBuilder;
import org.jedkos.ceptic.security.SecuritySettings;
import org.jedkos.ceptic.security.exceptions.SecurityException;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.server.ServerSettings;
import org.jedkos.ceptic.server.ServerSettingsBuilder;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

public class CepticInitializers {

    //region Unsecure
    public static CepticClient createUnsecureClient() throws SecurityException {
        return createUnsecureClient(null);
    }

    public static CepticClient createUnsecureClient(ClientSettings settings) throws SecurityException {
        if (settings == null)
            settings = new ClientSettingsBuilder().build();
        return new CepticClient(settings, SecuritySettings.ClientUnsecure());
    }

    public static CepticServer createUnsecureServer() throws SecurityException {
        return createUnsecureServer(null, true);
    }

    public static CepticServer createUnsecureServer(boolean verbose) throws SecurityException {
        return createUnsecureServer(null, verbose);
    }

    public static CepticServer createUnsecureServer(ServerSettings settings) throws SecurityException {
        return createUnsecureServer(settings, true);
    }

    public static CepticServer createUnsecureServer(ServerSettings settings, boolean verbose) throws SecurityException {
        if (settings == null)
            settings = new ServerSettingsBuilder().verbose(verbose).build();
        return new CepticServer(settings, SecuritySettings.ServerUnsecure());
    }
    //endregion

    //region Secure
    public static CepticServer createSecureServer(boolean verbose) throws SecurityException {
        return createSecureServer(null, verbose);
    }

    public static CepticServer createSecureServer(ServerSettings settings, boolean verbose) throws SecurityException {
        if (settings == null)
            settings = new ServerSettingsBuilder().verbose(verbose).build();
        String localCert = null;
        String localKey = null;
        try {
            localCert = Paths.get(Objects.requireNonNull(CepticInitializers.class.getResource("pem/server_cert.cer")).toURI()).toString();
            localKey = Paths.get(Objects.requireNonNull(CepticInitializers.class.getResource("pem/server_key.key")).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        SecuritySettings security = SecuritySettings.Server(localCert, localKey);
        return new CepticServer(settings, security);
    }

    public static CepticClient createSecureClient() throws SecurityException {
        return createSecureClient(null);
    }

    public static CepticClient createSecureClient(ClientSettings settings) throws SecurityException {
        if (settings == null)
            settings = new ClientSettingsBuilder().build();
        String remoteCert = null;
        try {
            remoteCert = Paths.get(Objects.requireNonNull(CepticInitializers.class.getResource("pem/server_cert.cer")).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        SecuritySettings security = SecuritySettings.Client(remoteCert, true);
        return new CepticClient(settings, security);
    }
    //endregion

}
