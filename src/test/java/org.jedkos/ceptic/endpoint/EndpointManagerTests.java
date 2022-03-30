package org.jedkos.ceptic.endpoint;

import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.jedkos.ceptic.server.ServerSettingsBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

public class EndpointManagerTests {

    private EndpointManager manager;

    private EndpointManager createEndpointManager() {
        return new EndpointManager(new ServerSettingsBuilder().build());
    }

    //region Test Setup
    @Before
    public void beforeEach() {
        manager = createEndpointManager();
    }
    //endregion

    @Test
    public void createManager_Success() {
        // Arrange, Act, and Assert
        assertThatNoException().isThrownBy(() -> new EndpointManager(new ServerSettingsBuilder().build()));
    }

    @Test
    public void addCommand_Success() {
        // Arrange
        String command = "get";
        // Act
        manager.addCommand(command);
        // Assert
        assertThat(manager.commandMap).containsKey(command);
    }

    @Test
    public void getCommand_Success() {
        // Arrange
        String command = "get";
        manager.addCommand("get");
        // Act and Assert
        assertThatNoException().isThrownBy(() -> manager.getCommand(command));
        CommandEntry entry = manager.getCommand(command);
        assertThat(entry).isNotNull();
        assertThat(entry.getCommand()).isEqualTo(command);
    }

    @Test
    public void getCommand_DoesNotExist_IsNull() {
        // Arrange
        manager.addCommand("get");
        // Act
        CommandEntry entry = manager.getCommand("post");
        // Assert
        assertThat(entry).isNull();
    }

