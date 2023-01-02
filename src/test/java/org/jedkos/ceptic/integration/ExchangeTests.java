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

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

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
            client = null;
        }
    }

    //region Unsecure Server + Client
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
        // sleep a little to make sure close frame is received by client before checking if stream is stopped
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) { }

        assertThat(stream.isStopped()).isTrue();
        assertThatThrownBy(() -> stream.readData(200)).isExactlyInstanceOf(StreamClosedException.class);
    }

    @Test
    public void exchange_Echo1000_Unsecure_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> {
            StreamHandler stream = request.beginExchange();
            if (stream == null)
                return new CepticResponse(CepticStatusCode.UNEXPECTED_END);
            try {
                while(true) {
                    StreamData data = stream.readData(1000);
                    if (!data.isData())
                        break;
                    if (stream.getSettings().verbose)
                        System.out.printf("Received data: %s%n", new String(data.getData(), StandardCharsets.UTF_8));
                    stream.sendData(data.getData());
                }
            }
            catch (StreamException e) {
                if (stream.getSettings().verbose)
                    System.out.printf("StreamException in Endpoint: %s,%s%n", e.getClass().toString(),e.getMessage());
                return new CepticResponse(CepticStatusCode.UNEXPECTED_END);
            }
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
        assertThat(response.getStream().isStopped()).isFalse();

        StreamHandler stream = response.getStream();

        for (int i=0; i < 1000; i++) {
            byte[] expectedData = MessageFormat.format("echo{0}", i).getBytes(StandardCharsets.UTF_8);
            stream.sendData(expectedData);
            StreamData data = stream.readData(1000);
            assertThat(data.isData()).isTrue();
            assertThat(data.getData()).isEqualTo(expectedData);
        }
        stream.sendResponse(new CepticResponse(CepticStatusCode.OK));
        StreamData lastData = stream.readData(1000);
        assertThat(lastData.isResponse()).isTrue();
        assertThat(lastData.getResponse().getStatusCode()).isEqualTo(CepticStatusCode.EXCHANGE_END);
        // sleep a little to make sure close frame is received by client before checking if stream is stopped
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) { }

        assertThat(stream.isStopped()).isTrue();
        assertThatThrownBy(() -> stream.readData(200)).isExactlyInstanceOf(StreamClosedException.class);
    }
    //endregion

    //region
    @Test
    public void exchange_Secure_Success() throws CepticException {
        // Arrange
        server = createSecureServer(true);
        client = createSecureClient();

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
        // sleep a little to make sure close frame is received by client before checking if stream is stopped
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) { }

        assertThat(stream.isStopped()).isTrue();
        assertThatThrownBy(() -> stream.readData(200)).isExactlyInstanceOf(StreamClosedException.class);
    }

    @Test
    public void exchange_Echo1000_Secure_Success() throws CepticException {
        // Arrange
        server = createSecureServer(true);
        client = createSecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> {
            StreamHandler stream = request.beginExchange();
            if (stream == null)
                return new CepticResponse(CepticStatusCode.UNEXPECTED_END);
            try {
                while(true) {
                    StreamData data = stream.readData(1000);
                    if (!data.isData())
                        break;
                    if (stream.getSettings().verbose)
                        System.out.printf("Received data: %s%n", new String(data.getData(), StandardCharsets.UTF_8));
                    stream.sendData(data.getData());
                }
            }
            catch (StreamException e) {
                if (stream.getSettings().verbose)
                    System.out.printf("StreamException in Endpoint: %s,%s%n", e.getClass().toString(),e.getMessage());
                return new CepticResponse(CepticStatusCode.UNEXPECTED_END);
            }
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
        assertThat(response.getStream().isStopped()).isFalse();

        StreamHandler stream = response.getStream();

        for (int i=0; i < 1000; i++) {
            byte[] expectedData = MessageFormat.format("echo{0}", i).getBytes(StandardCharsets.UTF_8);
            stream.sendData(expectedData);
            StreamData data = stream.readData(1000);
            assertThat(data.isData()).isTrue();
            assertThat(data.getData()).isEqualTo(expectedData);
        }
        stream.sendResponse(new CepticResponse(CepticStatusCode.OK));
        StreamData lastData = stream.readData(1000);
        assertThat(lastData.isResponse()).isTrue();
        assertThat(lastData.getResponse().getStatusCode()).isEqualTo(CepticStatusCode.EXCHANGE_END);
        // sleep a little to make sure close frame is received by client before checking if stream is stopped
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) { }

        assertThat(stream.isStopped()).isTrue();
        assertThatThrownBy(() -> stream.readData(200)).isExactlyInstanceOf(StreamClosedException.class);
    }

    @Test
    public void exchange_NoExchangeHeader_Secure_MissingExchange() throws CepticException {
        // Arrange
        server = createSecureServer(true);
        client = createSecureClient();

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

        // no Exchange header will be included on this
        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/");
        // Act, Assert
        server.start();
        CepticResponse response = client.connect(request);

        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.MISSING_EXCHANGE);
        assertThat(response.getExchange()).isFalse();
        assertThat(response.getStream()).isNotNull();
        assertThat(response.getStream().isStopped()).isTrue();
    }
    //endregion

}
