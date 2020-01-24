package ceptic.client;

import ceptic.common.CepticAbstraction;
import ceptic.common.CepticException;
import ceptic.managers.certificatemanager.CertificateManager;
import ceptic.managers.certificatemanager.CertificateManagerBuilder;
import ceptic.managers.endpointmanager.EndpointCommandClient;
import ceptic.managers.endpointmanager.EndpointManager;
import ceptic.managers.endpointmanager.EndpointManagerBuilder;
import ceptic.net.SocketCeptic;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CepticClientOld {

    private String netPass;
    private CertificateManager certificateManager;
    private boolean startTerminal;
    // Runtime variable
    private boolean shouldExit;

    public CepticClientOld(String location, boolean startTerminal, String name, String version, boolean clientVerify) throws CepticException {
        // initialize CepticAbstraction
        super(location);
        this.startTerminal = startTerminal;
        shouldExit = false;
        // set varDict arguments
        varDict.put("scriptname", name);
        varDict.put("version", version);
        // set up endpoints
        endpointManager.addCommand("ping", new EndpointCommandClient() {
            @Override
            public Map function(SocketCeptic s, Object data, Object dataToStore) {
                return pingEndpoint(s, data, dataToStore);
            }
        });
        addEndpointCommands();
        // create certificate manager
        certificateManager = new CertificateManagerBuilder()
                .clientVerify(clientVerify)
                .fileManager(fileManager)
                .buildClientManager();

    }

    @Override
    public EndpointManager createEndpointManager() {
        return new EndpointManagerBuilder().buildClientManager();
    }

    private void initialize() {
        // perform all tasks
        initSpecExtra();
        netPass = fileManager.getNetPass();
        certificateManager.generateContext();
        runProcesses();
    }

    private void runProcesses() {
        if (startTerminal) {

        }
    }

    // Override this function for custom behavior
    private void initSpecExtra() {

    }

    private Map pingEndpoint(SocketCeptic s, Object data, Object dataToStore) {
        return null;
    }

    private Map boot() {
        return null;
    }

    private Map help() {
        return null;
    }

    // Runs cleanProcesses() and sets shouldExit to true
    private Map exit() {
        cleanProcesses();
        shouldExit = true;
        return new HashMap<String, Object>();
    }

    // Override this function to do any cleanup on custom processes
    private void cleanProcesses() {

    }

}
