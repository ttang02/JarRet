package fr.upem.jarret.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ListJob {
	private final ArrayList<Job> jobs;
	private int numberOfJobDone;
	private final ArrayList<File> filesAnswer;
	
	private ListJob(ArrayList<Job> jobs) {
		this.jobs = jobs;
		this.numberOfJobDone = 0;
		this.filesAnswer = new ArrayList<>(jobs.size());
	}

	/**
	 *  create an instance of ListJob by parsing a file configuration
	 * @param file Pathname of the file containing all the job information
	 * @return a ListJob instance
	 */
	public static ListJob createFileToJobs(String file) {
		StringBuilder str = new StringBuilder();
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				str.append(line + "\n");
			}
		} catch (IOException e) {
			System.err.println("Exception File failed");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				System.err.println("Error BufferedReader");
			}
		}
		// Parsing Json
		Map<String, Object> json = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();

		ArrayList<Job> listjobs = new ArrayList<>();
		String[] tasks = str.toString().split("\n\n");

		for (int i = 0; i < tasks.length; i++) {
			try {
				json = mapper.readValue(tasks[i],
						new TypeReference<HashMap<String, Object>>() {
						});
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String jobId = (String) json.get("JobId");
			String jobTask = (String) json.get("JobTaskNumber");
			String jobPriority = (String) json.get("JobPriority");
			String WorkerV = (String) json.get("WorkerVersionNumber");
			String WorkerC = (String) json.get("WorkerClassName");
			String WorkerU = (String) json.get("WorkerURL");
			String JobDescription = (String) json.get("JobDescription");

			if (Integer.parseInt(jobPriority) > 0) {
				listjobs.add(new Job(jobId, WorkerV, WorkerU, WorkerC, jobTask,
						JobDescription, jobPriority));
			}
		}
		return new ListJob(listjobs);
	}

	/**
	 * return true if there is no more job available
	 * @return boolean
	 */
	public boolean isDone(){
		return jobs.size()==(numberOfJobDone);
	}
	
	/**
	 * poll a job in the list of job with the current highest Priority
	 * @return a Job instance with the current Task and highest priority
	 */
	private Job getHighestPriority() {
		int jobIndex = jobs.get(0).getCurrentPriority();
		Job jobHighestPriority = jobs.get(0);
		for (Job job : jobs) {
			if (job.getCurrentPriority() > jobIndex && !job.isDone()) {
				jobIndex = job.getCurrentPriority();
				jobHighestPriority = job;
			}
		}
		/* Si tout les jobs on au moins étaient envoyé une fois mais que le serveur continue
		 * il y a eu des réponses au tache donné perdu, on reset
		 * NB: l'index currentTask de job saute les task dont les réponses sont connue grace au bitset
		 *     , pas de risque de double envoie 
		*/
		if(jobIndex == 0){
			for (Job job : jobs) {
				if(job.isDone())
					job.reset();
			}
			jobIndex = jobs.get(0).getCurrentPriority();
			jobHighestPriority = jobs.get(0);
			for (Job job : jobs) {
				if (job.getCurrentPriority() > jobIndex && !job.isDone()) {
					jobIndex = job.getCurrentPriority();
					jobHighestPriority = job;
				}
			}
		}
		return jobHighestPriority;
	}

	/**
	 * create all the Files where Job answer will be write
	 * @param dossier Pathname of the Directory where all File Job are
	 * @throws IOException
	 */
	public void createFiles(String dossier) throws IOException{
		int cpt=0;
		File f = new File(dossier);
		 if(!f.exists())
		   f.mkdir();
		 
		for(Job job : jobs){
			
			 File file = new File(dossier+"/"+job.getJobId()+".txt");	
		     file.createNewFile(); 
		     FileWriter fw = new FileWriter(file);
		     fw.write("JobId: "+job.getJobId()+"\nDescription: "+job.getJobDescription()+"\nURL: "+job.getWorkerURL()+"\nVersion: "+job.getWorkerVersionNumber()+"\n\n");
		     fw.close();
		     filesAnswer.add(cpt++, file);
		}
	}

	/**
	 * Get a task, the Task will have the current highest priority
	 * @return a String representation of a Task
	 */
	public String getTask() {
		Job job = getHighestPriority();

		Map<String, Object> mjson = job.getTask();
	
		ObjectMapper mapper = new ObjectMapper();
		String task = "";
		try {
			task = mapper.writeValueAsString(mjson);
		} catch (JsonProcessingException e) {
			System.err.println("ListJob getTask mapper.writeValueAsString :"
					+ e);
		}
		return task;
	}

	public ArrayList<Job> getJobs() {
		return jobs;
	}

	/**
	 * Update the bitSet of the Job at task indices, and write the the answer in file
	 * @param jobId long representation of the jobId
	 * @param task int representation of the task
	 * @param Answer String representation of the Answer a client send
	 * @return
	 */
	public boolean update(long jobId, int task,String Answer) {
		int cpt=0;
		for (Job job : jobs) {
			if (Long.parseLong(job.getJobId()) == jobId) {
				if(job.updateBitSet(task)){
					
					try {
						FileWriter fw = new FileWriter(filesAnswer.get(cpt),true);
						fw.write(task +": "+Answer+"\n");
						fw.close();
						
					} catch (IOException e) {
						System.err.println("filewriter.write answer :"+e);
					}
				}
				if(job.isDone()){
					// don't count two time the same Job for the numberOfJobDone
					if(!job.getCounted()){
						numberOfJobDone++;
						job.counted();
						return true;
					}
				}
			}
			cpt++;
		}
		return false;

	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Info:");
		for(Job job : jobs){
			sb.append("JobId : ").append(job.getJobId())
			.append("\n Description : ").append(job.getJobDescription())
			.append("\n WorkerURL : ").append(job.getWorkerURL())
			.append("\n Number of task done: ").append(job.nbTaskDone()).append("/").append(job.getJobTaskNumber()).append("\n");
		}
		
		return sb.toString();
	}
}
