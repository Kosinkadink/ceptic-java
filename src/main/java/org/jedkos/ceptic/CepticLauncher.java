package org.jedkos.ceptic;

import org.jedkos.ceptic.client.CepticClient;
import org.jedkos.ceptic.client.CepticClientBuilder;
import org.jedkos.ceptic.client.ClientSettings;
import org.jedkos.ceptic.client.ClientSettingsBuilder;
import org.jedkos.ceptic.common.CepticRequest;
import org.jedkos.ceptic.common.CepticResponse;
import org.jedkos.ceptic.common.CepticStatusCode;
import org.jedkos.ceptic.common.CommandType;
import org.jedkos.ceptic.common.exceptions.CepticException;
import org.jedkos.ceptic.endpoint.EndpointEntry;
import org.jedkos.ceptic.endpoint.exceptions.EndpointManagerException;
import org.jedkos.ceptic.server.CepticServer;
import org.jedkos.ceptic.server.CepticServerBuilder;
import org.jedkos.ceptic.server.ServerSettings;
import org.jedkos.ceptic.server.ServerSettingsBuilder;
import org.jedkos.ceptic.stream.StreamData;
import org.jedkos.ceptic.stream.StreamHandler;
import org.jedkos.ceptic.stream.exceptions.StreamException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CepticLauncher {

	public static void main(String[] args) throws EndpointManagerException {
		doServer();
//		doClient();
//		doClientBasic();
//		doClientBasicParallel();
	}

	private static void doServer() throws EndpointManagerException {
		// create server settings
		ServerSettings settings = new ServerSettingsBuilder()
				.verbose(true)
				.daemon(false)
				.build();

		CepticServer server = new CepticServerBuilder()
				.secure(false)
				.settings(settings)
				.build();

		server.addCommand(CommandType.GET);

		server.addRoute(CommandType.GET, "/", new EndpointEntry() {
			@Override
			public CepticResponse perform(CepticRequest request, HashMap<String, String> values) {
				return new CepticResponse(CepticStatusCode.OK);
			}
		});

		server.addRoute(CommandType.GET, "/exchange", new EndpointEntry() {
			@Override
			public CepticResponse perform(CepticRequest request, HashMap<String, String> values) {
				StreamHandler stream = request.beginExchange();
				if (stream == null) {
					return new CepticResponse(CepticStatusCode.BAD_REQUEST);
				}
				try {
					String previousData = "";
					int count = 0;
					while (true) {
						StreamData streamData = stream.readData(100);
						if (!streamData.isData() && !streamData.isResponse())
							continue;
						//if (streamData.isData() && streamData.getData().length == 0)
						//	continue;
						count++;
//						try {
//							Thread.sleep(10);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
						//System.out.println(streamData.getData());
						String data;
						try {
							data = new String(streamData.getData(), StandardCharsets.UTF_8);
							previousData = data;
						} catch (NullPointerException e) {
							System.out.printf("Caught NPE for following data (previous was %s):", previousData);
							System.out.println("isData: " + streamData.isData());
							System.out.println("isResponse: " + streamData.isResponse());
							if (streamData.isData()) {
								System.out.println(streamData.getData().length);
							}
							if (streamData.isResponse()) {
								System.out.println(new String(streamData.getResponse().getData(), StandardCharsets.UTF_8));
							}
							throw e;
						}
						if (count % 500 == 0)
							System.out.println("DATA: " + data);
						if (data.equals("exit")) {
							System.out.println("Client requested end of exchange!");
							break;
						} else {
							stream.sendData(streamData.getData());
						}
					}
				} catch (StreamException e) {
					System.out.println("StreamException: " + e);
				}
				return new CepticResponse(CepticStatusCode.OK);
			}
		});

		server.start();
		Scanner in = new Scanner(System.in);
		System.out.println("Press ENTER to close server...");
		Thread thread = new Thread(){
			public void run(){
				System.out.println("Thread Running");
				doClient();
				System.out.println("Thread Done Running");
			}
		};
		//thread.start();
		in.nextLine();
		System.out.println("ENTER pressed!");
		server.stopRunning();
		System.out.println("Server has stopped");
	}

	private static void doClientBasic() {
		// create client settings
		ClientSettings settings = new ClientSettingsBuilder()
				.build();
		// create client
		CepticClient client = new CepticClientBuilder()
				.settings(settings)
				.checkHostname(false)
				.secure(false)
				.build();

		for (int i = 0; i < 10000; i++) {
			// create request
			CepticRequest request = new CepticRequest("get", "localhost/");

			// try to connect to server
			try {
				CepticResponse response = client.connect(request);
				System.out.println(String.format("#%d", i+1) + "Request successful!");
				System.out.printf("%s\n%s\n%s\n",
						response.getStatusCode().getValue(),
						response.getHeaders().toJSONString(),
						new String(response.getBody(), StandardCharsets.UTF_8));
			} catch (CepticException exception) {
				System.out.println("Exception occurred trying to make request:");
				exception.printStackTrace();
			}
		}
		client.stop();
	}

	private static void doClientBasicParallel() {
		// create client settings
		ClientSettings settings = new ClientSettingsBuilder()
				.build();
		// create client
		CepticClient client = new CepticClientBuilder()
				.settings(settings)
				.checkHostname(false)
				.secure(false)
				.build();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();


		for (int i = 0; i < 1000; i++) {

//			try {
//				Thread.sleep(25);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			int finalI = i;
			executor.execute(() -> {

				System.out.println(String.format("Executing Request #%d",finalI+1));
				// create request
				CepticRequest request = new CepticRequest("get", "localhost/");
				// try to connect to server
				try {
					CepticResponse response = client.connect(request);
					System.out.println(String.format("#%d", finalI +1) + "Request successful!");
					System.out.printf("%s\n%s\n%s\n",
							response.getStatusCode().getValue(),
							response.getHeaders().toJSONString(),
							new String(response.getBody(), StandardCharsets.UTF_8));
				} catch (CepticException exception) {
					System.out.println("Exception occurred trying to make request:");
					exception.printStackTrace();
				}
			});
		}
		System.out.println("Awaiting executor termination...");
		try {
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Stopping client");
		client.stop();
		System.out.println("Stopping executor");
		executor.shutdownNow();
	}

	private static void doClient() {
		// create client settings
		ClientSettings settings = new ClientSettingsBuilder()
				.build();
		// create client
		CepticClient client = new CepticClientBuilder()
				.settings(settings)
				.checkHostname(false)
				.secure(false)
				.build();
		// create request
		CepticRequest request = new CepticRequest("streamget", "localhost");

		// try to connect to server
		try {
			CepticResponse response = client.connect(request);
			System.out.println("Request successful!");
			System.out.printf("%s\n%s\n%s\n",
					response.getStatusCode().getValue(),
					response.getHeaders().toJSONString(),
					new String(response.getBody(), StandardCharsets.UTF_8));
		} catch (CepticException exception) {
			System.out.println("Exception occurred trying to make request:");
			exception.printStackTrace();
		}

		// create exchange request
		CepticRequest requestExchange = new CepticRequest(CommandType.GET, "localhost/exchange");
		// try to connect to server
		try {
			ArrayList<byte[]> allData = new ArrayList<>();
			CepticResponse response = client.connect(requestExchange);
			System.out.println("Request successful!");
			System.out.printf("%s\n%s\n%s\n",
					response.getStatusCode().getValue(),
					response.getHeaders().toJSONString(),
					new String(response.getBody(), StandardCharsets.UTF_8));
			if (response.getExchange()) {
				StreamHandler stream = response.getStream();
				boolean hasReceivedResponse = false;
				for (int i = 0; i < 10000; i++) {
					String stringData = String.format("echo%d", i);
					byte[] sentData = stringData.getBytes(StandardCharsets.UTF_8);
					stream.sendData(sentData);
					StreamData data = stream.readData(100);
					if (data.isResponse()) {
						hasReceivedResponse = true;
						System.out.println("Received response; end of exchange!");
						System.out.printf("%s\n%s\n%s\n",
								data.getResponse().getStatusCode().getValue(),
								data.getResponse().getHeaders().toJSONString(),
								new String(data.getResponse().getBody(), StandardCharsets.UTF_8));
						break;
					} else {

						byte[] receivedData = data.getData();
						if (receivedData == null) {
							System.out.println("Received null when expecting " + stringData);
						} else {
							allData.add(receivedData);
						}
						if (i % 500 == 0)
							System.out.printf("Received echo: %s\n", new String(data.getData(), StandardCharsets.UTF_8));
					}
				}
				if (!hasReceivedResponse) {
					stream.sendData("exit".getBytes(StandardCharsets.UTF_8));
					StreamData data = stream.readData(100);
					if (data.isResponse()) {
						System.out.println("Received response after sending exit; end of exchange!");
						System.out.printf("%s\n%s\n%s\n",
								data.getResponse().getStatusCode().getValue(),
								data.getResponse().getHeaders().toJSONString(),
								new String(data.getResponse().getBody(), StandardCharsets.UTF_8));
					}
				}
				System.out.println("Total non-null data echoed back: " + allData.size());
				stream.sendClose();
			}
		} catch (CepticException exception) {
			System.out.println("Exception occurred trying to make exchange request:");
			exception.printStackTrace();
		}

		// stop client
		client.stop();
	}

}
