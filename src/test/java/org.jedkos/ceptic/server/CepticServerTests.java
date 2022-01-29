package org.jedkos.ceptic.server;

import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.endpoint.EndpointValue;
import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CepticServerTests {

    private CepticServer createNonSecureServer() {
        return new CepticServerBuilder()
                .secure(false)
                .build();
    }

    @Test
    void createServer_Unsecure() {
        // Arrange
        CepticServer server = createNonSecureServer();
        // Act and Assert
        server.start();
        server.stopRunning();
    }

    @Test
    void addCommand() {
        // Arrange
        CepticServer server = createNonSecureServer();
        String command = "get";
        // Act
        server.addCommand(command);
        // Assert
        assertNotNull(server.endpointManager.getCommand("get"),
                "Command not added to server's endpointManager");
    }

    @Test
    void addRoute() throws EndpointManagerException {
        // Arrange
        CepticServer server = createNonSecureServer();
        String command = "get";
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        server.addCommand(command);
        // Act and Assert
        assertDoesNotThrow(() -> server.addRoute(command, endpoint, entry), "Exception thrown for addRoute");
        assertDoesNotThrow(() -> server.endpointManager.getEndpoint(command, endpoint),
                "Endpoint erroneously does not exist");
        EndpointValue value = server.endpointManager.getEndpoint(command, endpoint);
        assertTrue(value.getValues().isEmpty(), "Variables were present when none expected for endpoint");
    }

}
