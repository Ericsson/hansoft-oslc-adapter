package com.ericsson.eif.hansoft.configuration.util;

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
import java.util.ArrayList;
import java.util.List;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;

public class H2HValidationLogger {

	public List<H2HValidationLogItem> validationItems;
	public boolean writeToFile;
	public boolean writeToScreen;
	
	private static H2HValidationLogger instance = null;	
	
    /**
     * Default constructor
     */
    private H2HValidationLogger() {    	
    	// initialize class members here
    	this.validationItems = new ArrayList<H2HValidationLogItem>();
    	this.writeToFile = true;
    	this.writeToScreen = true;
    }
    
    /**
     * @return single instance of H2HValidationLogger
     */
    public static H2HValidationLogger getInstance() {
        if (instance == null)
        	instance = new H2HValidationLogger();
        
        return instance;
    }    

    /**
     * Write one line to log file
     * @param logItemType
     * @param message
     */
    public void log(H2HValidationLogItemType logItemType, String message) {
		H2HValidationLogItem item = new H2HValidationLogItem(logItemType, message);
		this.validationItems.add(item);
	}	
	    
    /** 
     * Create one line of Info message 
     * @param message
     */
    public void info(String message) {    	
    	H2HValidationLogItem item = new H2HValidationLogItem(H2HValidationLogItemType.Info, message);
		this.validationItems.add(item);
    }
    
    /**
     * Create one line of Warning message
     * @param message
     */
    public void warning(String message) {    	
    	H2HValidationLogItem item = new H2HValidationLogItem(H2HValidationLogItemType.Warning, message);
		this.validationItems.add(item);
    }
    
    /**
     * Create one line of Error message
     * @param message
     */
    public void error(String message) {    	
    	H2HValidationLogItem item = new H2HValidationLogItem(H2HValidationLogItemType.Error, message);
		this.validationItems.add(item);
    }
    
    /**
     * Writes output to file or on screen
     * based on configuration of properties
     * 
     * h2h_config_validation_output_to_file 
     * h2h_config_validation_output_on_screen 
     * 
     *  in adapter.properties file   
     */
    public void writeOutput() {
    	
    	if (this.writeToScreen == true) {
    		this.writeToScreen();
    	}
    	
    	if (this.writeToFile == true) {
    		this.writeToFile();
    	}    	
    }
    
	/**
	 * Writes output of validation to H2HConfog.log file
	 * and stores in on same place as H2HConfig.xml 
	 */
	public void writeToFile() {
		String adapterServletHome = HansoftManager.getAdapterServletHome();
		String validationLogPath = adapterServletHome + File.separator + "H2HConfig.log";
		
		File validationLog = new File(validationLogPath);
		FileWriter fw = null;
		
		// if log file does not exist, create it
		if (!validationLog.exists()) {
			try {
				validationLog.createNewFile();
			} catch (IOException e) {        	
			}
		}
		
		// overwrite file content
		try {
			fw = new FileWriter(validationLog, false);
			String content = this.createContent();
			fw.write(content);
			fw.flush();
			fw.close();
			
			// clean collection after writing to file
			this.validationItems.clear();
		} catch (IOException e) {			
		}
	}
	
	/**
	 *  Writes output of validation on screen
	 */
	public void writeToScreen() {
		System.out.println(this.createContent());
	}
	
	/**
	 * Creates one string message from collection of validation log items
	 * @return
	 */
	private String createContent() {
		if (this.validationItems == null || this.validationItems.isEmpty())	   
			   return "";
		
		StringBuilder sb = new StringBuilder();		
		for (H2HValidationLogItem item : this.validationItems) {

			// write tyoe of log item only for warning and error
			if (item.logItemType != H2HValidationLogItemType.Info) {
				sb.append("[");
				sb.append(item.logItemType);
				sb.append("] ");
			}
				
			sb.append(item.message);
			sb.append(Constants.LS);
		}
		
		return sb.toString();
	}
}
