package org.jedkos.ceptic.helpers;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.client.CepticClientBuilder;
import org.jedkos.ceptic.client.ClientSettings;
import org.jedkos.ceptic.client.ClientSettingsBuilder;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.server.CepticServerBuilder;
import org.jedkos.ceptic.server.ServerSettings;
import org.jedkos.ceptic.server.ServerSettingsBuilder;

public class CepticInitializers {

    //region
    public static CepticClient createUnsecureClient() {
        return createUnsecureClient(null);
    }

    public static CepticClient createUnsecureClient(ClientSettings settings) {
        if (settings == null)
            settings = new ClientSettingsBuilder().build();
        return new CepticClientBuilder().settings(settings).secure(false).build();
    }
    //endregion

    //region createUnsecureServer
    public static CepticServer createUnsecureServer() {
        return createUnsecureServer(null, true);
    }

    public static CepticServer createUnsecureServer(boolean verbose) {
        return createUnsecureServer(null, verbose);
    }

    public static CepticServer createUnsecureServer(ServerSettings settings) {
        return createUnsecureServer(settings, true);
    }

    public static CepticServer createUnsecureServer(ServerSettings settings, boolean verbose) {
        if (settings == null)
            settings = new ServerSettingsBuilder().verbose(verbose).build();
        return new CepticServerBuilder().settings(settings).secure(false).build();
    }
    //endregion

}
