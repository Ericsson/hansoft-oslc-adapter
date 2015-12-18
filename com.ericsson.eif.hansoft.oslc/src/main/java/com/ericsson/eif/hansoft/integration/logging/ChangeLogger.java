package com.ericsson.eif.hansoft.integration.logging;

/*
* Copyright (C) 2015 Ericsson AB. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer
*    in the documentation and/or other materials provided with the
*    distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;

/**
 * Class for logging changes of Hansoft attributes
 * @author lukas svacina
 */
public class ChangeLogger {

	private static final Logger logger = Logger.getLogger(ChangeLogger.class.getName());
	
	public String fromProjectName;
	public String toProjectName;
    public String taskId;
    public String taskName;
    public String username;       
    public ConnectionType connectionType;        
    public List<ChangeLogItem> changeLogItems;
    
	private final String csvColumnSeparator = ",";
	
	/**
	 *  Default constructor
	 */
	public ChangeLogger() {		
		this.clear();
	}

	/**
     *  Sets all class members to initial state
     */
    public void clear() {
    	this.fromProjectName = "Unknown";
    	this.toProjectName = "Unknown";
    	this.taskId = "Unknown";
    	this.taskName = "Unknown";
    	this.username = "Unknown";    	
    	this.connectionType = ConnectionType.Unknown;
    	this.changeLogItems = new ArrayList<ChangeLogItem>();
    }
    
    /**
     * Setup class attributes
     * 
     * @param fromProjectName
     * @param toProjectName
     * @param taskId
     * @param username
     * @param connectionType
     */
    public void setup(String fromProjectName, String toProjectName, HPMUniqueID taskId, String username, ConnectionType connectionType) {
 	   this.fromProjectName = fromProjectName;
 	   this.toProjectName = toProjectName;
 	   
 	   // if we setup change logger before creation of new task in hansoft, we do not know task Id yet and null is correct value
 	   if (taskId != null) {
 		   this.taskId = taskId.toString();
 		  
 		   try {
 	 		   this.taskName = this.getTaskName(HansoftManager.getMainSession(), taskId);
 			} catch (HPMSdkException | HPMSdkJavaException e) {
 				logger.error("Error getting of task name", e);
 				logger.debug("Using default task name Unknown"); 				
 			}
 	   }
 	   
 	   this.username = username; 	   
 	   this.connectionType = connectionType;
    }
    
    /**
     * Sets synchronisation status
     * @param synchronisationStatus
     */
    public void setSynchronisationStatus(SynchronisationStatus synchronisationStatus){   	 
    	for(ChangeLogItem cli : this.changeLogItems){
    		cli.synchronisationStatus = synchronisationStatus;
    	}    
    }    
    
    /**
     * Sets taskId to all items in collection
     * @param taskId
     */
    public void setTaskId(String taskId) {   	 
    	this.taskId = taskId;
    	
    	for(ChangeLogItem cli : this.changeLogItems){
    		cli.taskId = taskId;
    	}
    }
    
    /**
     * Adds to change log collection one change log item.
     * @param columnName
     * @param oldObject
     * @param newObject
     */
    public void log (String columnName, Object oldObject, Object newObject) {    	
    	String oldValue = "null";
    	if (oldObject != null)
    		oldValue = oldObject.toString();
    	
    	String newValue = "null";
    	if (newObject != null)
    		newValue = newObject.toString();
    	
    	log(columnName, oldValue, newValue);
    }
    
	/**
	 * Adds to change log collection one change log item.
	 * @param columnName
	 * @param oldValue
	 * @param newValue
	 */
	public void log(String columnName, String oldValue, String newValue) {	
	   DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
	   String modifiedDate = dateFormat.format(new Date()); 
	   
	   DateFormat timeFormat = new SimpleDateFormat(Constants.TIME_FORMAT);		
	   String modifiedTime = timeFormat.format(new Date());
		   
	   ChangeLogItem cli = new ChangeLogItem(this.fromProjectName, this.toProjectName, 
			   this.taskName, this.taskId, this.username, 
			   				modifiedDate, modifiedTime, columnName, oldValue, newValue, this.connectionType);
	   
	   this.changeLogItems.add(cli);
	}
			
