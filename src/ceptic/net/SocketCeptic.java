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
	public SocketCeptic() { }

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

	public SocketCeptic wrapSocket() {
		return null;
	}
	
	public void createSocket() throws UnknownHostException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, InvalidKeySpecException {
		//s = new Socket(host,port);
		//s = rc.createSSLContext().getSocketFactory().createSocket(host,port);
		sin = s.getInputStream();
		sout = s.getOutputStream();
		s.setTcpNoDelay(true);
	}
	
	public void send(String msg) throws SocketCepticException {
		byte[] byteBuffer;
		byteBuffer = msg.getBytes();
		send(byteBuffer);
	}

	public void send(byte[] msg) throws SocketCepticException {
		//create string with length of message
		byte[] total_size = String.format("%16s", Integer.toString(msg.length)).getBytes();
		sendRaw(total_size);
		sendRaw(msg);
	}

	public void sendall(String msg) throws SocketCepticException {
		send(msg);
	}

	public void sendall(byte[] msg) throws SocketCepticException {
		send(msg);
	}

	public void sendRaw(byte[] msg) throws SocketCepticException {
		try {
			sout.write(msg);
		}
		catch (IOException e) {
			throw new SocketCepticException(e.toString());
		}
	}

	public byte[] recvRaw(int bytes) throws SocketCepticException {
		int charCount = 0;
		int totalCount = 0;
		byte[] byteBuffer = new byte[bytes];
		while (totalCount < bytes) {
			try {
				charCount = sin.read(byteBuffer,charCount,bytes-totalCount);
			}
			catch (IOException e) {
				throw new SocketCepticException(e.toString());
			}
			totalCount += charCount;
			if (charCount == 0) {
				break;
			}
		}
		return byteBuffer;
	}

	public String recvRawString(int bytes) throws SocketCepticException {
		return new String(recvRaw(bytes));
	}

	public byte[] recvBytes(int bytes) throws SocketCepticException {
		byte[] sizeBuffer;
		// get length of bytes
		sizeBuffer = recvRaw(16);
		int sizeToRecv = Integer.parseInt(new String(sizeBuffer).replaceAll("\\D", ""));
		int amount = bytes;
		if (sizeToRecv < amount) {
			amount = sizeToRecv;
		}
		return recvRaw(amount);
	}
	
	public String recvString(int bytes) throws SocketCepticException {
		return new String(recvBytes(bytes));
	}
		
	public Socket getSocket() {
		return s;
	}
	
	public void close() throws SocketCepticException {
		try {
			s.close();
		}
		catch (IOException e) {
			throw new SocketCepticException(e.toString());
		}
	}
	
}
