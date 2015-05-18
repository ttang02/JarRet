package fr.upem.jarret.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.upem.jarret.http.HTTPHeader;
import fr.upem.jarret.http.HTTPReader;

public class RequestTask {
	private final SocketChannel sc;
	private final String nameServer;
	private final Charset CharsetASCII = Charset.forName("ASCII"); 
	
	public RequestTask(SocketChannel sc,String nameServer) throws IOException{
		this.sc = sc;
		this.nameServer = nameServer;
	}
	/**
	 * get TimeOut from the Map
	 * @param jmap
	 * @return timeout
	 */
	public static int getTimeOut(Map<String,Object> jmap){
		
		String seconds=(String)jmap.get("ComeBackInSeconds");
		if(seconds==null){
			return 0;
		}
		return Integer.parseInt(seconds);
		
	}
	/***
	 * Send a request to get a Task to the server ,and return the JSON
	 * @return a Json as String
	 * @throws IOException
	 */
	public Map<String,Object> getJson() throws IOException{
		String request = "GET Task HTTP/1.1\r\n"
				+ "Host: *** "+nameServer+" ***\r\n"
				+"\r\n";
		sc.write(CharsetASCII.encode(request));
		
		ByteBuffer bb = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc, bb);
		HTTPHeader header = reader.readHeader();
		
		int content_length = header.getContentLength();
		Charset charset = header.getCharset();
		ByteBuffer buffer = reader.readBytes(content_length);
		buffer.flip();
		String json = charset.decode(buffer).toString();
		
		Map<String, Object> jmap = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();

		try {
			jmap = mapper.readValue(json, 
					new TypeReference<HashMap<String, Object>>(){				
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
}
