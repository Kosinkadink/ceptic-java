package ceptic.managers.certificatemanager;

import ceptic.managers.filemanager.FileManager;

public class CertificateManagerBuilder {

    private FileManager _fileManager = null;
    // fields for storing actual locations to use
    private String _localCert;
    private String _localKey;
    private String _verifyCert;
    private boolean _clientVerify = true;

    // default locations
    String defautlClientCert = "cert_client";
    String defaultClientKey = "key_client";
    String defaultServerCert = "cert_server";
    String defaultServerKey = " key_server";

    public CertificateManagerBuilder() { }

    public CertificateManager buildClientManager() throws CertificateManagerException {
        // To build the client manager,
        // at least set up verifyCert.
        // To use client verification, also set up localCert and localKey

        // if fileManager not defined, throw exception
        if (_fileManager == null) {
            throw new CertificateManagerException("no FileManager was provided");
        }
        // check if at least verifyCert is set
        if (_verifyCert.isEmpty()) {
            throw new CertificateManagerException("no verifyCert provided for client instance");
        }
        // otherwise, good to attempt to return new CertificateManager
        return new CertificateManager(_fileManager, CertificateManager.CLIENT, _localCert, _localKey, _verifyCert);
    }

    public CertificateManager buildServerManager() throws CertificateManagerException {
        // To build the server manager,
        // at least set up localCert and localKey
        // verifyCert IF clients need to be verified

        // if fileManager not defined, throw exception
        if (_fileManager == null) {
            throw new CertificateManagerException("no FileManager was provided");
        }
        // check if localCert AND localKey are present
        if (_localCert.isEmpty() || _localKey.isEmpty()) {
            throw new CertificateManagerException("no localCert and/or no localKey provided for server instance");
        }
        // otherwise, good to attempt to return new CertificateManager
        return new CertificateManager(_fileManager, CertificateManager.SERVER, _localCert, _localKey, _verifyCert);
    }

    public CertificateManagerBuilder fileManager(FileManager _fileManager) {
        this._fileManager = _fileManager;
        return this;
    }

    public CertificateManagerBuilder localCert(String _localCert) {
        this._localCert = _localCert;
        return this;
    }

    public CertificateManagerBuilder localKey(String _localKey) {
        this._localKey = _localKey;
        return this;
    }

    public CertificateManagerBuilder verifyCert(String _verifyCert) {
        this._verifyCert = _verifyCert;
        return this;
    }

    public CertificateManagerBuilder clientVerify(boolean _clientVerify) {
        this._clientVerify = _clientVerify;
        return this;
    }

}
