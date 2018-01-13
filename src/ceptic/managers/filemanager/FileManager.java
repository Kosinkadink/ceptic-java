package ceptic.managers.filemanager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class FileManager {

    private String location;
    private HashMap<String, String> locations = new HashMap<>();

    FileManager(String location, boolean createImmediately) throws IOException {
        this.location = location;
        initializeDirectoriesMap();
        // create directories in map if specified to do so
        if (createImmediately) {
            createDirectories();
        }
    }

    // add all default directories to locations dictionary
    private void initializeDirectoriesMap() {
        locations.put("root", location);
        locations.put("resources", Paths.get(location,"resources/").toString());
        locations.put("protocols", Paths.get(locations.get("resources"), "protocols/").toString());
        locations.put("cache", Paths.get(locations.get("resources"), "cache/").toString());
        locations.put("programparts", Paths.get(locations.get("resources"), "programparts/").toString());
        locations.put("uploads", Paths.get(locations.get("resources"), "uploads/").toString());
        locations.put("downloads", Paths.get(locations.get("resources"), "downloads/").toString());
        locations.put("networkpass", Paths.get(locations.get("resources"), "networkpass/").toString());
        locations.put("certification", Paths.get(locations.get("resources"), "certification/").toString());
    }

    private void initializeNetPass() throws IOException {
        addFile("netpassfile", "default.txt", "networkpass", "");
    }

    // create all directories currently in locations dictionary
    private void createDirectories() throws IOException {
        for(HashMap.Entry<String, String> entry: locations.entrySet()) {
            String directory = entry.getValue();
            Files.createDirectories(Paths.get(directory));
        }
    }

    // Add more CEPtic implementation directory bindings and create them, with no base key
    public void addDirectory(String key, String location) throws IOException {
        addDirectory(key, location, null);
    }

    // Add more CEPtic implementation directory bindings and create them
    public void addDirectory(String key, String location, String baseKey) throws IOException {
        String actualLocation = getActualLocation(key, location, baseKey);
        // create directory
        Files.createDirectories(Paths.get(actualLocation));
        // add to dictionary
        locations.put(key, actualLocation);
    }

    // Add file location bindings to CEPtic implementation directory bindings, creating file if does not exist
    // In this case, no base key and no text is provided
    public void addFile(String key, String location) throws IOException {
        addFile(key, location, null, "");
    }

    // Add file location bindings to CEPtic implementation directory bindings, creating file if does not exist
    // In this case, no base key is provided
    public void addFile(String key, String location, String text) throws IOException {
        addFile(key, location, null, text);
    }

    // Add file location bindings to CEPtic implementation directory bindings, creating file if does not exist
    public void addFile(String key, String location, String baseKey, String text) throws IOException {
        String actualLocation = getActualLocation(key, location, baseKey);
        // if file does not exist, create it
        if (!Files.exists(Paths.get(actualLocation))) {
            // if no text, just create the file
            if (!text.isEmpty()) {
                Files.createFile(Paths.get(actualLocation));
            }
            // otherwise, create and write to file
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter(actualLocation));
                writer.write(text);
                writer.close();
            }
        }
        // add file location to dictionary
        locations.put(key, actualLocation);
    }

    // Return directory from dictionary, if exists
    public String getDirectory(String key) {
        return locations.getOrDefault(key, null);
    }

    public String getNetPass() {
        String filename = "default.txt";
        return getNetPass(filename);
    }

    public String getNetPass(String filename) {
        String netPass = null;
        try {
            InputStream fstream = new FileInputStream(Paths.get(locations.get("netpass"),filename).toString());
            InputStreamReader isr = new InputStreamReader(fstream);
            BufferedReader br = new BufferedReader(isr);
            netPass = br.readLine();
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: netpass.txt does not exist");
        } catch (IOException e) {
            System.out.println("ERROR: could not read netpass from file");
        }
        return netPass;
    }

    // Get actual location of file/directory
    // This function ensures that the returned location IS within the resources directory
    private String getActualLocation(String key, String location, String baseKey) {
        // if baseKey provided and is in locations dictionary, get location = baseKey + location
        if (baseKey != null && locations.containsKey(baseKey) ) {
            return Paths.get(locations.get(baseKey), location).toString();
        }
        // if no baseKey
        else {
            // if location does not start with resources, location = resources + location
            if (!location.startsWith(locations.get("resources"))) {
                return Paths.get(locations.get("resources"), location).toString();
            }
        }
        // otherwise, just return the location
        return location;
    }

}
