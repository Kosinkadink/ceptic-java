package org.jedkos.ceptic.integration;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.common.*;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.helpers.CepticInitializers;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.stream.exceptions.StreamException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationCommandTests {

    private CepticServer server;
    private CepticClient client;

    @AfterEach
    void afterEach() {
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
    void command_Unsecure_Success() throws CepticException {
        // Arrange
        CepticServer server = CepticInitializers.createUnsecureServer();
        CepticClient client = CepticInitializers.createUnsecureClient();

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
        assertFalse(response.getExchange(), "Exchange header was true when expected false");
    }

    @Test
    void command_Unsecure_1000_Success() throws CepticException {
        // Arrange
        CepticServer server = CepticInitializers.createUnsecureServer();
        CepticClient client = CepticInitializers.createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";
        EndpointEntry entry = (request, values) -> {
            System.out.println("Received body: " + new String(request.getBody()));
            return new CepticResponse(CepticStatusCode.OK);
        };

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);
        client.stop();

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
                assertEquals(CepticStatusCode.OK, response.getStatusCode(), String.format("Expected %d, but was %d: %s",
                        CepticStatusCode.OK.getValue(), response.getStatusCode().getValue(), response.getErrors()));
                assertFalse(response.getExchange(), "Exchange header was true when expected false");
            }
            catch (StreamException e) {
                System.out.printf("Error thrown on iteration: %d,%s,%s%n", i, e.getClass().toString(), e.getMessage());
                throw e;
            }
        }
    }

    @Test
    void command_Unsecure_EchoBody_Success() throws CepticException {
        // Arrange
        CepticServer server = CepticInitializers.createUnsecureServer();
        CepticClient client = CepticInitializers.createUnsecureClient();

        String command = CommandType.GET;
        String endpoint = "/";

        byte[] expectedBody = "Hello world!".getBytes(StandardCharsets.UTF_8);

        EndpointEntry entry = (request, values) -> new CepticResponse(CepticStatusCode.OK, request.getBody());

        server.addCommand(command);
        server.addRoute(command, endpoint, entry);

        CepticRequest request = new CepticRequest(CommandType.GET, "localhost/", expectedBody);
        // Act
        server.start();
        CepticResponse response = client.connect(request);
        // Assert
        assertEquals(CepticStatusCode.OK, response.getStatusCode(), String.format("Expected %d, but was %d: %s",
                CepticStatusCode.OK.getValue(), response.getStatusCode().getValue(), response.getErrors()));
        assertArrayEquals(expectedBody, response.getBody());
        assertFalse(response.getExchange(), "Exchange header was true when expected false");

        assertEquals(expectedBody.length, request.getContentLength());
        assertEquals(expectedBody.length, response.getContentLength());
    }

    @Test
    void command_Unsecure_EchoVariables_Success() throws CepticException {
        // Arrange
        CepticServer server = CepticInitializers.createUnsecureServer();
        CepticClient client = CepticInitializers.createUnsecureClient();

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
        assertEquals(CepticStatusCode.OK, response.getStatusCode(), String.format("Expected %d, but was %d: %s",
                CepticStatusCode.OK.getValue(), response.getStatusCode().getValue(), response.getErrors()));
        assertArrayEquals(expectedBody, response.getBody());
        assertFalse(response.getExchange(), "Exchange header was true when expected false");

        assertEquals(expectedBody.length, request.getContentLength());
        assertEquals(expectedBody.length, response.getContentLength());
    }

}
