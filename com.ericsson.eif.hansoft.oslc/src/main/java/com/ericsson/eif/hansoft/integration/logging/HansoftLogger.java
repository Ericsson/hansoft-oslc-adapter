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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.utils.OSLCUtils;

/**
 * Class for logging transfer of hansoft change requests
 * @author lukas svacina
 */
public class HansoftLogger {

	private static final Logger logger = Logger.getLogger(HansoftLogger.class.getName());

	private static HansoftLogger instance = null;	
	
    /**
     * Default constructor
     */
    private HansoftLogger() {    	
    	// initialize class members here    	
    }    
    
    /**
     * @return single instance of hansoft logger
     */
    public static HansoftLogger getInstance() {
        if (instance == null)
        	instance = new HansoftLogger();        
        
        return instance;
    }    
   
    /**
     * creates one record
     * @param hcr
     */
    public void log(HansoftChangeRequest hcr) {
    	String rdfData = "";
		try {			
			rdfData = OSLCUtils.createRDFData(hcr);			
		} catch (IOException e) {
			logger.error("Failed to createRDFData from HansoftChangeRequest for HansoftLogger: ", e);
		}

		this.log(rdfData + Constants.LS);
    }
    
    /**
	 * creates one record
	 * @param content
	 */
	public void log(String content) {		
		String hansoftLogDir = HansoftManager.getHansoftLogDir();
		if (StringUtils.isEmpty(hansoftLogDir)) {
			hansoftLogDir = HansoftManager.adapterServletHome;
			logger.info("hansoft_log_dir is not set in adapter.properties");
			logger.info("using default path " + hansoftLogDir);
		}
		
		String hansoftLogPath = hansoftLogDir + File.separator + "HansoftLog.log";
		
		File hansoftLogFile = new File(hansoftLogPath);
    	FileWriter fw = null;
    	
		// if log file does not exist, create it
		if (!hansoftLogFile.exists()) {
    		try {
    			hansoftLogFile.createNewFile();
			} catch (IOException e) {				
            	logger.error("Failed to create hansoft log file : " + hansoftLogPath, e);
			}
    	}
		
		// open file for writing to end of file
		try {
			fw = new FileWriter(hansoftLogFile, true);
			content = this.addCurrentDate(content);
			fw.write(content);			
			fw.flush();
			fw.close();
		} catch (IOException e) {
			logger.error("Failed to open hansoft log file : " + hansoftLogPath, e);
		}
	}
	
	/** 
	 * @param content
	 * @return log entry starting with current date and time
	 */
	private String addCurrentDate(String content) {
    	// add current date, time and line separators to content
		String currentDateTime = new SimpleDateFormat(Constants.DATE_TIME_FORMAT).format(new Date()).toString();
		content = currentDateTime + " " + content + Constants.LS;
		return content;
    }
}