package integration;

import ceptic.client.CepticClient;
import ceptic.client.CepticClientBuilder;
import ceptic.common.CepticRequest;
import ceptic.common.CepticResponse;
import ceptic.common.CepticStatusCode;
import ceptic.common.CommandType;
import ceptic.common.exceptions.CepticException;
import ceptic.endpoint.EndpointEntry;
import ceptic.server.CepticServer;
import ceptic.server.CepticServerBuilder;
import ceptic.server.ServerSettingsBuilder;
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
