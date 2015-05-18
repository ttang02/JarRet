package fr.upem.jarret.client;

import java.util.Map;

public class ClientInfo {

	private final String jobId; 
	private final String version;
	

	private ClientInfo(String jobId, String version){
		this.jobId = jobId;
		this.version = version;
	}
	public static  ClientInfo createClient(Map<String, Object> jmap){
		String job = (String) jmap.get("JobId");
		String version = (String) jmap.get("WorkerVersion");
		return new ClientInfo(job,version);
	}
	
	
	public String getJobId(){
		return jobId;
	}
	public String getVersion(){
		return version;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobId == null) ? 0 : jobId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object c){
		if(c instanceof ClientInfo){
			ClientInfo tmp = (ClientInfo) c;
			return this.jobId.equals(tmp.jobId)  &&
					this.version.equals(tmp.version);
		}
		return false;
	}

}

