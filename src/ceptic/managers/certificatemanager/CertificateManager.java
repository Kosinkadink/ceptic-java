package ceptic.managers.certificatemanager;

import ceptic.managers.filemanager.FileManager;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CertificateManager {

    public interface RequestType {
        String request = null;
    }
    // TODO: update to match present version of ceptic

    public static RequestType SERVER = new RequestType() { String request = "__SERVER"; };
    public static RequestType CLIENT = new RequestType() { String request = "__CLIENT"; };

    private FileManager fileManager;
    private RequestType request;
    private String localCert;
    private String localKey;
    private String verifyCert;
    private boolean clientVerify;
    private SSLContext context;

    private String defaultPassword = "C3pt1c4daW1n";
    // default locations
    private static String defaultClientCert = "cert_client";
    private static String defaultClientKey = "key_client";
    private static String defaultServerCert = "cert_server";
    private static String defaultServerKey = " key_server";

    public CertificateManager(RequestType request, FileManager fileManager, String localCert, String localKey, String verifyCert, boolean clientVerify) {
        this.fileManager = fileManager;
        this.request = request;
        this.localCert = localCert;
        this.localKey = localKey;
        this.verifyCert = verifyCert;
        this.clientVerify = clientVerify;
    }

    public Socket wrapSocket(Socket socket) {
        return null;
    }

    public Socket wrapSocketServer(Socket socket) {
        return null;
    }

    public Socket wrapSocketClient(Socket socket) {
        return null;
    }

    public void generateContext() {
        if (request.equals(CLIENT)) {
            try {
                generateContextClient();
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | InvalidKeySpecException | CertificateException | KeyStoreException | IOException | KeyManagementException e) {
                e.printStackTrace();
            }
        }
        else if (request.equals(SERVER)) {
            try {
                generateContextServer();
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | InvalidKeySpecException | CertificateException | KeyStoreException | IOException | KeyManagementException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateContextClient() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, InvalidKeySpecException, KeyStoreException, IOException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        // if clientVerify is required, use everything
        if (clientVerify) {
            sslContext.init(loadKeyStore(getClientLocalCert(),getClientLocalKey()).getKeyManagers(),loadTrustStore(getServerVerifyCert()).getTrustManagers(), null);
        }
        // otherwise, only use localCert and localKey
        else {
            sslContext.init(null ,loadTrustStore(getServerVerifyCert()).getTrustManagers(), null);
        }
        context = sslContext;
    }

    private void generateContextServer() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, InvalidKeySpecException, KeyStoreException, IOException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        // if clientVerify is required, use everything
        if (clientVerify) {
            sslContext.init(loadKeyStore(getServerLocalCert(),getServerLocalKey()).getKeyManagers(),loadTrustStore(getClientVerifyCert()).getTrustManagers(), null);
        }
        // otherwise, only use verifyCert
        else {
            sslContext.init(loadKeyStore(getServerLocalCert(),getServerLocalKey()).getKeyManagers(),null, null);
        }
        context = sslContext;
    }

    private String getFullLocation(String toBeChanged, String defaultLoc) {
        // if empty, replace with default
        if (toBeChanged.isEmpty()) {
            toBeChanged = defaultLoc;
        }
        if (!toBeChanged.startsWith(fileManager.getDirectory("certification"))) {
            toBeChanged = Paths.get(fileManager.getDirectory("certification"),toBeChanged).toString();
        }
        return toBeChanged;
    }

    private KeyManagerFactory loadKeyStore(String certificateToUse, String keyToUse) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, InvalidKeySpecException {
        //load client certificate
        PemReader cr = new PemReader(new FileReader(certificateToUse));
        PemObject co = cr.readPemObject();
        cr.close();
        ByteArrayInputStream incert = new ByteArrayInputStream(co.getContent());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(incert);
        //load client private key
        PemReader pk = new PemReader(new FileReader(keyToUse));
        //SOURCE FOR LOADING PRIVATE KEY: http://stackoverflow.com/questions/11787571/how-to-read-pem-file-to-get-private-and-public-key
        BufferedReader br = new BufferedReader(pk);
        StringBuilder builder = new StringBuilder();
        boolean inKey = false;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (!inKey) {
                if (line.startsWith("-----BEGIN ") &&
                        line.endsWith(" PRIVATE KEY-----")) {
                    inKey = true;
                }
            }
            else {
                if (line.startsWith("-----END ") &&
                        line.endsWith(" PRIVATE KEY-----")) {
                    inKey = false;
                    break;
                }
                builder.append(line);
            }
        }
        //close BufferedReader
        br.close();
        byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privkey = kf.generatePrivate(keySpec);

        //set up KeyStore
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(null, defaultPassword.toCharArray());
        ksKeys.setCertificateEntry("ClientCert", cert);
        ksKeys.setKeyEntry("ClientKey",privkey,defaultPassword.toCharArray(),new Certificate[]{cert});
        //set up KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ksKeys,defaultPassword.toCharArray());
        return kmf;
    }

    private TrustManagerFactory loadTrustStore(String certificateToUse) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        //load server certificate
        PemReader cr = new PemReader(new FileReader(certificateToUse));
        PemObject co = cr.readPemObject();
        cr.close();
        ByteArrayInputStream incert = new ByteArrayInputStream(co.getContent());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(incert);

        //set up TrustStore
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(null, defaultPassword.toCharArray());
        ksKeys.setCertificateEntry("ClientCert", cert);
        //set up TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ksKeys);
        return tmf;
    }

    // GETTERS (that change state) for all certs/keys
    private String getClientLocalCert() {
        return getFullLocation(localCert, defaultClientCert);
    }

    private String getClientLocalKey() {
        return getFullLocation(localKey, defaultClientKey);
    }

    private String getClientVerifyCert() {
        return getFullLocation(verifyCert, defaultServerCert);
    }

    private String getServerLocalCert() {
        return getFullLocation(localCert, defaultServerCert);
    }

    private String getServerLocalKey() {
        return getFullLocation(localKey, defaultServerKey);
    }

    private String getServerVerifyCert() {
        return getFullLocation(verifyCert, defaultClientKey);
    }
    // END OF GETTERS
}
