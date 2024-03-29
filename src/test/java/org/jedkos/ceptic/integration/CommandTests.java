package org.jedkos.ceptic.integration;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.common.*;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.common.exceptions.CepticIOException;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.helpers.CepticInitializers;
import org.jedkos.ceptic.net.exceptions.SocketCepticException;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.junit.After;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class CommandTests extends CepticInitializers {

    private CepticServer server;
    private CepticClient client;

    @After
    public void afterEach() {
        if (server != null) {
            server.stopRunning();
//            try {
//                server.join();
//            } catch (InterruptedException ignored) {
//
//            }
            server = null;
        }
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    //region Unsecure Server + Client
    @Test
    public void command_Unsecure_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

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
        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.OK);
        assertThat(response.getBody()).hasSize(0);
        assertThat(response.getExchange()).isFalse();
    }

    @Test
    public void command_Unsecure_1000_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> {
            System.out.println("Received body: " + new String(request.getBody()));
            return new CepticResponse(CepticStatusCode.OK);
        };

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        // Act
        server.start();
        for (int i = 0; i < 1000; i++) {
            try {
                CepticRequest request = new CepticRequest(command, "localhost/",
                        String.format("%d", i).getBytes(StandardCharsets.UTF_8));
                Stopwatch stopwatch = new Stopwatch();
                stopwatch.start();
                CepticResponse response = client.connect(request);
                stopwatch.stop();
                System.out.println("Connection took " + stopwatch.getTimeDiffMillis() + " ms");
                // Assert
                assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.OK);
                assertThat(response.getExchange()).isFalse();
            }
            catch (StreamException e) {
                System.out.printf("Error thrown on iteration: %d,%s,%s%n", i, e.getClass().toString(), e.getMessage());
                throw e;
            }
        }
    }

    @Test
    public void command_Unsecure_EchoBody_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";

        byte[] expectedBody = "Hello world!".getBytes(StandardCharsets.UTF_8);

        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK, request.getBody());

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/", expectedBody);
        // Act
        server.start();
        int tries = 0;
        int tiresLimit = 3;
        CepticResponse response = null;
        Exception except = null;
        while (tries < tiresLimit) {
            try {
                response = client.connect(request);
                break;
            } catch (CepticIOException e) {
                except = e;
                tries++;
            }
        }
        if (tries >= tiresLimit)
            throw new CepticException(MessageFormat.format("Could not connect after {0} tries: {1}", tries, except));
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.OK);
        assertThat(response.getBody()).isEqualTo(expectedBody);
        assertThat(response.getExchange()).isFalse();

        assertThat(request.getContentLength()).isEqualTo(expectedBody.length);
        assertThat(response.getContentLength()).isEqualTo(expectedBody.length);
    }

    @Test
    public void command_Unsecure_EchoVariables_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createUnsecureClient();

        String command = CommandType.GET;
        String variableName1 = "var1";
        String variableName2 = "var2";
        String registerEndpoint = String.format("<%s>/<%s>", variableName1, variableName2);
        String expectedValue1 = UUID.randomUUID().toString();
        String expectedValue2 = UUID.randomUUID().toString();
        String endpoint = String.format("%s/%s", expectedValue1, expectedValue2);

        byte[] expectedBody = String.format("%s was %s, %s was %s",
                variableName1, expectedValue1, variableName2, expectedValue2).getBytes(StandardCharsets.UTF_8);

        EndpointEntry entry = (request, values) -> {
            String stringResult = String.format("%s was %s, %s was %s",
                    variableName1, values.get(variableName1), variableName2, values.get(variableName2));
            if (request.getStream().getSettings().verbose)
                System.out.println("Sending body: " + stringResult);
            return new CepticResponse(CepticStatusCode.OK, stringResult.getBytes(StandardCharsets.UTF_8));
        };

        server.addCommand(command);
        server.addRoute(command, registerEndpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/"+endpoint, expectedBody);
        // Act
        server.start();
        CepticResponse response = client.connect(request);
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.OK);
        assertThat(response.getBody()).isEqualTo(expectedBody);
        assertThat(response.getExchange()).isFalse();

        assertThat(request.getContentLength()).isEqualTo(expectedBody.length);
        assertThat(response.getContentLength()).isEqualTo(expectedBody.length);
    }
    //endregion

    //region Secure Server + Client
    @Test
    public void command_Secure_Success() throws CepticException {
        // Arrange
        server = createSecureServer(true);
        client = createSecureClient();

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
        assertThat(response.getStatusCode()).isEqualTo(CepticStatusCode.OK);
        assertThat(response.getBody()).hasSize(0);
        assertThat(response.getExchange()).isFalse();
    }
    //endregion

    //region Incompatible Combinations
    @Test
    public void command_SecureServer_UnsecureClient_Success() throws CepticException {
        // Arrange
        server = createSecureServer(true);
        client = createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/");
        // Act
        server.start();
        // should throw exception after timing out and not having parsable size to read
        assertThatThrownBy(() -> client.connect(request)).isExactlyInstanceOf(SocketCepticException.class);
    }

    @Test
    public void command_UnsecureServer_SecureClient_Success() throws CepticException {
        // Arrange
        server = createUnsecureServer();
        client = createSecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK);

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/");
        // Act
        server.start();
        // should throw exception relatively quickly after SSLHandshakeException occurs
        assertThatThrownBy(() -> client.connect(request)).isExactlyInstanceOf(SocketCepticException.class);
    }
    //endregion

}
