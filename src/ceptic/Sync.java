package ceptic;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

//TechTem Libraries
import ceptic.net.SocketCeptic;


public class Sync extends CepticClient {
	
	public Sync(String host, int port) {
		//rename some strings
		scriptname = "sync";
		scriptfunction = "sync_client";
		version = "3.0.0";
		//do rest of init
		this.host = host;
		this.port = port;
		initialize();
		init_spec();
		runProcesses();
	}
	
	protected void init_spec() {
		funcMap = new HashMap<String, CommandMethod>();
		funcMap.put("time", new CommandMethod() {
			public String method(SocketCeptic s, Object data) { return timeCommand(s,data); }
		});
	}
	
	private String timeCommand(SocketCeptic s, Object data) {
		String timestamp = null;
		try {
			s.send("send");
			timestamp = s.recvString(128);
		} catch (IOException e) {
			System.out.println("ERROR: possible disconnection occurred");
		}
		return timestamp;
	}
	
	protected void runProcesses() {
		terminal();
	}
	
	private void terminal() {
		boot();
		Scanner scan = new Scanner(System.in);
		//String s = scan.next();
		//int i = scan.nextInt();
		while (keepRunning) {
			String user_inp = scan.next();
			if (user_inp.equals("exit")) {
				keepRunning = false;
			}
			else if (user_inp.equals("time")) {
				System.out.println(connectip(this.host, this.port, "", "time"));
			}
		}
		//exit program
		scan.close();
		System.exit(0);
	}
	
	private String connectip(String host, int port, String data, String command) {
		SocketCeptic s;
		try {
			s = new SocketCeptic(host, port, rc);
			return connectprotocolclient(s,data,command);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "ERROR: host or port not valid";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "ERROR: socket closed";
		}
	}
	
	
}