    @Test
    public void removeCommand_Success() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        // Act
        CommandEntry entry = manager.removeCommand(command);
        // Assert
        assertThat(entry).isNotNull();
        assertThat(entry.getCommand()).isEqualTo(command);
        assertThat(manager.commandMap).isEmpty();
    }

    @Test
    public void removeCommand_DoesNotExist_IsNull() {
        // Arrange and Act
        CommandEntry entry = manager.removeCommand("get");
        // Assert
        assertThat(entry).isNull();
    }

    @Test
    public void addEndpoint_Success() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        String endpoint = "/";
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // Act and Assert
        assertThatNoException().isThrownBy(() -> manager.addEndpoint(command, endpoint, endpointEntry));
        CommandEntry commandEntry = manager.commandMap.get(command);
        assertThat(commandEntry.endpointMap).isNotEmpty();
        assertThat(commandEntry.endpointMap.values().stream().findFirst().get().getEntry()).isEqualTo(endpointEntry);
    }

    @Test
    public void addEndpoint_CommandDoesNotExist_ThrowsException() {
        // Arrange
        String command = "get";
        String endpoint = "/";
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // Act and Assert
        assertThatThrownBy(() -> manager.addEndpoint(command, endpoint, endpointEntry))
                .isExactlyInstanceOf(EndpointManagerException.class);
    }

    @Test
    public void addEndpoint_GoodEndpoints_NoExceptions() {
        // Arrange
        String command = "get";
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
            manager.addCommand(command);
            assertThatNoException().isThrownBy(() -> manager.addEndpoint(command, endpoint, endpointEntry));
            manager.removeCommand(command);
        }
    }

    @Test
    public void addEndpoint_BadEndpoints_ThrowException() {
        // Arrange
        String command = "get";
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
            manager.addCommand(command);
            assertThatThrownBy(() -> manager.addEndpoint(command, endpoint, endpointEntry))
                    .isExactlyInstanceOf(EndpointManagerException.class);
            manager.removeCommand(command);
        }
    }

    @Test
    public void addEndpoint_EquivalentEndpoints_ThrowException() throws EndpointManagerException {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        // add valid endpoints
        manager.addEndpoint(command, "willalreadyexist", endpointEntry);
        manager.addEndpoint(command, "willalready/<exist>", endpointEntry);
        // create list of endpoints to try
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
            assertThatThrownBy(() -> manager.addEndpoint(command, endpoint, endpointEntry))
                    .isExactlyInstanceOf(EndpointManagerException.class);
        }
    }

    @Test
    public void getEndpoint() throws EndpointManagerException {
        // Arrange
        String command = "get";
        String endpoint = "/";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        manager.addEndpoint(command, endpoint, endpointEntry);

        // Act and Assert
        assertThatNoException().isThrownBy(() -> manager.getEndpoint(command, endpoint));

        EndpointValue endpointValue = manager.getEndpoint(command, endpoint);
        assertThat(endpointValue).isNotNull();
        // variable map should be empty
        assertThat(endpointValue.getValues()).isEmpty();
        // entry should be the same as put in
        assertThat(endpointValue.getEntry()).isEqualTo(endpointEntry);
    }

    @Test
    public void getEndpoint_WithVariables() throws EndpointManagerException {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        Pattern template = Pattern.compile("@");

        List<String> endpointTemplates = new ArrayList<String>() {
            {
                add("test/@");
                add("@/@");
                add("test/@/@");
                add("test/@/other/@");
                add("@/tests/variable0/@");
                add("@/@/@/@/@");
                add("@/@/@/@/@/test");
            }
        };

        // Act
        for (String endpointTemplate : endpointTemplates) {
            int count = 0;
            HashMap<String, String> variableMap = new HashMap<>();
            String endpointQuery = endpointTemplate;
            // replace @ with actual variable names/values
            while (template.matcher(endpointTemplate).find()) {
                String name = String.format("variable%d", count);
                String value = String.format("value%d", count);
                variableMap.put(name, value);
                endpointTemplate = endpointTemplate.replaceFirst(template.pattern(), String.format("<%s>", name));
                endpointQuery = endpointQuery.replaceFirst(template.pattern(), value);
                count++;
            }
            // add endpoint
            manager.addEndpoint(command, endpointTemplate, endpointEntry);
            // get endpoint
            EndpointValue endpointValue = manager.getEndpoint(command, endpointQuery);
            // Assert
            assertThat(endpointValue).isNotNull();
            // returned values should have same count as template variables
            assertThat(endpointValue.getValues().size())
                    .overridingErrorMessage("Variable count did not match that of the template")
                    .isEqualTo(variableMap.size());
            for(Map.Entry<String,String> expectedEntry : variableMap.entrySet()) {
                String variable = expectedEntry.getKey();
                assertThat(endpointValue.getValues())
                        .overridingErrorMessage(String.format("Expected variable '%s' not found", variable))
                        .containsKey(variable);
                String expectedValue = expectedEntry.getValue();
                String actualValue = endpointValue.getValues().get(expectedEntry.getKey());
                assertThat(actualValue)
                        .overridingErrorMessage(String.format("Variable '%s' had value '%s' instead of '%s",
                                variable, actualValue, expectedValue))
                        .isEqualTo(expectedValue);
            }
        }
    }

    @Test
    public void getEndpoint_GoodEndpoints_NoExceptions() {

    }

    @Test
    public void getEndpoint_BadEndpoints_ThrowException() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        // create list of endpoints to try
        List<String> endpoints = new ArrayList<>();
        // endpoint query cannot have conse
    }

    @Test
    public void getEndpoint_DoesNotExist_ThrowException() {
        // Arrange
        String command = "get";
        manager.addCommand(command);
        // create list of endpoints to try
        List<String> endpoints = new ArrayList<>();
        // can begin/end with 0 or many slashes
        endpoints.add("no_slashes_at_all");
        endpoints.add("/only_slash_at_start");
        endpoints.add("only_slash_at_end/");
        endpoints.add("/surrounding_slashes/");
        endpoints.add("////multiple_slashes/////////////////");
        // endpoints can contain regex-life format, causing no issues
        endpoints.add("^[a-zA-Z0-9]+$");

        // Act and Assert
        for (String endpoint : endpoints) {
            assertThatThrownBy(() -> manager.getEndpoint(command, endpoint))
                    .isExactlyInstanceOf(EndpointManagerException.class);
        }
    }

    @Test
    public void getEndpoint_CommandDoesNotExist_ThrowException() {
        // Arrange, Act, and Assert
        assertThatThrownBy(() -> manager.getEndpoint("get", "/"))
                .isExactlyInstanceOf(EndpointManagerException.class);
    }

    @Test
    public void removeEndpoint() throws EndpointManagerException {
        // Arrange
        String command = "get";
        String endpoint = "/";
        EndpointEntry endpointEntry = (request, values) -> new CepticResponse(CepticStatusCode.OK);
        manager.addCommand(command);
        manager.addEndpoint(command, endpoint, endpointEntry);
        // Act and Assert
        EndpointSaved saved = manager.removeEndpoint(command, endpoint);
        assertThat(saved).isNotNull();
        assertThat(saved.getEntry()).isEqualTo(endpointEntry);
    }

}
