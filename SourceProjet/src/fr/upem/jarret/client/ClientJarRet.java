package fr.upem.jarret.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import upem.jarret.worker.Worker;

public class ClientJarRet {

	private SocketChannel sc;
	private final String clientID;
	private final String nameServer;
	private final int port;
	private Map<ClientInfo, Worker> mapWorker;


	public ClientJarRet(String clientID, String nameServer, int port)
			throws IOException {
		
		this.clientID = clientID;
		this.nameServer = nameServer;
		this.port = port;
		this.mapWorker = new HashMap<>();
	}
	/**
	 * Start the connection with server
	 * @throws IOException
	 */

	private void open() throws IOException {
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress(nameServer, port));
	}
	/**
	 * Close the connection with client
	 * @throws IOException
	 */
	private void close() throws IOException {
		sc.close();
	}
	/**
	 * Manage all actions from the client
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InterruptedException
	 */
	public void query() throws IOException, ClassNotFoundException,
	IllegalAccessException, InstantiationException, InterruptedException {

		int timeOut = 0;
		Map<String, Object> json;
		// request a Task
		do {
			//open();
			RequestTask rT = new RequestTask(sc, nameServer);
			System.out.println("GET TASK");
			json = rT.getJson();
			System.out.println(json);
			timeOut = RequestTask.getTimeOut(json);

			if (timeOut > 0) {
				System.out.println("Timeout :"+timeOut+"...");
				close();
				Thread.sleep(timeOut*1000);
			}
		} while (timeOut != 0);

		ClientInfo client = ClientInfo.createClient(json);

		Worker worker = mapWorker.get(client);
		if(worker == null){
			//System.out.println("NEW WORKER");
			// treat the JSON
			RequestAndAnswer js = RequestAndAnswer
					.createMapJson(json, clientID, sc);
			System.out.println("Computing...");
			js.compute();
			js.sendAnswer();
			System.out.println("Sending...");
			js.getAnswerAfterPost();
			mapWorker.put(client, js.getWorker());
			//close();

		}
		else{
			//System.out.println("WORKER KNOWN");
			RequestAndAnswer js = RequestAndAnswer.createMapJsonWithWorker(json, worker , clientID, sc);
			System.out.println("Computing...");
			js.compute();
			System.out.println("Sending...");
			js.sendAnswer();
			js.getAnswerAfterPost();
			//close();

		}

	}
	/**
	 * Display usage int System.out
	 */
	private static void usage() {
		System.out.println("ClientJarRet clientID nameServer port");
	}

	public static void main(String[] args) throws NumberFormatException,
	IOException, ClassNotFoundException, IllegalAccessException,
	InstantiationException {
		if (args.length != 3) {
			usage();
		}

		ClientJarRet client = new ClientJarRet(args[0], args[1],
				Integer.parseInt(args[2]));
		client.open();
		while(true){
			try {

				client.query();
			} catch (InterruptedException e) {
				client.close();
				System.err.println("InterruptedException ClientJarRet.query :"+ e);
				return;
			}catch(IOException e){
				client.close();
				System.err.println("IOException ClientJarRet.query :"+ e);
				return;
			}
		}

	}
}
