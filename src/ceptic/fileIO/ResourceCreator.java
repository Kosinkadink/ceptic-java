package ceptic.fileIO;

import java.security.CodeSource;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;

import ceptic.CepticLauncher;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URISyntaxException;

public class ResourceCreator {
	String mainDir = null;
	String certDir = null;
	
	public ResourceCreator() throws URISyntaxException {
		mainDir = getMainDirectory();
		certDir = Paths.get(mainDir,"resources/source/certification/").toString();
	}
	
	private String getMainDirectory() throws URISyntaxException  {
		//SOURCE: http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
		CodeSource codeSource = CepticLauncher.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		String jarDir = jarFile.getParentFile().getPath();
		return jarDir;
	}
	
	public void createDirectories() throws IOException {
		Files.createDirectories(Paths.get(mainDir,"resources/"));
		Files.createDirectories(Paths.get(mainDir,"resources/cache/"));
		Files.createDirectories(Paths.get(mainDir,"resources/downloads/"));
		Files.createDirectories(Paths.get(mainDir,"resources/uploads/"));
		Files.createDirectories(Paths.get(mainDir,"resources/source/"));
		Files.createDirectories(Paths.get(mainDir,"resources/source/certification/"));
		Files.createDirectories(Paths.get(mainDir,"resources/protocols/"));
		Files.createDirectories(Paths.get(mainDir,"resources/programparts/"));
		Files.createDirectories(Paths.get(mainDir,"resources/networkpass/"));
	}
	
	public void createNetPass() throws IOException {
		File netPassFile = new File(Paths.get(mainDir,"resources/networkpass/netpass.txt").toString());
		netPassFile.createNewFile();
	}
	
	public String getNetPass() {
		String netPass = null;
		try {
			InputStream fstream = new FileInputStream(Paths.get(mainDir,"resources/networkpass/netpass.txt").toString());
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
	
	public String getMainDir() {
		return mainDir;
	}
	
	public SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException, InvalidKeySpecException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(loadKeyStore().getKeyManagers(), loadTrustStore().getTrustManagers(), null);
		return sslContext;
	}
	
	/*private KeyManagerFactory loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, InvalidKeySpecException {
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
	}	*/
}
