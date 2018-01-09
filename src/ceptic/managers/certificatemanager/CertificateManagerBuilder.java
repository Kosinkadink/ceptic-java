package ceptic.managers.certificatemanager;

import ceptic.managers.filemanager.FileManager;

public class CertificateManagerBuilder {

    FileManager _fileManager = null;
    // fields for storing actual locations to use
    String _localCert;
    String _localKey;
    String _verifyCert;

    // default locations
    String defautlClientCert = "cert_client";
    String defaultClientKey = "key_client";
    String defaultServerCert = "cert_server";
    String defaultServerKey = " key_server";

    public CertificateManagerBuilder() { }

    public CertificateManager buildClientManager() {
        return new CertificateManager();
    }

    public CertificateManager buildServerManager() {
        return new CertificateManager();
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

}
