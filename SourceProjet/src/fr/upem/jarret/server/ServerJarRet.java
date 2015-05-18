package fr.upem.jarret.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.upem.jarret.http.HTTPException;
import fr.upem.jarret.http.HTTPHeader;
import fr.upem.jarret.http.HTTPReader;

public class ServerJarRet {

	private ListJob listJob;
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final Charset ASCII = Charset.forName("ASCII");
	private static final int BUFFER_SIZE = 4096;
	private static long COMEBACK;
	private Info info;
	private ConfigServer config;
	private LoggerServer logs;
	private boolean accept = true;

	static class Info {
		private int numberOfClient = 0;

		private static String stateOfJob(ListJob listJob) {
			return listJob.toString();
		}

		public void addClient() {
			numberOfClient++;
		}

		public void subClient() {
			numberOfClient--;
		}

		public String toString(ListJob listJob) {
			StringBuilder sb = new StringBuilder();
			sb.append("Number of client :").append(numberOfClient).append("\n")
					.append(stateOfJob(listJob));
			return sb.toString();
		}
	}

	static class ClientResquest {

		ByteBuffer bb = ByteBuffer.allocate(4096);

		public ByteBuffer getByteBuffer() {
			return bb;
		}

		public void setBuffer(ByteBuffer buffer) {
			this.bb = buffer;
		}

	}

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;

	/***
	 * Constructor of Server
	 * 
	 * @param port
	 *            port connected
	 * @param fileJob
	 *            pathname of the list of job
	 * @param fileconfig
	 *            pathname of the file server configuration
	 * @throws IOException
	 */
	public ServerJarRet( String fileJob, String fileconfig)
			throws IOException {
		this.config = ConfigServer.createConfigServer(fileconfig);
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(config.getPort()));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		this.listJob = ListJob.createFileToJobs(fileJob);
		
