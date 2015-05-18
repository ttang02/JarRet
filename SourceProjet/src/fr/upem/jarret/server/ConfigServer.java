package fr.upem.jarret.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigServer {
	private final int port;
	private final String LogsPath;
	private final String AnswersPath;
	private final int maxFileSize;
	private final long comeback;

	private ConfigServer(int port, String logs, String answers, int maxFileSize, long comeback){
		this.port = port;
		this.LogsPath = logs;
		this.AnswersPath = answers;
		this.maxFileSize = maxFileSize;
		this.comeback = comeback;
	}
	public int getPort() {
		return port;
	}

	public String getDirectorylogs() {
		return LogsPath;
	}

	public String getDirectoryAnswers() {
		return AnswersPath;
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}
	public long getComeback() {
		return comeback;
	}
	
	/**
	 * Parse the fileConfigjSon and create a ConfigServer used to configure Server parameter 
	 * @param fileConfigjSon Pathname of the file configuration
	 * @return a ConfigServer instance
	 */
	public static ConfigServer createConfigServer(String fileConfigjSon){
		Map<String, String> json = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			json = mapper.readValue(new File(fileConfigjSon),
					new TypeReference<HashMap<String, String>>() {
					});
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int port = Integer.parseInt(json.get("Port"));
		String logs = json.get("LogsPath");
		String answers = json.get("AnswersPath");
		int maxFileSize = Integer.parseInt(json.get("MaxFileSize"));
		long comeback = Long.parseLong(json.get("Comeback"));
		
		return new ConfigServer(port, logs, answers, maxFileSize, comeback);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Port: ")
		.append(port)
		.append("\n")
		.append("LogsPath: ")
		.append(LogsPath)
		.append("\n")
		.append("AnswersPath: ")
		.append(AnswersPath)
		.append("\n")
		.append("maxFileSize: ")
		.append(maxFileSize)
		.append("\n")
		.append("ComeBack: ")
		.append(comeback)
		.append("\n");
		return sb.toString();
	}
	
}

