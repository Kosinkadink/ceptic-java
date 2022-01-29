package org.jedkos.ceptic.integration;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.client.CepticClientBuilder;
import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.common.CommandType;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.server.CepticServerBuilder;
import org.jedkos.ceptic.server.ServerSettingsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationCommandTests {

    private CepticServer _server;

    private CepticServer createNonSecureServer() {
        return new CepticServerBuilder()
                .secure(false)
                .build();
    }

    private CepticClient createUnsecureClient() {
        return new CepticClientBuilder()
                .secure(false)
                .build();
    }

    @AfterEach
    void afterEach() {
        if (_server != null) {
            _server.stopRunning();
            _server = null;
        }
    }

    @Test
    void basicCommand_Unsecure() throws CepticException {
        // Arrange
        CepticClient client = new CepticClientBuilder()
                .secure(false)
                .build();

        CepticServer server = new CepticServerBuilder()
                .secure(false)
                .settings(new ServerSettingsBuilder().verbose(true).build())
                .build();
        _server = server;

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/");
        // Act
        server.start();
        CepticResponse response = client.connect(request);
        // Assert
        assertEquals(CepticStatusCode.OK, response.getStatusCode(), String.format("Expected %d, but was %d: %s",
                CepticStatusCode.OK.getValue(), response.getStatusCode().getValue(), response.getErrors()));
        assertEquals(0, response.getBody().length,
                String.format("Body should have been empty but had length %d", response.getBody().length));
    }

}
