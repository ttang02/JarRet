package fr.upem.jarret.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerServer {

	private final Logger logInfo;
	private final Logger logError;

	private LoggerServer(Logger logInfo, Logger logError){
		this.logError = logError;
		this.logInfo = logInfo;
		
	}


	/**
	 * Create two log file, one for information and the other for the error, at the pathname "logPath"
	 * @param logPath Pathname of the two log file
	 * @return a LogerServer instance
	 */
	public static LoggerServer createLogger(String logPath){
		Logger logInfo = Logger.getLogger("Fichier Log Info");
		Logger logError = Logger.getLogger("Fichier log Erreur");
		File dossier = new File(logPath);
		FileHandler fh, fh1;
		try {  
			// This block configure the logger with handler and formatter  
			if(!dossier.exists()){
				dossier.mkdir();
			}
			
			fh = new FileHandler(dossier+"/logInfo.log");
			fh1 = new FileHandler(dossier+"/logError.log");
			// Set Simple Formattter 
			logInfo.addHandler(fh);
			logError.addHandler(fh1);
			fh.setFormatter(new SimpleFormatter());
			fh1.setFormatter(new SimpleFormatter());
			
			
			logError.setUseParentHandlers(false);
			logInfo.setUseParentHandlers(false);

		} catch (SecurityException e) {  
			e.printStackTrace();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}  
		return new LoggerServer(logInfo, logError);
	}
	public Logger getLoggerInfo(){
		return logInfo;
	}
	public Logger getLoggerError(){
		return logError;
	}
	
	/**
	 * Write an error in the logerrror file with the correspondant information
	 * @param error String representation of the error
	 * @param jobId long: the jobId
	 * @param numberTask int : the task id
	 * @param client the clientId
	 */
	public void logError(String error,long jobId, int numberTask, String client){
		String msgError = error+" :JobId :"+jobId+" Task : "+numberTask+" Client : "+client;
		logError.log(Level.SEVERE, msgError);

	}
	/**
	 *  write a warning in the logerror file with the corespondant information
	 * @param message String representation of the warning
	 * @param sourceClass  class
	 * @param method
	 * @param t throwable
	 */
	public void logWarning(String message, String sourceClass, String method, Throwable t){
		logError.logp(Level.WARNING, sourceClass, method, message, t);
	}
	/**
	 * write message in the loginfo file
	 * @param message String of the message 
	 */
	public void logInfos(String message){
		logInfo.log(Level.INFO,message);
	}
	






}
