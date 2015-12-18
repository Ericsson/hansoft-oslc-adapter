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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Created by matejlajcak on 02/06/15.
 */
@XStreamAlias("project")
public class Project {

	private String id;
    private String server;
    private String database;
    private String projectName;
    private String projectUserName;
    private IntegrationLinks integrationLinks = new IntegrationLinks();
	List<Sync> syncList = new ArrayList<Sync>();

    /**
     * Default constructor
     */
    public Project() {    	
    }

    /**
     * Constructor
     * @param projectName
     * @param manualTriggerColumnName
     */
    public Project(String projectName, String manualTriggerColumnName) {
        this.projectName = projectName;
        this.integrationLinks.manualTriggerColumnName = manualTriggerColumnName;
    }

    /**
     * Constructor
     * @param projectName
     * @param manualTriggerColumnName
     * @param autoSyncColumnName
     */
    public Project(String projectName, String manualTriggerColumnName, String autoSyncColumnName) {
        this.projectName = projectName;
        this.integrationLinks.manualTriggerColumnName = manualTriggerColumnName;
        this.integrationLinks.autoSyncColumnName = autoSyncColumnName;
    }

    /**
     * Constructor
     * @param projectName
     * @param manualTriggerColumnName
     * @param autoSyncColumnName
     * @param backlinkColumnName
     */
    public Project(String projectName, String manualTriggerColumnName, String autoSyncColumnName, String backlinkColumnName) {
        this.projectName = projectName;
        this.integrationLinks.manualTriggerColumnName = manualTriggerColumnName;
        this.integrationLinks.autoSyncColumnName = autoSyncColumnName;
        this.integrationLinks.backlinkColumnName = backlinkColumnName;
    }
    
    /**
     * Constructor
     * @param projectName
     * @param manualTriggerColumnName
     * @param autoSyncColumnName
     * @param backlinkColumnName
     * @param errorColumnName
     */
    public Project(String projectName, String manualTriggerColumnName, String autoSyncColumnName, String backlinkColumnName, String errorColumnName) {
        this.projectName = projectName;
        this.integrationLinks.manualTriggerColumnName = manualTriggerColumnName;
        this.integrationLinks.autoSyncColumnName = autoSyncColumnName;
        this.integrationLinks.backlinkColumnName = backlinkColumnName;
        this.integrationLinks.errorColumnName = errorColumnName;
    }
    
	/**
	 * @return  project ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * sets project Id
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
    /**
     * @return autoSyncColumnName
     */
    public String getAutoSyncColumnName() {
        return integrationLinks.autoSyncColumnName;
    }

    /**
     * sets autoSyncColumnName
     * @param autoSyncColumnName
     */
    public void setAutoSyncColumnName(String autoSyncColumnName) {
        this.integrationLinks.autoSyncColumnName = autoSyncColumnName;
    }

    /**
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * sets project name
     * @param projectName
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return projectUserName
     */
    public String getProjectUserName() {
		return projectUserName;
	}

	/**
	 * sets projectUserName
	 * @param projectUserName
	 */
	public void setProjectUserName(String projectUserName) {
		this.projectUserName = projectUserName;
	}

	/**
	 * @return manualTriggerColumnName
	 */
	public String getManualTriggerColumnName() {
        return integrationLinks.manualTriggerColumnName;
    }

    /**
     * sets manualTriggerColumnName
     * @param manualTriggerColumnName
     */
    public void setManualTriggerColumnName(String manualTriggerColumnName) {
        this.integrationLinks.manualTriggerColumnName = manualTriggerColumnName;
    }

    /**
     * @return backlinkColumnName
     */
    public String getBacklinkColumnName() {
        return integrationLinks.backlinkColumnName;
    }

    /**
     * sets backlinkColumnName
     * @param backlinkColumnName
     */
    public void setBacklinkColumnName(String backlinkColumnName) {
        this.integrationLinks.backlinkColumnName = backlinkColumnName;
    }

    /**
     * @return errorColumnName
     */
    public String getErrorColumnName() {
        return integrationLinks.errorColumnName;
    }

    /**
     * sets errorColumnName
     * @param errorColumnName
     */
    public void setErrorColumnName(String errorColumnName) {
        this.integrationLinks.errorColumnName = errorColumnName;
    }

    /**
     * @return syncList
     */
    public List<Sync> getSyncList() {
        return syncList;
    }

    /**
     * sets syncList
     * @param syncList
     */
    public void setSyncList(List<Sync> syncList) {
        this.syncList = syncList;
    }
    
    /**
     * @return IntegrationColumnNames
     */
    public List<String> getIntegrationColumnNames() {
    	ArrayList<String> results = new ArrayList<String>();
    	for(Field field : integrationLinks.getClass().getDeclaredFields()) {
    		if (String.class.equals(field.getType())) {
    			try {
					results.add((String)field.get(this.integrationLinks));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
    		}
    	}
    	return results;
    }

	/**
	 * @return database
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * sets database
	 * @param database
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * @return server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * sets server
	 * @param server
	 */
	public void setServer(String server) {
		this.server = server;
	}
	
    /**
     * @return synchronization label
     * label is in format .(daatabase, projectName, server).
     * e.g .(OSLC_Adapter_Test, xpromarProject, <Hansoft server address>). 
     */
    public String getLabel() {
    	return Constants.SYNC_TO_SEPARATOR_START + database + Constants.SYNC_TO_SEPARATOR_SEPARATOR + projectName + Constants.SYNC_TO_SEPARATOR_SEPARATOR + server + Constants.SYNC_TO_SEPARATOR_END;
    }
    
    @Override
    public boolean equals(Object other){
        if (other == null) 
        	return false;
        
        if (this.getClass() != other.getClass())
        	return false;

        Project otherProject = (Project) other;
        
        // project primary keys is not label any more but Id
        // this id has two parts - hansoft project Id and database name
        if (this.getId().equalsIgnoreCase(otherProject.getId()))
        	return true;
        
        return false;
    }
}
