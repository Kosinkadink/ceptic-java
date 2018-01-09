package ceptic.managers.filemanager;

import java.io.IOException;

public class FileManagerBuilder {

    private String _location;
    private boolean _create = true; // default value

    public FileManagerBuilder() { }

    public FileManager buildManager() throws FileManagerException, IOException {
        if (_location.isEmpty()) {
            throw new FileManagerException("location must be specified");
        }
        return new FileManager(_location, _create);
    }

    public FileManagerBuilder location(String _location) {
        this._location = _location;
        return this;
    }

    public FileManagerBuilder create(boolean _create) {
        this._create = _create;
        return this;
    }

}
