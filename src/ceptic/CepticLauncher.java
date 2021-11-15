package ceptic;

import ceptic.client.CepticClient;
import ceptic.client.CepticClientBuilder;
import ceptic.client.ClientSettings;
import ceptic.client.ClientSettingsBuilder;
import ceptic.common.CepticRequest;
import ceptic.common.CepticResponse;
import ceptic.common.CepticStatusCode;
import ceptic.common.CommandType;
import ceptic.common.exceptions.CepticException;
import ceptic.endpoint.EndpointEntry;
import ceptic.endpoint.exceptions.EndpointManagerException;
import ceptic.server.CepticServer;
import ceptic.server.CepticServerBuilder;
import ceptic.server.ServerSettings;
import ceptic.server.ServerSettingsBuilder;
import ceptic.stream.StreamData;
import ceptic.stream.StreamHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class CepticLauncher {

	public static void main(String[] args) throws EndpointManagerException {
		doServer();
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

		server.start();
		Scanner in = new Scanner(System.in);
		System.out.println("Press ENTER to close server...");
		in.nextLine();
		System.out.println("ENTER pressed!");
		server.stopRunning();
		System.out.println("Server has stopped");
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
					response.getStatusCode().getValueInt(),
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
			CepticResponse response = client.connect(requestExchange);
			System.out.println("Request successful!");
			System.out.printf("%s\n%s\n%s\n",
					response.getStatusCode().getValueInt(),
					response.getHeaders().toJSONString(),
					new String(response.getBody(), StandardCharsets.UTF_8));
			if (response.getExchange()) {
				StreamHandler stream = response.getStream();
				boolean hasReceivedResponse = false;
				for (int i = 0; i < 4; i++) {
					stream.sendData(String.format("echo%d", i).getBytes(StandardCharsets.UTF_8));
					StreamData data = stream.readData(100);
					if (data.isResponse()) {
						hasReceivedResponse = true;
						System.out.println("Received response; end of exchange!");
						System.out.printf("%s\n%s\n%s\n",
								data.getResponse().getStatusCode().getValueInt(),
								data.getResponse().getHeaders().toJSONString(),
								new String(data.getResponse().getBody(), StandardCharsets.UTF_8));
						break;
					} else {
						System.out.printf("Received echo: %s\n", new String(data.getData(), StandardCharsets.UTF_8));
					}
				}
				if (!hasReceivedResponse) {
					stream.sendData("exit".getBytes(StandardCharsets.UTF_8));
					StreamData data = stream.readData(100);
					if (data.isResponse()) {
						System.out.println("Received response after sending exit; end of exchange!");
						System.out.printf("%s\n%s\n%s\n",
								data.getResponse().getStatusCode().getValueInt(),
								data.getResponse().getHeaders().toJSONString(),
								new String(data.getResponse().getBody(), StandardCharsets.UTF_8));
					}
				}
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
