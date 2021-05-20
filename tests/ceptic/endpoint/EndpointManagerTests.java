package ceptic.endpoint;

import ceptic.common.CepticRequest;
import ceptic.common.CepticResponse;
import ceptic.common.CepticStatusCode;
import ceptic.endpoint.exceptions.EndpointManagerException;
import ceptic.server.ServerSettingsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointManagerTests {

    private EndpointManager manager;

    private EndpointManager createEndpointManager() {
        return new EndpointManager(new ServerSettingsBuilder().build());
    }

    //region Test Setup
    @BeforeEach
    void beforeEach() {
        manager = createEndpointManager();
    }
    //endregion

    @Test
    void createManager_Success() {
        // Arrange, Act, and Assert
        assertDoesNotThrow(() -> new EndpointManager(new ServerSettingsBuilder().build()));
    }

    @Test
    void addCommand_Success() {
        // Arrange
        String command = "get";
        // Act
        manager.addCommand(command);
        // Assert
        assertTrue(manager.commandMap.containsKey(command));
    }

    @Test
    void getCommand_Success() {
        // Arrange
        String command = "get";
        manager.addCommand("get");
        // Act and Assert
        assertDoesNotThrow(() -> manager.getCommand(command), "Exception thrown when not expected");
        CommandEntry entry = manager.getCommand(command);
        assertNotNull(entry, "CommandEntry is not supposed to be null from manager.getCommand");
        assertEquals(command, entry.getCommand(), "Command was not as expected");
    }

    @Test
    void getCommand_DoesNotExist_IsNull() {
        // Arrange
        manager.addCommand("get");
        // Act
        CommandEntry entry  = manager.getCommand("post");
        // Assert
        assertNull(entry, "CommandEntry is supposed to be null from manager.getCommand");
    }

    @Test
    void removeCommand_Success() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        // Act
        CommandEntry entry = manager.removeCommand(command);
        // Assert
        assertNotNull(entry, "CommandEntry is not supposed to be null from manager.removeCommand");
        assertEquals(command, entry.getCommand(), "Command was not as expected");
        assertTrue(manager.commandMap.isEmpty(), "Manager's commandMap was not empty");
    }

    @Test
    void removeCommand_DoesNotExist_IsNull() {
        // Arrange and Act
        CommandEntry entry = manager.removeCommand("get");
        // Assert
        assertNull(entry, "CommandEntry is supposed to be null from manager.removeCommand");
    }

    @Test
    void addEndpoint_Success() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        String endpoint = "/";
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // Act and Assert
        assertDoesNotThrow(() -> manager.addEndpoint(command, endpoint, endpointEntry));
        CommandEntry commandEntry = manager.commandMap.get(command);
        assertFalse(commandEntry.endpointMap.isEmpty(), "EndpointMap should not be empty");
        assertEquals(endpointEntry, commandEntry.endpointMap.values().stream().findFirst().get().getEntry(),
                "EndpointEntry not in endpointMap");
    }

    @Test
    void addEndpoint_CommandDoesNotExist_ThrowsException() {
        // Arrange
        String command = "get";
        String endpoint = "/";
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // Act and Assert
        assertThrows(EndpointManagerException.class, () -> manager.addEndpoint(command, endpoint, endpointEntry));
    }

    @Test
    void addEndpoint_GoodEndpoints_NoExceptions() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        List<String> endpoints = new ArrayList<>();
        // endpoint can be a single slash
        endpoints.add("/");
        // endpoint can be composed of any alphanumerics as well as -.<>_/ characters
        // (but <> have to be enclosing something)
        String goodVariableStartCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";
        String goodVariableCharacters = goodVariableStartCharacters + "1234567890";
        String goodCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._";
        for (char character : goodCharacters.toCharArray()) {
            String stringCharacter = String.valueOf(character);
            endpoints.add(stringCharacter);
            endpoints.add(String.format("%s/test", stringCharacter));
            endpoints.add(String.format("test/%s", stringCharacter));
        }
        // endpoint can contain braces for variable portion or URL; they have to enclose something
        endpoints.add("good/<braces>");
        endpoints.add("<braces>");
        endpoints.add("<good>/braces");
        // variables can start with alphabet and underscore, non-first characters can be alphanumerics and underscore
        for (char character : goodVariableStartCharacters.toCharArray()) {
            String stringCharacter = String.valueOf(character);
            endpoints.add(String.format("<%s>", stringCharacter));
            for (char otherCharacter : goodVariableCharacters.toCharArray()) {
                String stringOtherCharacter = String.valueOf(otherCharacter);
                endpoints.add(String.format("<%s>", stringCharacter + stringOtherCharacter));
            }
        }
        // variable name can start with underscore
        endpoints.add("<_underscore>/in/variable/name");
        // multiple variables allowed separated by slashes
        endpoints.add("<multiple>/<variables>");
        // endpoint can start or end with multiple (or no) slashes
        endpoints.add("no_slashes_at_all");
        endpoints.add("/only_slash_at_start");
        endpoints.add("only_slash_at_end/");
        endpoints.add("/surrounding_slashes/");
        endpoints.add("////multiple_slashes/////////////////");

        // Act and Assert
        for (String endpoint : endpoints) {
            assertDoesNotThrow(() -> manager.addEndpoint(command, endpoint, endpointEntry),
                    String.format("Exception thrown for endpoint '%s'", endpoint));
        }
    }

    @Test
    void addEndpoint_BadEndpoints_ThrowException() throws EndpointManagerException {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        List<String> endpoints = new ArrayList<>();
        // endpoint cannot be blank
        endpoints.add("");
        // non-alpha numeric or non -.<>_/ symbols are not allowed
        String badCharacters = "!@#$%^&*()=+`~[}{]\\|;:\"', ";
        for (char character : badCharacters.toCharArray()) {
            String stringCharacter = String.valueOf(character);
            endpoints.add(stringCharacter);
            endpoints.add(String.format("%s/test", stringCharacter));
            endpoints.add(String.format("test/%s", stringCharacter));
        }
        // consecutive slashes in the middle are not allowed
        endpoints.add("bad//endpoint");
        endpoints.add("/bad/endpoint//2/");
        // braces cannot be across a slash
        endpoints.add("bad/<bra/ces>");
        // braces cannot have nothing in between
        endpoints.add("bad/<>/braces");
        // braces must close
        endpoints.add("unmatched/<braces");
        endpoints.add("unmatched/<braces>>");
        endpoints.add("unmatched/<braces>/other>");
        endpoints.add(">braces");
        endpoints.add("braces<");
        // braces cannot contain other braces
        endpoints.add("unmatched/<<braces>>");
        endpoints.add("unmatched/<b<race>s>");
        // braces cannot be placed directly adjacent to each other
        endpoints.add("multiple/<unslashed><braces>");
        // braces cannot be placed more than once between slashes
        endpoints.add("multiple/<braces>.<between>");
        endpoints.add("multiple/<braces>.<between>/slashes");
        endpoints.add("<bad>bad<braces>");
        // variable name in braces cannot start with a number
        endpoints.add("starts/<1withnumber>");
        // multiple variables cannot have the same name
        endpoints.add("<variable>/<variable>");

        // Act and Assert
        for (String endpoint : endpoints) {
            assertThrows(EndpointManagerException.class, () -> manager.addEndpoint(command, endpoint, endpointEntry),
                    String.format("Exception not thrown for invalid endpoint '%s'", endpoint));
        }
    }

    @Test
    void addEndpoint_EquivalentEndpoints_ThrowException() throws EndpointManagerException {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // add valid endpoints
        manager.addEndpoint(command, "willalreadyexist", endpointEntry);
        manager.addEndpoint(command, "willalready/<exist>", endpointEntry);
        // create lost of endpoints to try
        List<String> endpoints = new ArrayList<>();
        // endpoint cannot already exist; slash at beginning or end makes no difference
        endpoints.add("willalreadyexist");
        endpoints.add("/willalreadyexist");
        endpoints.add("willalreadyexist/");
        endpoints.add("///willalreadyexist/////");
        // equivalent variable format is also not allowed
        endpoints.add("willalready/<exist>");
        endpoints.add("willalready/<exist1>");

        // Act and Assert
        for (String endpoint : endpoints) {
            assertThrows(EndpointManagerException.class, () -> manager.addEndpoint(command, endpoint, endpointEntry),
                    String.format("Exception not thrown for duplicate endpoint '%s'", endpoint));
        }
    }

}
