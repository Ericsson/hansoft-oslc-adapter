package com.ericsson.eif.hansoft;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ericsson.eif.hansoft.configuration.util.CryptoUtil;
import com.ericsson.eif.hansoft.utils.StringUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Created by Matej Lajcak on 28/05/15.
 */
@XStreamAlias("Hansoft2HansoftConfig")
public class H2HConfig {

	private static final Logger logger = Logger.getLogger(H2HConfig.class.getName());
	
    private static H2HConfig INSTANCE = new H2HConfig();
    List<Friend> friendsList = new ArrayList<Friend>();
    List<Project> projectList = new ArrayList<Project>();

    /**
     *  Default private constructor
     */
    private H2HConfig() {
    }

    /**
     * Private Constructor
     * @param filename
     * @throws IOException
     */
    private H2HConfig(String filename) throws IOException {
        if (StringUtils.isEmpty(filename)) {
        	String errorMessage = "Name of file with H2H configuration is empty"; 
        	System.out.println(errorMessage);
        	logger.error(errorMessage);
        	return;
        }   	
    	        
    	BufferedReader reader = null;
    	try {
             reader = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
    	} catch(IOException e) {
    		logger.error("Cannot open file " + filename, e);
    		throw e;
    	}
    	
    	String xmlFromFile = new String();
    	String line = null;
    	try {   
	            while ((line = reader.readLine()) != null) {                
	                xmlFromFile = xmlFromFile + line;
	            }	            
    	} catch(IOException e) {
    		logger.error("Cannot read from file " + filename, e);
    		throw e;
    	}
    	
        XStream xStream = new XStream();
        xStream.alias("Hansoft2HansoftConfig", H2HConfig.class);
        xStream.autodetectAnnotations(true);
        
        H2HConfig.INSTANCE = (H2HConfig) xStream.fromXML(xmlFromFile);
        for(Friend friend: H2HConfig.getInstance().getFriendsList()) {
            try {
                friend.setPassword(CryptoUtil.deCrypt(friend.getPassword()));
            } catch (Exception e) {
                System.out.println("Password decrypting failed.");
                logger.error("Password decryption failed for friend " + friend.getFriendName(), e);
            }
        }
    }

    /**
     * Creates H2HConfig instance and initializes it from configuration file
     * @param fileName
     * @throws IOException
     */
    public static void getConfigFromFile(String fileName) throws IOException {
        new H2HConfig(fileName);
    }

    /**
     *  Creates new instance, resets all variables
     */
    public static void clearConfig() {
        H2HConfig.INSTANCE = new H2HConfig();
    }

    /**
     * @return singleton instance of H2HConfig
     */
    public static H2HConfig getInstance() {
        return INSTANCE;
    }

    /**
     * @return List of Friends from configuration file
     */
    public List<Friend> getFriendsList() {
        return friendsList;
    }

    /**
     * @param friendsList
     */
    public void setFriendsList(List<Friend> friendsList) {
        this.friendsList = friendsList;
    }

    /**
     * @return List of projects from configuration file
     */
    public List<Project> getProjectList() {
        return projectList;
    }

    /**
     * @param projectList
     */
    public void setProjectList(List<Project> projectList) {
        this.projectList = projectList;
    }
}
