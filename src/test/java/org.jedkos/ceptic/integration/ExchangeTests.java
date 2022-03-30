package org.jedkos.ceptic.integration;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.common.CommandType;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.helpers.CepticInitializers;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.stream.StreamData;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.exceptions.StreamClosedException;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;


public class ExchangeTests extends CepticInitializers {

    private CepticServer server;
    private CepticClient client;

    @After
    public void afterEach() {
        if (server != null) {
            server.stopRunning();
            server = null;
        }
        if (client != null) {
            client.stop();
            client.stop();
        }
    }

    @Test
    public void exchange_Unsecure_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> {
            StreamHandler stream = request.beginExchange();
            if (stream == null)
                return new CepticResponse(CepticStatusCode.UNEXPECTED_END);
            return new CepticResponse(CepticStatusCode.EXCHANGE_END);
        };

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/");
        request.setExchange(true);
        // Act, Assert
        server.start();
        CepticResponse response = client.connect(request);

        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.EXCHANGE_START);
        assertThat(response.getExchange()).isTrue();
        assertThat(response.getStream()).isNotNull();

        StreamHandler stream = response.getStream();
        StreamData data = stream.readData(200);
        assertThat(data.isResponse()).isTrue();
        response = data.getResponse();
        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.EXCHANGE_END);
        // sleep a little bit to make sure close frame is received by client before checking if stream is stopped
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) { }

        assertThat(stream.isStopped()).isTrue();
        assertThatThrownBy(() -> stream.readData(200)).isExactlyInstanceOf(StreamClosedException.class);
    }

}
