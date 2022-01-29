package ceptic.common;

public class SecuritySettings {

    private final String certFile;
    private final String keyFile;
    private final String caFile;

    public SecuritySettings(String certFile, String keyFile, String caFile) {
        this.certFile = certFile;
        this.keyFile = keyFile;
        this.caFile = caFile;
    }
}