		this.logs = LoggerServer.createLogger(config.getDirectorylogs());
		COMEBACK = config.getComeback();
		this.info = new Info();
		listJob.createFiles(config.getDirectoryAnswers());
	}

	/**
	 * launch the server
	 * 
	 * @throws IOException
	 */
	public void launch() throws IOException {
		System.out
				.println("Server launched on port "+config.getPort()+"\n waiting for client ...");
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {

			selector.select();
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable() && accept) {
				try {
					doAccept(key);
				} catch (IOException e) {
					// Serveur problem
					System.err
							.println("IOException doAccept Server encounteur a problem :"
									+ e);
					return;
				}
			}
			try {
				if (key.isValid() && key.isWritable()) {
					doWrite(key);
				}
				if (key.isValid() && key.isReadable()) {

					doRead(key);
				}
			} catch (HTTPException e) {
				SocketChannel sc = (SocketChannel) key.channel();
				silentlyClose(sc);
				key.cancel();
				info.subClient();
				logs.logInfos("A client disconected");
			} catch (IOException e) {
				SocketChannel sc = (SocketChannel) key.channel();
				silentlyClose(sc);
				key.cancel();
				info.subClient();
				logs.logInfos("A client disconected");
			}
		}

	}

	/**
	 * silently close a socket
	 * @param socket2
	 */
	private void silentlyClose(SocketChannel socket2) {
		if (socket2 != null)
			try {
				socket2.close();
			} catch (IOException e) {
				// Ignore
			}

	}

	/**
	 * Accept method a client, attach a ClientRequest Object at the key
	 * @param key the SelectionKey
	 * @throws IOException
	 */
	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc = serverSocketChannel.accept();
		if (sc == null)
			return;

		System.out.println("Accept");
		sc.configureBlocking(false);
		ClientResquest client = new ClientResquest();
		sc.register(selector, SelectionKey.OP_READ, client);
		info.addClient();
		logs.logInfos("A new client connected");
	}

	/**
	 * Read byte received, two cases: 
	 *   -read a "GET TASK": fill the buffer to send with a task selectioned in listJob and go in write mode
	 *   -read a "POST ANSWER" : fill the buffer to send with an good or bad request to the client ,if an error is read
	 *                           write it in the logerror, and if an answer is read we update the listJob(update the bitset and 
	 *                           write the answer in the job file associated) 
	 * 
	 * @param key th Selection Key
	 * @throws IOException
	 * @throws HTTPException
	 */
	private void doRead(SelectionKey key) throws IOException, HTTPException {
		SocketChannel sc = (SocketChannel) key.channel();
		if (sc == null)
			return;

		ClientResquest client = (ClientResquest) key.attachment();
		ByteBuffer bb = client.getByteBuffer();

		HTTPReader reader = new HTTPReader(sc, bb);
		HTTPHeader header = reader.readHeaderServer();
		String request = header.getResponse();

		if (request.equals("GET Task HTTP/1.1")) {

			if (listJob.isDone()) {

				bb = computeWait();
				client.setBuffer(bb);
				logs.logInfos("ALL JOB DONE !!!!");

			} else {
				String task = listJob.getTask();
				bb = computeTask(task);
				client.setBuffer(bb);
			}

		} else if (request.equals("POST Answer HTTP/1.1")) {

			bb = reader.readBytes(header.getContentLength());
			bb.flip();
			long jobId = bb.getLong();
			int taskNumber = bb.getInt();
			String answer = UTF_8.decode(bb).toString();

			Map<String, Object> mapAnswer = stringToJson(answer);
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			if (mapAnswer.containsKey("Error")) {
				// send bad request
				buffer = computeError();
				logs.logError((String) mapAnswer.get("Error"), jobId,
						taskNumber, (String) mapAnswer.get("ClientId"));
			} else {
				// update bitset of the Job and write answer in file
				if (listJob.update(jobId, taskNumber,
						(String) mapAnswer.get("Answer").toString())) {
					logs.logInfos("JobId : " + (String) mapAnswer.get("JobId")
							+ " is done");
				}

				// write in associated file

				buffer = computeOKPost();

			}
			client.setBuffer(buffer);

		} else {
			bb = computeError();
			client.setBuffer(bb);

		}
		key.attach(client);
		key.interestOps(SelectionKey.OP_WRITE);
	}
	/**
	 * convert a string in json format to a Map
	 * @param json String in json syntax
	 * @return
	 */
	private static Map<String, Object> stringToJson(String json) {
		Map<String, Object> jmap = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();

		try {
			jmap = mapper.readValue(json,
					new TypeReference<HashMap<String, Object>>() {
					});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jmap;
	}

	/**
	 * send to the client the buffer in the object attached to key (buffer is already filled in doRead)
	 * @param key the SelectionKey
	 * @throws IOException
	 */
	private void doWrite(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		if (sc == null)
			return;
		ClientResquest client = (ClientResquest) key.attachment();
		ByteBuffer bb = client.getByteBuffer();

		sendBuffer(sc, bb);
		client.setBuffer(bb);
		key.attach(client);
		key.interestOps(SelectionKey.OP_READ);

	}

	/**
	 * Send the buffer "bb" with the socketChannel "sc"
	 * @param sc the socket channel
	 * @param bb the ByteBuffer in write mode
	 * @throws IOException
	 */
	private void sendBuffer(SocketChannel sc, ByteBuffer bb) throws IOException {
		if (bb.position() != 0)
			bb.flip();
		while (bb.hasRemaining()) {
			sc.write(bb);
		}
		bb.clear();
	}

	/**
	 * return a ByteBuffer filled with the task polled in HTTP/1.1 format
	 * @param task String representation the Content of the Task
	 * @return a ByteBuffer with a task in write mode
	 */
	private static ByteBuffer computeTask(String task) {
		ByteBuffer bbField = UTF_8.encode(task);
		String head = "HTTP/1.1 200 OK\r\n"
				+ "Content-Type: application/json; charset=utf-8\r\n"
				+ "Content-Length: " + bbField.remaining() + "\r\n\r\n";
		ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		bb.put(ASCII.encode(head)).put(bbField);
		return bb;

	}

	/**
	 * return a ByteBuffer filled with the badRequest in HTTP/1.1 format
	 * 
	 * @return a ByteBuffer bad request in write mode
	 */
	private static ByteBuffer computeError() {
		String messageError = "HTTP/1.1 400 Bad Request\r\n\r\n";
		return ASCII.encode(messageError);
	}
	/**
	 * return a ByteBuffer filled with the comeback wait for client in HTTP/1.1 format
	 * 
	 * @return a ByteBuffer a comback request in write mode
	 */
	private static ByteBuffer computeWait() {
		String comeback = "{\n   \"ComeBackInSeconds\" : \"" + COMEBACK
				+ "\"\n}";
		ByteBuffer bbField = UTF_8.encode(comeback);
		String head = "HTTP/1.1 200 OK\r\n"
				+ "Content-Type: application/json; charset=utf-8\r\n"
				+ "Content-Length: " + bbField.remaining() + "\r\n\r\n";
		ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		bb.put(ASCII.encode(head)).put(bbField);
		return bb;
	}

	/**
	 * return a ByteBuffer filled with the Good Request in HTTP/1.1 format
	 * 
	 * @return a ByteBuffer good request in write mode
	 */
	private static ByteBuffer computeOKPost() {
		String messageOK = "HTTP/1.1 200 OK\r\n\r\n";
		return ASCII.encode(messageOK);
	}
	
	/**
	 * Write Information
	 * @return a String representation of the information
	 */
	private String info() {
		return info.toString(listJob);
	}

	/**
	 *  shutdown called to sever server right now
	 * @throws IOException
	 */
	private void shutdownNow() throws IOException {
		serverSocketChannel.close();
	}

	/**
	 *  block any future Acception
	 * @throws IOException
	 */
	private void shutdown() throws IOException {
		accept = false;
	}
	private static void usage() {
		System.out.println("ClientJarRet jobsFile configServer");
	}

	
	public static void main(String[] args) throws NumberFormatException,
			IOException {
		if (args.length != 2) {
			usage();
		}
		final ServerJarRet server = new ServerJarRet(args[0],args[1]);
		
		
		Runnable r1 = new Runnable() {
			@Override
			public void run() {
				try {
					server.launch();
				} catch (IOException e) {
				}
			}
		};

		Thread t = new Thread(r1);
		t.start();

		try (Scanner scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("INFO")) {
					System.out.println(server.info());
				} else if (line.equals("SHUTDOWN NOW")) {
					System.out.println("command: SHUTDOWN NOW");
					server.shutdownNow();
					t.interrupt();
					break;
				} else if (line.equals("SHUTDOWN")) {
					System.out.println("command: SHUTDOWN");
					server.shutdown();
				}
			}
		}
		System.out.println("EXIT SERVER");

	}
}