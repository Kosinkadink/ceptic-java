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

    public static RequestType SERVER = new RequestType() { String request = "__SERVER"; };
    public static RequestType CLIENT = new RequestType() { String request = "__CLIENT"; };

    private FileManager fileManager = null;
    private RequestType request;
    private String localCert;
    private String localKey;
    private String verifyCert;
    private SSLContext context;

    public CertificateManager(FileManager fileManager, RequestType request, String localCert, String localKey, String verifyCert) {
        this.fileManager = fileManager;
        this.request = request;
        this.localCert = localCert;
        this.localKey = localKey;
        this.verifyCert = verifyCert;

    }

    private void generateContextClient() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, InvalidKeySpecException, KeyStoreException, IOException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(loadKeyStore().getKeyManagers(), loadTrustStore().getTrustManagers(), null);
        context = sslContext;
    }

    private void generateContextServer() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, InvalidKeySpecException, KeyStoreException, IOException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(loadKeyStore().getKeyManagers(), loadTrustStore().getTrustManagers(), null);
        context = sslContext;
    }

    private KeyManagerFactory loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, InvalidKeySpecException {
        //load client certificate
        PemReader cr = new PemReader(new FileReader(Paths.get(certDir, "techtem_cert_client.pem").toString()));
        PemObject co = cr.readPemObject();
        cr.close();
        ByteArrayInputStream incert = new ByteArrayInputStream(co.getContent());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(incert);
        //load client private key
        PemReader pk = new PemReader(new FileReader(Paths.get(certDir, "techtem_client_key.pem").toString()));
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
                continue;
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

        //most secure of passwords
        String password = "TechTemRul35";

        //set up KeyStore
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(null, password.toCharArray());
        ksKeys.setCertificateEntry("ClientCert", cert);
        ksKeys.setKeyEntry("ClientKey",privkey,password.toCharArray(),new Certificate[]{cert});
        //set up KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ksKeys,password.toCharArray());
        return kmf;
    }

    private TrustManagerFactory loadTrustStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        //load server certificate
        PemReader cr = new PemReader(new FileReader(Paths.get(certDir, "techtem_cert.pem").toString()));
        PemObject co = cr.readPemObject();
        cr.close();
        ByteArrayInputStream incert = new ByteArrayInputStream(co.getContent());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(incert);
        //most secure of passwords
        String password = "TechTemRul35";

        //set up TrustStore
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(null, password.toCharArray());
        ksKeys.setCertificateEntry("ClientCert", cert);
        //set up TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ksKeys);
        return tmf;
    }
}
