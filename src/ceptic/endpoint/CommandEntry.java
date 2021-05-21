package ceptic.endpoint;

import ceptic.common.RegexHelper;
import ceptic.endpoint.exceptions.EndpointManagerException;
import ceptic.server.ServerSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandEntry {

    private final String command;
    protected final ConcurrentHashMap<EndpointPattern, EndpointSaved> endpointMap = new ConcurrentHashMap<>();
    protected final CommandSettings settings;

    //region Regex
    private static final Pattern allowedRegex = Pattern.compile("^[!-\\[\\]-~]+$"); // ! to [ and ] to ~ ascii characters
    private static final Pattern startSlashRegex = Pattern.compile("^/{2,}"); // 2 or more slashes at start
    private static final Pattern endSlashRegex = Pattern.compile("/+$"); // slashes at the end
    private static final Pattern middleSlashRegex = Pattern.compile("/{2,}"); // 2 or more slashes next to each other

    // alphanumerical and -.<>_/
    private static final Pattern allowedRegexConvert = Pattern.compile("^[a-zA-Z0-9\\-.<>_/]+$");
    // varied portion of endpoint - cannot start with number, only letters and _
    private static final Pattern variableRegex = Pattern.compile("^[a-zA-Z_]+[a-zA-Z0-9_]*$");
    // non-matching braces, no content between braces, open brace at end, slash between braces, multiple braces without slash,
    // or characters between slash and outside of braces
    private static final Pattern badBracesRegex = Pattern.compile(
            "<[^>]*<|>[^<]*>|<[^>]+$|^[^<]+>|<>|<$|<([^/][^>]*/[^/][^>]*)+>|><|>[^/]+|/[^/]+< ");
    private static final Pattern bracesRegex = Pattern.compile("<([^>]*)>"); // find variables in endpoint
    private static final String replacementRegexString = "([!-\\.0-~]+)";
    //endregion

    public CommandEntry(String command, CommandSettings settings) {
        this.command = command;
        this.settings = settings;
    }

    public CommandEntry(String command, ServerSettings serverSettings) {
        this.command = command;
        this.settings = CommandSettings.createWithBodyMax(serverSettings.bodyMax);
    }

    public String getCommand() {
        return command;
    }

    public CommandSettings getSettings() {
        return settings;
    }

    public void addEndpoint(String endpoint, EndpointEntry entry) throws EndpointManagerException {
        // convert endpoint into EndpointPattern
        EndpointPattern endpointPattern = convertEndpointIntoRegex(endpoint);
        // check if endpoint already exists
        if (endpointMap.containsKey(endpointPattern))
            throw new EndpointManagerException(String.format("endpoint '%s' for command '%s' already exists; " +
                    "endpoints for a command must be unique", endpoint, command));
        // put pattern into endpoint map
        endpointMap.put(endpointPattern, new EndpointSaved(entry, endpointPattern.getVariables()));
    }

    public EndpointValue getEndpoint(String endpoint) throws EndpointManagerException {
        // check that endpoint is not empty
        if (endpoint == null || endpoint.isEmpty())
            throw new EndpointManagerException("endpoint cannot be empty");
        // check if using allowed characters
        if (!allowedRegex.matcher(endpoint).find())
            throw new EndpointManagerException(String.format("endpoint '%s' contains invalid characters", endpoint));
        // remove '/' at end of endpoint
        endpoint = endSlashRegex.matcher(endpoint).replaceFirst("");
        // add '/' to start of endpoint if not present
        if (!endpoint.startsWith("/"))
            endpoint = "/" + endpoint;
        // otherwise replace multiple '/' at start with single
        else
            endpoint = startSlashRegex.matcher(endpoint).replaceFirst("/");
        // check if there are multiple slashes in the middle; if so, invalid
        if (middleSlashRegex.matcher(endpoint).find())
            throw new EndpointManagerException("endpoint cannot contain consecutive slashes: " + endpoint);
        // search endpoint map for matching endpoint
        Matcher matcher = null;
        Map.Entry<EndpointPattern,EndpointSaved> match = null;
        for (Map.Entry<EndpointPattern,EndpointSaved> entry : endpointMap.entrySet()) {
            matcher = entry.getKey().getPattern().matcher(endpoint);
            if (matcher.find()) {
                match = entry;
                break;
            }
        }
        // if nothing found, endpoint doesn't exist
        if (match == null) {
            throw new EndpointManagerException(String.format("endpoint '%s' cannot be found for command '%s", endpoint, command));
        }
        // get endpoint variables values from matcher and fill out HashMap
        HashMap<String,String> values = new HashMap<>();
        int index = 1;
        for (String variableName : match.getValue().getVariables()) {
            values.put(variableName, matcher.group(index));
            index++;
        }
        return new EndpointValue(match.getValue().getEntry(), values);
    }

    public EndpointSaved removeEndpoint(String endpoint) {
        try {
            return endpointMap.remove(convertEndpointIntoRegex(endpoint));
        } catch (EndpointManagerException e) {
            return null;
        }
    }

    protected EndpointPattern convertEndpointIntoRegex(String endpoint) throws EndpointManagerException {
        // check that endpoint is not empty
        if (endpoint == null || endpoint.isEmpty())
            throw new EndpointManagerException("endpoint definition cannot be empty");
        // check if using allowed characters
        if (!allowedRegexConvert.matcher(endpoint).find())
            throw new EndpointManagerException(String.format("endpoint definition '%s' contains invalid characters", endpoint));
        // remove '/' at end of endpoint
        endpoint = endSlashRegex.matcher(endpoint).replaceFirst("");
        // add '/' to start of endpoint if not present
        if (!endpoint.startsWith("/"))
            endpoint = "/" + endpoint;
        // add '/' to start of endpoint if not present
        if (!endpoint.startsWith("/"))
            endpoint = "/" + endpoint;
            // otherwise replace multiple '/' at start with single
        else
            endpoint = startSlashRegex.matcher(endpoint).replaceFirst("/");
        // check if there are multiple slashes in the middle; if so, invalid
        if (middleSlashRegex.matcher(endpoint).find())
            throw new EndpointManagerException("endpoint definition cannot contain consecutive slashes: " + endpoint);
        // check if braces are incorrect
        if (badBracesRegex.matcher(endpoint).find())
            throw new EndpointManagerException("endpoint definition contains invalid brace placement");
        // check if variables exist in endpoint, and if so store their names and replace by regex
        Matcher bracesMatcher = bracesRegex.matcher(endpoint);
        // escape unsafe characters in endpoint
        endpoint = RegexHelper.escape(endpoint);
        List<String> variableNames = new ArrayList<>();
        while (bracesMatcher.find()) {
            // check if found variable is valid
            String name = bracesMatcher.group(1);
            if (!variableRegex.matcher(name).find())
                throw new EndpointManagerException(String.format("variable '%s' for endpoint definition '%s' must" +
                        " start with non-numerics and only contain alphanum and underscores", name, endpoint));
            // check if it has a unique name
            if (variableNames.contains(name)) {
                throw new EndpointManagerException(String.format("multiple instances of variable '%s' in endpoint" +
                        " definition '%s'; variable names in an endpoint definition must be unique", name, endpoint));
            }
            // store variable name
            variableNames.add(name);
        }
        // replace variables in endpoint with regex
        for (String variableName : variableNames) {
            // add braces to either side of variable name
            String safeBraces = RegexHelper.escape(String.format("<%s>", variableName), 2);
            // variable contained in braces '<variable>' acts as the string to substitute;
            // regex statement is put in its place for usage when looking up proper endpoint
            endpoint = Pattern.compile(safeBraces).matcher(endpoint).replaceFirst(replacementRegexString);
        }
        // add regex to make sure beginning and end of string will be included
        endpoint = String.format("^%s$", endpoint);
        // return pattern generated from endpoint
        return new EndpointPattern(Pattern.compile(endpoint), variableNames);
    }
}
