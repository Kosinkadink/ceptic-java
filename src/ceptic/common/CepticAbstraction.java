package ceptic.common;

import ceptic.managers.endpointmanager.EndpointManager;
import ceptic.managers.filemanager.FileManager;
import ceptic.managers.filemanager.FileManagerBuilder;
import ceptic.managers.terminalmanager.TerminalManager;
import ceptic.managers.filemanager.FileManagerException;

import java.io.IOException;
import java.util.HashMap;

public abstract class CepticAbstraction {

    protected HashMap<String, Object> varDict = new HashMap<>();
    private String location;
    protected FileManager fileManager;
    protected TerminalManager terminalManager;
    protected EndpointManager endpointManager;

    CepticAbstraction(String location) throws FileManagerException {
        this.location = location;
        // put send_cache variable into dictionary
        varDict.put("send_cache", 409600);
        // initialize file manager
        try {
            fileManager = new FileManagerBuilder().location(this.location).buildManager();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // initialize terminal manager
        terminalManager = new TerminalManager();
        // create endpoint manager
        endpointManager = createEndpointManager();
    }

    // Return an endpoint manager
    abstract EndpointManager createEndpointManager();

    /* Override to insert application-specific terminal commands */
    void addTerminalCommands() {}
    /* Override to insert application-specific endpoint commands */
    void addEndpointCommands() {}

    /* Attempts to interpret input through the terminal manager */
    void serviceTerminal() {}
    int getCacheSize() {
        return (int)varDict.get("send_cache");
    }
    void clear() {}

}