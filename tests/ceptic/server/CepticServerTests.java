package ceptic.server;

import ceptic.common.CepticResponse;
import ceptic.common.CepticStatusCode;
import ceptic.endpoint.EndpointEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void addRoute() {
        // Arrange
        CepticServer server = createNonSecureServer();
        String command = "get";
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        server.addCommand(command);
        // Act and Assert
        assertDoesNotThrow(() -> server.addRoute(command, endpoint, entry));
    }

}
