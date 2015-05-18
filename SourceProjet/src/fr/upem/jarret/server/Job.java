package fr.upem.jarret.server;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class Job {
	private final String JobId;
	private final String WorkerVersionNumber;
	private final String WorkerURL;
	private final String WorkerClassName;
	private final String JobTaskNumber;
	private final String JobDescription;
	private final String JobPriority; 
	private int currentTask; 
	private int currentPriority;
	private BitSet set;
	private boolean counted =false;
	
	public Job(String JobId, String WorkerV, String WorkerU, String WorkerC, String JobT,String JobD,String JobP) {
		this.JobId = JobId;
		this.WorkerVersionNumber = WorkerV;
		this.WorkerURL = WorkerU;
		this.WorkerClassName = WorkerC;
		this.JobTaskNumber = JobT;
		this.JobDescription = JobD;
		this.JobPriority = JobP;
		this.currentPriority = Integer.parseInt(JobP);
		this.currentTask = 1;
		this.set = new BitSet(Integer.parseInt(JobT));
	}
	
	
	
	public String getJobId() {
		return JobId;
	}
	public int nbTaskDone(){
		return set.cardinality();
	}
	public String getWorkerVersionNumber() {
		return WorkerVersionNumber;
	}
	public String getWorkerURL() {
		return WorkerURL;
	}
	public String getWorkerClassName() {
		return WorkerClassName;
	}
	public String getJobTaskNumber() {
		return JobTaskNumber;
	}		
	public String getJobPriority() {
		return JobPriority;
	}
	public String getJobDescription() {
		return JobDescription;
	}
	
	public int getCurrentPriority(){
		return currentPriority;
	}
	
	/**
	 *  Create a map representation of a Task.This method use the current Index and currentPriority intern of class Job
	 *  and auto increment/ decrement it.
	 * @return a Map<String,Object> representation of a task
	 */
	private Map<String, Object> taskMap(){
		HashMap<String, Object> map = new HashMap<>();
		map.put("JobId", JobId);
		map.put("WorkerVersion", getWorkerVersionNumber());
		map.put("WorkerURL", getWorkerURL());
		map.put("WorkerClassName", getWorkerClassName());
		map.put("Task", Integer.toString(currentTask));
		
		currentTask = set.nextClearBit(currentTask+1);
		if((currentTask >Integer.parseInt(JobTaskNumber)) && currentPriority>0){
			
			currentPriority--;
			currentTask = set.nextClearBit(0);
		}
		
		return map;
	}

	public Map<String,Object> getTask() {	
		return taskMap();	
	}
	/**
	 * set the bitSet index Task-1 at true only if it was false
	 * @param task int : task number
	 * @return true if the set was done, false if the bitSet index was already true
	 */
	public boolean updateBitSet(int task) {
		if(set.get(task-1)){
			return false;
		}
		set.set(task-1);
		return true;
	}
	
	/**
	 * Indicate if all the task are answered
	 * @return true if all task are done, false if not
	 */
	public boolean isDone(){
		if(set.cardinality() == Integer.parseInt(JobTaskNumber)){
			return true;
		}
		return false;
	}
	
	public boolean getCounted(){
		return counted;
	}
	
	/** 
	 * reset currentPriority and currentTask
	 */
	public void reset() {
		currentPriority = Integer.parseInt(JobPriority);
		currentTask =set.nextClearBit(0);
		
	}

	/**
	 * set the boolean counted at true, it used to indicate that the job was already counted in the 
	 * number of job done
	 */
	public void counted() {
		counted=true;
		
	}
}
