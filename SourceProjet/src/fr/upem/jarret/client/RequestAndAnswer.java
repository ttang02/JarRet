package fr.upem.jarret.client;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import upem.jarret.worker.Worker;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.upem.jarret.http.HTTPException;
import fr.upem.jarret.http.HTTPHeader;
import fr.upem.jarret.http.HTTPReader;
import fr.upem.jarret.worker.WorkerFactory;

public class RequestAndAnswer {
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private Map<String, Object> map = new HashMap<>();
	private final ByteBuffer bb = ByteBuffer.allocate(4096); 
	private final Worker worker;
	private String clientID;
	private final SocketChannel sc;
	
	public Worker getWorker(){
		return worker;
	}

	private RequestAndAnswer(Map<String, Object> map, Worker worker, String clientID, SocketChannel sc){
		this.map = map;
		this.worker = worker;
		this.clientID = clientID;
		this.sc = sc;
	}
	
	
	/**
	 * Create a new RequestAndAnswer with mapjSon, nameClientId, and SocketChannel
	 * @param jmap
	 * @param clientID
	 * @param sc
	 * @return RequestAndAnswer
	 * @throws IOException
	 */
	public static RequestAndAnswer createMapJson(Map<String,Object> jmap, String clientID, SocketChannel sc) throws IOException{
	
		String url=(String) jmap.get("WorkerURL");
		String className=(String) jmap.get("WorkerClassName");
		Worker worker;
		try {
			worker = (Worker) WorkerFactory.getWorker(url, className);

		}catch (ClassNotFoundException | IllegalAccessException
				| InstantiationException e) {
			throw new IOException("Invalid jar file");
		}
		return new RequestAndAnswer(jmap, worker, clientID, sc);
	}
	/**
	 * 
	 * @param json
	 * @param worker
	 * @param clientID
	 * @param sc
	 * @return
	 */
	public static RequestAndAnswer createMapJsonWithWorker(Map<String, Object> json, Worker worker,String clientID, SocketChannel sc){
		return new RequestAndAnswer(json, worker, clientID, sc);
	}
	
	/**
	 * Send the buffer result to server
	 * @throws IOException
	 */
	
	public void sendAnswer() throws IOException {
		bb.flip();
		sc.write(bb);
		bb.clear();
	}
	/**
	 * Read response from server after sending the post's buffer
	 * @throws IOException
	 */
	public void getAnswerAfterPost() throws IOException {
		HTTPReader reader = new HTTPReader(sc, bb);
		HTTPHeader header;
		try{
			header = reader.readHeader();
		} catch (HTTPException e) {
			System.err.println("Cannot read header");
			return;
		}
		int code = header.getCode();
		
		switch(code){
		case 200:
			System.out.println("Success");
			break;
		default:
			System.err.println("Error from server" + code);
		}
		
	}
	
	/**
	 * Execute compute with task from server, and fill a ByteBuffer
	 * @throws IOException
	 */
	public void compute() throws IOException{
		int taskNumber = Integer.parseInt((String) map.get("Task"));
		String result = null;
		try{
			result = worker.compute(taskNumber);
			
		} catch(Exception e){
			setBufferError("Computation error");
			return;
		}
		if(result == null){
			setBufferError("Computation error");
			return;
		}
		Map<String, String> map = new HashMap<String , String>();
		ObjectMapper mapper = new ObjectMapper();
		try{
			map = mapper.readValue(result,
					new TypeReference<HashMap<String, Object>>() {
					});
		}catch(JsonParseException e){
			setBufferError("Answer is not valid JSON"); 
			return;
		} catch(JsonMappingException e){
			setBufferError("Answer is nested");
			return;
		}
		
		setBufferAnswer(map);
		return;
		
	}
	/**
	 * Fill ByteBuffer with a error message
	 * @param errorMessage
	 * @throws IOException
	 */
	private void setBufferError(String errorMessage) throws IOException {
		ByteBuffer resultbb = constructPost("Error", errorMessage);
		addHTTPHeader(resultbb.position()+12);
		ByteBuffer res = ByteBuffer.allocate(Long.BYTES + Integer.BYTES +resultbb.position());
		res.clear();
		long job = Long.parseLong((String) map.get("JobId"));
		int task = Integer.parseInt((String) map.get("Task"));
		
		resultbb.flip();
		
		res.putLong(job).putInt(task).put(resultbb);
			
		res.flip();

		bb.put(res);
	}
	/**
	 * Fill ByteBuffer with a answer message 
	 * @param answer
	 * @throws IOException
	 */
	
	private void setBufferAnswer(Object answer) throws IOException {
		try{
			ByteBuffer resultbb = constructPost("Answer", answer);
			addHTTPHeader(resultbb.position()+12);
			ByteBuffer res = ByteBuffer.allocate(Long.BYTES + Integer.BYTES +resultbb.position());
			res.clear();
			long job = Long.parseLong((String) map.get("JobId"));
			int task = Integer.parseInt((String) map.get("Task"));
			resultbb.flip();
			res.putLong(job).putInt(task).put(resultbb);
			res.flip();

			bb.put(res);
		} catch(BufferOverflowException e){
			bb.clear();
			setBufferError("Too Long");
		}
	}
	/**
	 * Construct a response for the server
	 * @param key key for the Map
	 * @param msg Message for the Map
	 * @return
	 * @throws IOException
	 */
	private ByteBuffer constructPost(String key, Object msg) throws IOException{
		map.put("ClientId", clientID);
		map.put(key, msg);
		return encodePost();
	}
	/**
	 * Encode jsonMap in a new ByteBuffer
	 * @return
	 * @throws IOException
	 */
	private ByteBuffer encodePost() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ByteBuffer bb = UTF_8.encode(mapper.writeValueAsString(map));
		bb.compact();
		return bb;
	}
	/**
	 * Fill ByteBuffer with the header POST
	 * @param size
	 * @throws IOException
	 */
	private void addHTTPHeader(int size) throws IOException {
		String header = "POST Answer HTTP/1.1\r\n"+
						"Host: "+sc.getLocalAddress().toString() +
						"\r\nContent-Type: application/json\r\n"+
						"Content-Length: "+
						
						(size) + "\r\n\r\n";
		bb.put(ASCII.encode(header));

	}
	

}
