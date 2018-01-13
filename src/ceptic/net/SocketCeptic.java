package ceptic.net;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import ceptic.fileIO.ResourceCreator;


public class SocketCeptic {
	private Socket s;
	private InputStream sin;
	private OutputStream sout;
	
	/*public SocketCeptic(String host, int port, ResourceCreator rc) throws UnknownHostException, IOException {
		//initialize socket
		this.host = host;
		this.port = port;
		this.rc = rc;
		try {
			createSocket();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
				| CertificateException | InvalidKeySpecException e) {
			e.printStackTrace();
			throw new RuntimeException("SocketTem could not be initialized");
		}
	}*/

	public SocketCeptic(Socket socket) {
		s = socket;
		try {
			sin = s.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			sout = s.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			s.setTcpNoDelay(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void createSocket() throws UnknownHostException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, InvalidKeySpecException {
		//s = new Socket(host,port);
		//s = rc.createSSLContext().getSocketFactory().createSocket(host,port);
		sin = s.getInputStream();
		sout = s.getOutputStream();
		s.setTcpNoDelay(true);
	}
	
	public int send(String msg) throws IOException {
		byte[] byteBuffer;
		byteBuffer = msg.getBytes();
		return send(byteBuffer);
	}
	
	public int sendall(String msg) throws IOException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		return send(msg);
	}
	
	public int send(byte[] msg) throws IOException {
		//create string with length of message
		byte[] total_size = String.format("%16s", Integer.toString(msg.length)).getBytes();
		sout.write(total_size);
		sout.write(msg);
		return 0;
	}
	
	public int sendall(byte[] msg) throws IOException {
		return send(msg);
	}

	public byte[] recvBytes(int bytes) throws IOException {
		int charCount = 0;
		int totalCount = 0;
		byte[] byteBuffer;
		//get length of bytes
		byteBuffer = new byte[16];
		while (totalCount < 16) {
			try {
				charCount = sin.read(byteBuffer,charCount,16-totalCount);
			}
			catch (IOException e) {
				break;
			}
			totalCount += charCount;
		}
		int bytes_to_recv = Integer.parseInt(new String(byteBuffer).replaceAll("\\D", ""));
		//reset charCount and totalCount
		charCount = 0;
		totalCount = 0;
		int actualBytes = bytes;
		if (actualBytes > bytes_to_recv) {
			actualBytes = bytes_to_recv;
		}
		byteBuffer = new byte[actualBytes];
		while (totalCount < actualBytes) {
			try {
				charCount = sin.read(byteBuffer,charCount,actualBytes-totalCount);
			}
			catch (IOException e) {
				break;
			}
			totalCount += charCount;
			if (charCount == 0) {
				break;
			}
		}

		return byteBuffer;
	}
	
	public String recvString(int bytes) throws IOException {
		String data;
		byte[] byteBuffer = recvBytes(bytes);
		data = new String(byteBuffer);

		return data;
	}
		
	public Socket getSocket() {
		return s;
	}
	
	public int close() throws IOException {
		s.close();
		return 0;
	}
	
}
