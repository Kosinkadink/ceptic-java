package org.jedkos.ceptic.server;

import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.endpoint.EndpointValue;
import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.jedkos.ceptic.helpers.CepticInitializers;
import org.jedkos.ceptic.security.exceptions.SecurityException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CepticServerTests extends CepticInitializers {

    @Test
    public void createServer_Unsecure() throws SecurityException {
        // Arrange
        CepticServer server = createUnsecureServer();
        // Act and Assert
        server.start();
        server.stopRunning();
    }

    @Test
    public void createServer_Secure() throws SecurityException {
        // Arrange
        CepticServer server = createSecureServer(true);
        // Act and Assert
        server.start();
        server.stopRunning();
    }

    @Test
    public void addCommand() throws SecurityException {
        // Arrange
        CepticServer server = createUnsecureServer();
        String command = "get";
        // Act
        server.addCommand(command);
        // Assert
        assertThat(server.endpointManager.getCommand("get"))
                .overridingErrorMessage("Command not added to server's endpointManager")
                .isNotNull();
    }

    @Test
    public void addRoute() throws EndpointManagerException, SecurityException {
        // Arrange
        CepticServer server = createUnsecureServer();
        String command = "get";
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        server.addCommand(command);
        // Act and Assert
        assertThatNoException().isThrownBy(() -> server.addRoute(command, endpoint, entry));
        assertThatNoException().isThrownBy(() -> server.endpointManager.getEndpoint(command, endpoint));

        EndpointValue value = server.endpointManager.getEndpoint(command, endpoint);
        assertThat(value.getValues())
                .overridingErrorMessage("Variables were present when none expected for endpoint")
                .isEmpty();
    }

}
