package org.jedkos.ceptic.security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

public class SecuritySettings {

    private String keyPassword = null;

    private String localCert = null;
    private String localKey = null;
    private String remoteCert = null;
    private boolean verifyRemote = true;
    private boolean secure = true;

    private KeyManagerFactory localFactory;
    private TrustManagerFactory remoteFactory;

    //region Key Password
    // TODO: replace this implementation with something that is actually good
    public void setKeyPassword(String password) {
        keyPassword = password;
    }

    public String getKeyPassword() {
        String tempPassword = keyPassword;
        keyPassword = null;
        return tempPassword;
    }
    //endregion

    //region Getters
    public String getLocalCert() {
        return localCert;
    }

    public String getLocalKey() {
        return localKey;
    }

    public String getRemoteCert() {
        return remoteCert;
    }

    public boolean isVerifyRemote() {
        return verifyRemote;
    }

    public boolean isSecure() {
        return secure;
    }

    public KeyManagerFactory getLocalFactory() {
        return localFactory;
    }

    public TrustManagerFactory getRemoteFactory() {
        return remoteFactory;
    }
    //endregion

    protected SecuritySettings(boolean verifyRemote, boolean secure) {
        this.verifyRemote = verifyRemote;
        this.secure = secure;
    }

    protected SecuritySettings(String remoteCert, boolean verifyRemote) {
        this.remoteCert = remoteCert;
        this.verifyRemote = verifyRemote;
    }

    protected SecuritySettings(String localCert, String localKey, boolean verifyRemote) {
        this.localCert = localCert;
        this.localKey = localKey;
        this.verifyRemote = verifyRemote;
    }

    protected SecuritySettings(String localCert, String localKey, String remoteCert, boolean verifyRemote) {
        this.localCert = localCert;
        this.localKey = localKey;
        this.remoteCert = remoteCert;
        this.verifyRemote = verifyRemote;
    }

    //region Client
    public static SecuritySettings Client() {
        return new SecuritySettings(true, true);
    }

    public static SecuritySettings Client(boolean verifyRemote) {
        return new SecuritySettings(verifyRemote, true);
    }

    public static SecuritySettings Client(String remoteCert) {
        return new SecuritySettings(remoteCert, true);
    }

    public static SecuritySettings Client(String remoteCert, boolean verifyRemote) {
        return new SecuritySettings(remoteCert, verifyRemote);
    }

    public static SecuritySettings Client(String localCert, String remoteCert) {
        return new SecuritySettings(localCert, null, remoteCert, true);
    }

    public static SecuritySettings Client(String localCert, String remoteCert, boolean verifyRemote) {
        return new SecuritySettings(localCert, null, remoteCert, verifyRemote);
    }

    public static SecuritySettings Client(String localCert, String localKey, String remoteCert) {
        return new SecuritySettings(localCert, localKey, remoteCert, true);
    }

    public static SecuritySettings Client(String localCert, String localKey, String remoteCert, boolean verifyRemote) {
        return new SecuritySettings(localCert, localKey, remoteCert, verifyRemote);
    }

    public static SecuritySettings ClientUnsecure() {
        return new SecuritySettings(true, false);
    }
    //endregion

    //region Server
    public static SecuritySettings Server(String localCert) {
        return new SecuritySettings(localCert, null, true);
    }

    public static SecuritySettings Server(String localCert, String localKey) {
        return new SecuritySettings(localCert, localKey, true);
    }

    public static SecuritySettings Server(String localCert, String localKey, String remoteCert) {
        return new SecuritySettings(localCert, localKey, remoteCert, true);
    }

    public static SecuritySettings ServerUnsecure() {
        return new SecuritySettings(true, false);
    }
    //endregion

}
