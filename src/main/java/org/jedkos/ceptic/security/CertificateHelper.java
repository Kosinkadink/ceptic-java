package org.jedkos.ceptic.security;

import org.jedkos.ceptic.security.exceptions.SecurityException;
import org.jedkos.ceptic.security.exceptions.SecurityPEMException;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertificateHelper {

    private static final Pattern privateKeyRegex = Pattern.compile(
            "-----BEGIN ([A-Z ]+)-----([\\s\\S]*?)-----END [A-Z ]+-----");

    private static final String BeginString = "-----BEGIN ";

    private static final String RSAPrivateKey = "RSA PRIVATE KEY";
    private static final String PrivateKeyNormal = "PRIVATE KEY";
    private static final String EncryptedPrivateKey = "ENCRYPTED PRIVATE KEY";

    private static final String RSAPublicKey = "RSA PUBLIC KEY";
    private static final String PublicKeyNormal = "PUBLIC KEY";
    private static final String CertificateString = "CERTIFICATE";

    private static final char[] dummyPassword = "c3pt1c".toCharArray();

    /**
     * @param certificate Path to certificate file
     * @param key Path to key file
     * @param password Password to key file - set to null if none
     * @return
     */
    public static KeyManagerFactory generateFromSeparate(String certificate, String key, String password) throws SecurityException {
        // load public key
        Collection<? extends Certificate> origCerts = getCertificatesFromFile(certificate);
        Certificate[] certs = origCerts.toArray(new Certificate[0]);

        // load private key
        PrivateKey privateKey = getPrivateKeyFromFile(key, password);
        // return KeyManagerFactory with certificate and private key loaded
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("Cert", certs[0]); // assumes first cert is top level and not root
            keyStore.setKeyEntry("Key", privateKey, dummyPassword, certs);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, dummyPassword);
            return keyManagerFactory;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new SecurityException(MessageFormat.format("Could not create KeyManagerFactory from certificate and key: {0}", e), e);
        }
    }

    public static KeyManagerFactory generateFromCombined(String certificate, String password) throws SecurityException {
        // return KeyManagerFactory with certificate and private key loaded
        return generateFromSeparate(certificate, certificate, password);
    }

    public static Certificate getCertificateFromFile(String certificate) throws SecurityException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificate(new FileInputStream(certificate));
        } catch(CertificateException | FileNotFoundException e) {
            throw new SecurityException(MessageFormat.format("Certificate at '{0}' could not be loaded: {1}", certificate, e));
        }
    }

    public static Collection<? extends Certificate> getCertificatesFromFile(String certificate) throws SecurityException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificates(Files.newInputStream(Paths.get(certificate)));
        } catch(CertificateException | IOException e) {
            throw new SecurityException(MessageFormat.format("Certificate at '{0}' could not be loaded: {1}", certificate, e), e);
        }
    }

    private static PrivateKey getPrivateKeyFromFile(String key, String password) throws SecurityException {
        // read key file
        try (DataInputStream rawStream = new DataInputStream(Files.newInputStream(Paths.get(key)))) {
            byte[] bytes = new byte[rawStream.available()];
            rawStream.readFully(bytes);
            String pemRaw = new String(bytes, StandardCharsets.UTF_8);
            pemRaw = pemRaw.trim();
            if (!pemRaw.startsWith(BeginString)) {
                throw new SecurityPEMException(MessageFormat.format("No Key found in file at '{0}'.", key));
            }
            pemRaw = pemRaw.replace("\r\n", "");
            pemRaw = pemRaw.replace("\n", "");
            Matcher matcher = privateKeyRegex.matcher(pemRaw);
            while (matcher.find()) {
                String keyType = matcher.group(1);
                String pemBase64 = matcher.group(2);
                byte[] pemBytes = Base64.getDecoder().decode(pemBase64);
                KeyFactory keyFactory;
                KeySpec keySpec;
                switch (keyType) {
                    // Solution for converting PKCS#1 to PKCS#8 format (PKCS#1 not natively supported by Java)
                    // https://stackoverflow.com/questions/7216969/getting-rsa-private-key-from-pem-base64-encoded-private-key-file/55339208#55339208
                    case RSAPrivateKey:
                        // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
                        int pkcs1Length = pemBytes.length;
                        int totalLength = pkcs1Length + 22;
                        byte[] pkcs8Header = new byte[] {
                                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                                0x2, 0x1, 0x0, // Integer (0)
                                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
                        };
                        byte[] combined = Arrays.copyOf(pkcs8Header, pkcs8Header.length + pemBytes.length);
                        System.arraycopy(pemBytes, 0, combined, pkcs8Header.length, pemBytes.length);
                        pemBytes = combined; // pemBytes is now in PKCS#8 key format
                    case PrivateKeyNormal:
                        keyFactory = KeyFactory.getInstance("RSA");
                        keySpec = new PKCS8EncodedKeySpec(pemBytes);
                        return keyFactory.generatePrivate(keySpec);
                    case EncryptedPrivateKey:
                        EncryptedPrivateKeyInfo privateKeyInfo = new EncryptedPrivateKeyInfo(pemBytes);
                        PBEKeySpec passwordKeySpec = new PBEKeySpec(password.toCharArray());
                        SecretKeyFactory pbeKeyFactory = SecretKeyFactory.getInstance(privateKeyInfo.getAlgName());
                        keySpec = privateKeyInfo.getKeySpec(pbeKeyFactory.generateSecret(passwordKeySpec));
                        keyFactory = KeyFactory.getInstance("RSA");
                        return keyFactory.generatePrivate(keySpec);
                    case RSAPublicKey:
                    case PublicKeyNormal:
                    case CertificateString:
                        continue;
                    default:
                        throw new SecurityPEMException(MessageFormat.format("Private Key Type '{0}' not recognized in file at '{1}'.", keyType, key));
                }
            }
        } catch (IOException e) {
            throw new SecurityException(MessageFormat.format("Key file at '{0}' could not be read: {1}", key, e), e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            throw new SecurityPEMException(MessageFormat.format("Error while attempting to load private key: {0}", e), e);
        }
        throw new SecurityPEMException(MessageFormat.format("No Key found in the file at '{0}'.", key));
    }

    public static TrustManagerFactory loadTrustManager(String certificate) throws SecurityException {
        Collection<? extends Certificate> origCerts = getCertificatesFromFile(certificate);
        Certificate[] certs = origCerts.toArray(new Certificate[0]);

        //Certificate[] certs = (Certificate[]) getCertificatesFromFile(certificate);
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("Cert", certs[0]); // assumes first cert is top level and not root
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new SecurityException(MessageFormat.format("Certificate at '{0}' could not be loaded: {1}", certificate, e), e);
        }
    }

    private static KeyManagerFactory loadKeyManager(String certificate, String key, String password) {
        return null;
    }

}
