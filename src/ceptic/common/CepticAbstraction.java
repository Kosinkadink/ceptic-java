package ceptic.common;

import ceptic.managers.endpointmanager.EndpointManager;
import ceptic.managers.filemanager.FileManager;
import ceptic.managers.filemanager.FileManagerBuilder;
import ceptic.managers.filemanager.FileManagerException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class CepticAbstraction {

    protected HashMap<String, Object> varDict = new HashMap<>();
    protected String location;
    protected FileManager fileManager;
    protected EndpointManager endpointManager;

    public CepticAbstraction(String location) throws FileManagerException {
        this.location = location;
        // put send_cache variable into dictionary
        varDict.put("send_cache", 409600);
        // initialize file manager
        try {
            fileManager = new FileManagerBuilder()
                    .location(this.location)
                    .buildManager();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // create endpoint manager
        endpointManager = createEndpointManager();
    }

    // Return an endpoint manager
    public abstract EndpointManager createEndpointManager();

    /* Override to insert application-specific endpoint commands */
    protected void addEndpointCommands() {}

    protected int getCacheSize() {
        return (int)varDict.get("send_cache");
    }

    protected void clear() {}

}
