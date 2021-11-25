package ceptic.net;
import ceptic.net.exceptions.SocketCepticException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;


public class SocketCeptic {
	private final Socket s;
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

	public SocketCeptic(Socket socket) throws SocketCepticException {
		s = socket;
		try {
			sin = s.getInputStream();
		} catch (IOException e) {
			throw new SocketCepticException("Issue getting Socket input stream: " + e);
		}
		try {
			sout = s.getOutputStream();
		} catch (IOException e) {
			throw new SocketCepticException("Issue getting Socket output stream: " + e);
		}
		try {
			s.setTcpNoDelay(true);
		} catch (SocketException e) {
			throw new SocketCepticException("Issue setting TcpNoDelay: " + e);
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

	//region Send
	public void send(String msg) throws SocketCepticException {
		byte[] byteBuffer;
		byteBuffer = msg.getBytes();
		send(byteBuffer);
	}

	public void send(byte[] msg) throws SocketCepticException {
		//create string with length of message
		byte[] total_size = String.format("%16s", msg.length).getBytes();
		sendRaw(total_size);
		sendRaw(msg);
	}

	public void sendRaw(byte[] msg) throws SocketCepticException {
		try {
			sout.write(msg);
		}
		catch (IOException e) {
			throw new SocketCepticException(e.toString());
		}
	}

	public void sendRaw(String msg) throws SocketCepticException {
		sendRaw(msg.getBytes(StandardCharsets.UTF_8));
	}
	//endregion

	//region Receive

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
		return new String(recvRaw(bytes), StandardCharsets.UTF_8);
	}

	public byte[] recvBytes(int bytes) throws SocketCepticException {
		byte[] sizeBuffer;
		// get length of bytes
		sizeBuffer = recvRaw(16);
		//int sizeToRecv = Integer.parseInt(new String(sizeBuffer).replaceAll("\\D", ""));
		int sizeToRecv = Integer.parseInt(new String(sizeBuffer));
		int amount = bytes;
		if (sizeToRecv < amount) {
			amount = sizeToRecv;
		}
		return recvRaw(amount);
	}
	
	public String recvString(int bytes) throws SocketCepticException {
		return new String(recvBytes(bytes), StandardCharsets.UTF_8);
	}
	//endregion
		
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