	/**
	 *  Creates a CSV file with change logs
	 *  Directory where CSV file is taken from adapter.properties from hansoft_change_log_dir property
	 *  Keep method thread safe as change loggers for incoming and outgoing changes share one CSV file. 
	 */
	public synchronized void writeToCsv() {	
		String changeLogDir = HansoftManager.getChangeLogDir();
		
		if (StringUtils.isEmpty(changeLogDir)) {
			changeLogDir = HansoftManager.adapterServletHome;
			logger.info("hansoft_change_log_dir is not set in adapter.properties");
			logger.info("using default path " + changeLogDir);
		}
		
		String changeLogPath = changeLogDir + File.separator + "ChangeLog.csv";
		
		File changeLogFile = new File(changeLogPath);
    	FileWriter fw = null;
    	
		// if log file does not exist, create it and write there CSV header
		if (!changeLogFile.exists()) {
    		try {
				changeLogFile.createNewFile();
				
				fw = new FileWriter(changeLogFile);
				String header = this.createCsvHeader();
				fw.write(header);
				fw.flush();
				fw.close();
			} catch (IOException e) {
            	logger.error("Failed to create change log CSV file : " + changeLogPath, e);
			}
    	}
		
		// open file for writing to end of file
		try {
			fw = new FileWriter(changeLogFile, true);
			String content = this.createCsvContent();
			fw.write(content);
			fw.flush();
			fw.close();
			
			// clean change log collection after writing to file
			this.changeLogItems.clear();
			
		} catch (IOException e) {
			logger.error("Failed to open change log CSV file : " + changeLogPath, e);
		}
	}

    /**
     * creates a header of CSV file - name of columns separated by ;
     * @return header 
     */
    private String createCsvHeader() {
    	
		StringBuilder sb = new StringBuilder();
		sb.append("From Project");
		sb.append(this.csvColumnSeparator);
		
		sb.append("To Project");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Task Name");
		sb.append(this.csvColumnSeparator);

		sb.append("Task Database Id");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Username");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Date");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Time");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Column Name");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Old Value");
		sb.append(this.csvColumnSeparator);
		
		sb.append("New Value");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Connection Type");
		sb.append(this.csvColumnSeparator);
		
		sb.append("Synchronization status");
		sb.append(Constants.LS);
		    	
    	return sb.toString();
	}
	
    /**
     * creates a log of changed attributes in csv format 
     * @return change log
     */
    private String createCsvContent() {
	   if (this.changeLogItems == null || this.changeLogItems.isEmpty())	   
		   return "";
	   
	   StringBuilder sb = new StringBuilder();
	   for (ChangeLogItem cli : changeLogItems) {
		   sb.append(cli.fromProjectName);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.toProjectName);
		   sb.append(this.csvColumnSeparator);		   
		   
		   sb.append(cli.taskName);
		   sb.append(this.csvColumnSeparator);		   
		   
		   sb.append(cli.taskId);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.username);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.modifiedDate);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.modifiedTime);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.columnName);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.oldValue);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.newValue);
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.connectionType.toString());
		   sb.append(this.csvColumnSeparator);
		   
		   sb.append(cli.synchronisationStatus.toString());
		   sb.append(Constants.LS);		   
	   }
	   
	   return sb.toString();	   
	}
   
    /** gets task name
     * @param session
     * @param taskId
     * @return task name or Unknown in case of error
     */
    private String getTaskName(HPMSdkSession session, HPMUniqueID taskId) {
    
    	String taskName = "Unknown";
    	
    	if (session == null) {
			logger.error("Error while getting task name for task id." + taskId.toString());
			logger.debug("parameter session is null.");
			logger.debug("using default task name " + taskName);
			return taskName;
		}
    	
    	try {
			taskName = session.TaskGetDescription(taskId);
		} catch (Exception e) {
			logger.error("Error while getting task name for task id." + taskId.toString());	
			logger.debug("using default task name " + taskName);
		}   
    	
    	return taskName;
    }
}