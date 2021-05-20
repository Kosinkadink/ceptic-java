//package ceptic;
//
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.HashMap;
//import java.util.Map;
//
//import ceptic.fileIO.ResourceCreator;
//import ceptic.net.SocketCeptic;
//
//public abstract class CepticClient {
//	protected String host;
//	protected int port;
//	protected boolean keepRunning = true;
//	protected String netPass;
//	protected String scriptname;
//	protected String scriptfunction;
//	protected String version = "3.0.0";
//	protected ResourceCreator rc = null;
//	protected Map<String, CommandMethod> funcMap = null;
//
//	//interface for returning a method
//	interface CommandMethod {
//		String method(SocketCeptic s, Object data);
//	}
//
//
//	protected void initialize() {
//		try {
//			rc = new ResourceCreator();
//			System.out.println("Main dir: " + rc.getMainDir());
//			rc.createDirectories();
//			rc.createNetPass();
//		} catch (IOException | URISyntaxException e) {
//			e.printStackTrace();
//			throw new RuntimeException("Could not initialize");
//		}
//	}
//
//
//	protected void init_spec() {
//		funcMap = new HashMap<String, CommandMethod>();
//		/*funcMap.put("commandName", new CommandMethod() {
//			public String method(SocketTem s, Object data) { return actualMethod(s,data); }
//		});*/
//	}
//
//
//	protected void boot() {
//		System.out.println("TechTem " + scriptname.substring(0,1).toUpperCase() +
//				scriptname.substring(1) + " Client");
//		System.out.println(version);
//	}
//
//
//	protected String connectprotocolclient(SocketCeptic s, String data, String command) throws IOException {
//		//strings for temporary storage
//		String incomingData = null;
//		//string to determine if network pass required
//		String hasPass = null;
//		//load netPass
//		netPass = rc.getNetPass();
//		//receive if netPass required
//		hasPass = s.recvString(2);
//		//if netPass required, do what's necessary
//		if (hasPass.equals("yp")) {
//			if (netPass == null || netPass.equalsIgnoreCase("")) {
//				s.send("n");
//				s.close();
//				return("requires password");
//			}
//			else {
//				s.send("y");
//				s.recvString(2);
//				s.send(netPass);
//				incomingData = s.recvString(1);
//				if (!(incomingData.equals("y"))) {
//					s.close();
//					return("incorrect password");
//				}
//			}
//		}
//		//send client info to see if compatible to continue
//		s.send(scriptname+":"+scriptfunction+":"+version);
//		incomingData = s.recvString(1);
//		if (incomingData.equals("y")) {
//			s.send("ok");
//			s.recvString(2);
//			System.out.println("success initiated");
//			return distinguishCommand(s,data,command);
//		}
//		//not compatible, so receive some info as a consolation prize
//		else {
//			s.send("ok");
//			incomingData = s.recvString(1024);
//			s.close();
//			System.out.println("failure. closing connection...");
//			return incomingData;
//		}
//	}
//
//
//	protected String distinguishCommand(SocketCeptic s, String data, String command) throws IOException {
//		CommandMethod cm = funcMap.get(command);
//		if (cm == null) {
//			System.out.println("unknown command: " + command);
//			s.send("no");
//			return null;
//		}
//		else {
//			s.send(command);
//			String understood = s.recvString(2);
//			if (understood.equals("ok")) {
//				System.out.println("command: " + command + " understood by server");
//				return cm.method(s,(Object)data);
//			}
//			else {
//				System.out.println("command: " + command + " not understood by server");
//				return null;
//			}
//		}
//	}
//
//
//}
