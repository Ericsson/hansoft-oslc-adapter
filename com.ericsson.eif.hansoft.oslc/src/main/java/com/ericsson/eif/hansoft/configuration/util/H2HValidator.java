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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.xml.sax.SAXException;

import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.Credentials;
import com.ericsson.eif.hansoft.Friend;
import com.ericsson.eif.hansoft.H2HConfig;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Scheduler;
import com.ericsson.eif.hansoft.SchedulerTrigger;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.configuration.ConfigurationControllerImpl;
import com.ericsson.eif.hansoft.exception.UnauthorizedException;
import com.ericsson.eif.hansoft.integration.HansoftIntegration;
import com.ericsson.eif.hansoft.utils.HansoftUtils;
import com.ericsson.eif.hansoft.utils.StringUtils;

public class H2HValidator {

	private ConfigurationControllerImpl configCtrl = ConfigurationControllerImpl.getInstance();
	private H2HConfig h2hConfig = H2HConfig.getInstance();
	private H2HValidationLogger logger = H2HValidationLogger.getInstance();
	
	/**
	 * Main validation method
	 * executes XSD validation 
	 * executes validation of FriendList
	 * executes validation of Projects
	 * 
	 * Writes validation output to file or on screen
	 * depending on configuration of 
	 * h2h_config_validation_output_to_file
	 * and h2h_config_validation_output_on_screen
	 * in adapter.properties file	 * 
	 */
	public void validate() {		
		 logger.info("Validation of H2H config is starting ...");		 
		 this.validadeXSD();
		 this.validateFriendsList();
		 this.validateProjectList();		 
		 logger.info("Validation of H2H config is done");
		 logger.writeOutput();
	}
	
	/**
	 * Set validation output to file
	 * @param flag
	 */
	public void setOutputToFile(boolean flag) {
		logger.writeToFile = flag;
	}
	
	/**
	 * Set validation output on screen
	 * @param flag
	 */
	public void setOutputToScreen(boolean flag) {
		logger.writeToScreen = flag;
	}
	
	/**
	 * Validates Friend List section in H2HConfig.xml file
	 * - checks if friend name is not empty
	 * - checks if integration user name is not empty
	 * - checks if password for integration user is not empty
	 * - checks if friend is defined only once (so we do not have duplicates)
	 */
	private void validateFriendsList() {
		List<Friend> friendList = h2hConfig.getFriendsList();
		if (friendList.isEmpty()) {
			logger.error("Missing friends in H2HConfig file");
			return;
		}
				
		for (Friend friend : friendList) {
			if (StringUtils.isEmpty(friend.getFriendName())) {
				logger.error("Empty friendName in FriendList section");
			}			
			
			if (StringUtils.isEmpty(friend.getUsername())) {
				logger.error("Empty username in FriendList section for friend " + friend.getFriendName());
			}
			
			if (StringUtils.isEmpty(friend.getPassword())) {
				logger.error("Empty password in FriendList section for friend " + friend.getFriendName());
			}
		}
		
		this.checkForDuplicateFriendNames();		
	}
	
	/**
	 * Checks if H2HConfig.xml contains duplicate Friends in Friend List section 
	 */
	private void checkForDuplicateFriendNames() {
		List<Friend> friendList = h2hConfig.getFriendsList();
		Set<String> set = new HashSet<>();
		
		for (Friend friend : friendList) {
			if (StringUtils.isNotEmpty(friend.getFriendName())) {
				if (!set.contains(friend.getFriendName())) {
					set.add(friend.getFriendName());
				} else {
					logger.error("Found duplicate Friends");
					logger.info("Name: " + friend.getFriendName());
				}
			}
		}
	}

	/**
	 * Validates Project List section in H2HConfig.xml file
	 * - checks if project is defined only once (so we do not have duplicates)
	 * - checks if connection is defined (server, database, project name, project user)
	 * - checks if integration columns are defined (column for backLinks, Errors)
	 *   if connection is defined correctly, it checks if these columns are defined in Hansoft projects 
	 * - checks if Sync List section is defined 
	 *   and each sync has defined connection (friend name, server, database, project name)
	 *  - check if there are no duplicate Creation factory url
	 *  - check if we have scheduled synchronization from mother to child 
	 *	 and from child to mother at same time
	 */
	private void validateProjectList() {
		if (configCtrl.getProjects().isEmpty()) {
			logger.warning("No projects found in H2HConfig file");
			return;
		}
		
		this.checkForDuplicateProjects();
		this.validateProjectConnectionConfiguration();		
		this.validateIntegrationColumns();
		this.checkForDuplicateCreationFactoryUrl();
		this.validateProjectSyncList();
		this.validateScheduledTimes();
	}
	
	/**
	 * Checks if there are no duplicate project configurations
	 */
	private void checkForDuplicateProjects() {
		List<Project> projects = configCtrl.getProjects(); 
		Set<Project> set = new HashSet<>();
		
		for (Project p : projects) {
			if (!set.contains(p)) {
				set.add(p);
			} else {
				logger.error("Found duplicate configuration for project:");
				logger.info("Name: " + p.getProjectName());
				logger.info("Database: " + p.getDatabase());
				logger.info("Server: " + p.getServer());
			}
		}
	}
	
	/**
	 * Validates that each project has defined 
	 * Id, project name, database, server, project user
	 * and that Id is in format hansoftProjectId_DatabaseName 
	 * Check that Id is matching HansoftId for given project name
	 */
	private void validateProjectConnectionConfiguration() {
		List<Project> projects = configCtrl.getProjects();
		for (Project p : projects) {
			if (StringUtils.isEmpty(p.getId())) {
				logger.error("Missing project id for project " + p.getProjectName());
			}
			
			if (StringUtils.isEmpty(p.getProjectName())) {
				logger.error("Missing project name for project " + p.getLabel());
			}
			
			if (StringUtils.isEmpty(p.getDatabase())) {
				logger.error("Missing database for project " + p.getProjectName());
			}
			
			if (StringUtils.isEmpty(p.getServer())) {
				logger.error("Missing server for project " + p.getProjectName());
			}
			
			if (StringUtils.isEmpty(p.getProjectUserName())) {
				logger.error("Missing project user for project " + p.getProjectName());
			}
			
			if (!p.getId().endsWith(p.getDatabase())) {
				logger.error("Wrong format of project Id for project " + p.getProjectName());
				logger.error("Correct format is hansoftProjectId_DatabaseName");				
			}
			
			HPMUniqueID projectId = HansoftUtils.getProjectIdFromProjectName(p.getProjectName());
			String p_id = projectId.toString() + "_" + p.getDatabase();
			if (!p_id.equals(p.getId())) {
				logger.error("Wrong projectId for project " + p.getProjectName());
				logger.error("correct project id should be " + p_id);
			}
		}
	}
	
	/**
	 * Validates that configuration H2HConfig.xml file contains mandatory integration columns
	 * Back links  - column where link to established connection is saved
	 * Errors - columns where errors are written
	 * 
	 * if project connection configuration is provided and valid
	 * - this method also tries to verify that mandatory columns are defined in Hansoft project
	 *   and that they are of correct type (text, multiline text...)
	 */
	private void validateIntegrationColumns() {
		for (Project p : configCtrl.getProjects()) {			
			// check if we have any integration links in project
			if (p.getIntegrationColumnNames().isEmpty()) {
				logger.error("Missing Integration Links section for project " + p.getProjectName());
				continue;
			}
			
			if (StringUtils.isEmpty(p.getBacklinkColumnName())) {
				logger.error("Missing Backlink Column Name in H2HConfig file for project " + p.getProjectName());
			}
			
			if (StringUtils.isEmpty(p.getErrorColumnName())) {
				logger.error("Missing Error Column Name in H2HConfig file for project " + p.getProjectName());
			}
			
			// try to connect to hansoft and get projectId
			HPMUniqueID projectId = null;
			try {
				projectId = HansoftUtils.getProjectIdFromProjectName(p.getProjectName());
			} catch (Exception e1) {
				logger.error("Error getting project Id for project " + p.getProjectName());				
			}
			
			if (projectId == null || projectId.m_ID == -1) {
				logger.error("Can't get projectId for project " + p.getProjectName());
				logger.error("Validation of integration columns will be skipped for this project");
				continue;
			}				
			
			// check if backLink column is configured in hansoft screen and has correct type			
			try {				 
				 HansoftIntegration.getBackLinkHansoftColumn(projectId, p.getProjectName());
			} catch (Exception e) {
				logger.error("Backlink column validation in project " + p.getProjectName() + " failed");				
				if (e.getMessage() != null) {
					logger.error(e.getMessage());
				}
			}
						
			// check if error column is configured in hansoft screen and has correct type			
			try {				 
				 HansoftIntegration.getErrorHansoftColumn(projectId, p.getProjectName());
			} catch (Exception e) {
				logger.error("Error column validation in project " + p.getProjectName() + " failed");
				if (e.getMessage() != null) {
					logger.error(e.getMessage());
				}
			}			
		}		
	}
		
	/**
	 * Validates Sync List section
	 * - checks if connection configuration is defined
	 * - checks if connection can be established
	 *   and that defined friend has access to project where we want to sync
	 * - checks if creation factory URL is provided and has correct format
	 * - checks for duplicate creation factory url
	 * - checks that we do not have duplicate triggers within a sync
	 * 
	 * - if parent task ID is defined, it checks if task with such Id is existing 
	 *   in project where we want to sync 
	 */
	private void validateProjectSyncList() {
		List<Project> projects = configCtrl.getProjects();
		
		for (Project p : projects) {
			List<Sync> projectSyncList = p.getSyncList();			
			for (Sync s : projectSyncList) {
				logger.info("Validating sync to " + s.getProjectName() + " from project " + p.getProjectName()); 

				if (this.validateSyncConnectionConfiguration(s)) {
					this.checkSyncConnection(p, s);
				}
				
				this.validateParentTaskId(s);
				this.validateCreationFactoryUrl(s);
				this.validateMappings(s);
				this.validateScheduling(s);
				
				// Warn user that sync rules are not applied when KeepInSync is set to true
				if (s.isKeptInSync() == true && !s.getSyncRules().isEmpty()) {
					logger.warning("Defined synchronization rules are not applied because KeepInSync is set to true");
				}
			}
		}

		this.checkForDuplicateCreationFactoryUrl();
	}
	
	/**
	 * Validate one Sync configuration record
	 * - checks if friend is from defined friend list
	 * - checks if database, server, project name are defined
	 * @param s
	 * @return true if sync configuration in H2HConfig.xml is valid
	 */
	private boolean validateSyncConnectionConfiguration(Sync s) {
		boolean isConfigurationValid = true;
		if (StringUtils.isEmpty(s.getFriendName())) {		
			isConfigurationValid = false;
			logger.error("Missing friend name");
		} else {
			// check if friend is from FriendList section
			String syncFriendName = s.getFriendName();
			List<Friend> friendList = H2HConfig.getInstance().getFriendsList();
					
			boolean foundFriend = false;
			for (Friend f : friendList) {
				if (f.getFriendName() == syncFriendName) {
					foundFriend = true;
					break;
				}
			}
			
			if (!foundFriend) {
				logger.error(s.getFriendName() + " is not in defined in FriendList section");
			}
		}		
		
		if (StringUtils.isEmpty(s.getDatabase())) {
			isConfigurationValid = false;
			logger.error("Missing database");
		}
		
		if (StringUtils.isEmpty(s.getServer())) {
			isConfigurationValid = false;
			logger.error("Missing server");
		}
		
		if (StringUtils.isEmpty(s.getProjectName())) {
			isConfigurationValid = false;
			logger.error("Missing project name");
		}
		
		if (StringUtils.isEmpty(s.getTrigger())) {			
			logger.error("Missing sync trigger - automatic or manual?");
		}
		else {
				if (!s.getTrigger().equalsIgnoreCase("automatic") 
						&& !s.getTrigger().equalsIgnoreCase("manual")) {					
						logger.error("Wrong value for sync trigger. Allowed values are manual or automatic");
				}			
		}
		
		// check if <id>15_OSLC_Adapter_Test3</id> in Sync section 
		// is matching <id> of one project
		boolean idMatch = false;
		for (Project p : configCtrl.getProjects()) {
			if (p.getId().equals(s.getId())) {				
				idMatch = true;
				break;
			}
		}
		
		if (idMatch == false) {
			isConfigurationValid = false;
			logger.error("Sync Id " + s.getId() + " does not match any project Id");
		}
		
		return isConfigurationValid;
	}
	
	/**
	 * Checks if connection defined in Sync can be established
	 * and that defined friend has access to project where we want to sync
	 * @param p
	 * @param s
	 */
	private void checkSyncConnection(Project p, Sync s) {
		
		   // Find friend in FriendList section and get credentials
	  	   Friend syncFriend = null;
		   for (Friend f : H2HConfig.getInstance().getFriendsList()) {		   
			   if (f.getFriendName() == s.getFriendName()) {			   
				   syncFriend = f;
				   break;
			   }
		   }
		   
		   if (syncFriend == null) {
			   logger.error("Friend " + s.getFriendName() + " was not found in FriendList section");
			   return;
		   }
		   
		   // Get friend's credentials and create connector to hansoft
		   Credentials syncFriendCredentials = new Credentials(syncFriend.getUsername(), syncFriend.getPassword());
		   HansoftConnector hc = null;
		   try {
			   hc = HansoftConnector.createAuthorized(syncFriendCredentials);
			   hc.setImpersonate();
		   } catch (UnauthorizedException | HPMSdkException | HPMSdkJavaException e) {
			   if (hc != null) {
                   hc.release();
			   }
			   
			   logger.error("Configured friend " + syncFriend.getFriendName() + " is not able to create Hansoft session");
			   logger.error("User " + syncFriend.getUsername() + " is not existing in Hansoft");
			   logger.error("Or user " + syncFriend.getUsername() + " does not have access rights to Hansoft");
			   return;
		   } 
		   
		   // get session
		   HPMSdkSession session = null;
		   try {
			    session =  hc.getHansoftSession();
		   } catch (HPMSdkException | HPMSdkJavaException e){
			   	if (hc != null) {
			   		hc.release();
			   	}
			   	
			   	return;
		   }
		   		  
		   // get projectId of project where we want to sync and try to fetch tasks from this project
		   // just to check if friend user has access to it.
		   HPMUniqueID syncToProjectId = HansoftUtils.getProjectIdFromProjectName(s.getProjectName());
		   if (syncToProjectId == null) {
			   logger.error("Project with name " + s.getProjectName() + " does not exists in database");
			   return;
		   }
		 		   
		   try {
				session.ProjectUtilGetBacklog(syncToProjectId);
		   } catch (HPMSdkException | HPMSdkJavaException e) {
			   logger.error("User " + syncFriend.getUsername() + " does not have access rights to project " + s.getProjectName());
		   }
		   
		   // close connection to hansoft
		   if (hc != null) {
               hc.release();
		   }
	}

	/**
	 * If parent task Id is provided in H2HConfig file
	 * it checks if task with such ID is existing and is 
	 * defined in project where we want to sync	 *  
	 * @param s
	 */
	private void validateParentTaskId(Sync s) {		
		String parentTaskIdString = s.getParentTaskid();		
		
		if (StringUtils.isNotEmpty(parentTaskIdString)) {
			
			HPMUniqueID parentTaskId = HansoftUtils.getTaskId(parentTaskIdString);
			if (parentTaskId == null) {
				logger.error("Parent task ID " + parentTaskIdString + " does not exist");
				return;
			}
			
			// first check if task is still existing in hansoft
			try {
				HansoftUtils.getTaskReference(parentTaskId);
			} catch (HPMSdkException | HPMSdkJavaException e) {
				logger.warning("Parent Task Id " + parentTaskIdString + " is not existing any more.");
				return;
			}
			
			// check if parent task id belongs to project where we want to sync
			try {				
	    		HPMUniqueID projectId = HansoftUtils.getProjectIdOfTask(parentTaskId);
	    		HPMUniqueID projectIdReal = HansoftUtils.getRealProjectIdOfProjectId(projectId);
	    		String projectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), projectIdReal);
				if (!projectName.equalsIgnoreCase(s.getProjectName())) {		
					logger.error("Parent task ID " + parentTaskIdString + " belongs to project " + projectName);
					logger.error("And not to project " + s.getProjectName() + " where you want to sync");
				}
			} catch (Exception e) {
				return;
			}
		}				
	}
	
	/**
	 * Validates creation factory url format
	 * - also check if port number where hansoft adapter is working
	 *   matches port number defined in adapter.properties 
	 * @param s
	 */
	private void validateCreationFactoryUrl(Sync s) {		
		String url = s.getCreationFactoryURL();
		if (StringUtils.isEmpty(url)) {
			logger.error("Creation factory URL is empty");
			return;
		}
		
		String syncToProjectName = s.getProjectName();
		if (StringUtils.isEmpty(syncToProjectName)) {
			logger.warning("Cannot validate creation factory URL, project name is empty");
			return;
		}
				
		if (url.startsWith(" ")) {
			logger.error("URL starts with space");
		}
		
		if (url.endsWith(" ")) {
			logger.error("URL ends with space");
		}
		
		url = url.trim();		
		String[] urlParts = url.split("/");
		
		String urlPart = urlParts[urlParts.length - 1]; 
		if (!urlPart.equalsIgnoreCase("tasks")) {
			logger.error("URL must end with   /tasks");			
		}
		
		HPMUniqueID syncToProjectId = HansoftUtils.getProjectIdFromProjectName(syncToProjectName);
		if (syncToProjectId != null) {
			urlPart = urlParts[urlParts.length - 2]; 
			if (!urlPart.equalsIgnoreCase(syncToProjectId.toString())) {
				if (StringUtils.isNumeric(urlPart)) {
					logger.error("URL contains wrong projectID");
					logger.error("Expected ID: " + syncToProjectId.toString());
					logger.error("Current ID: " + urlParts[urlParts.length - 2]);
				} else {
					logger.error("URL must contain projectId before /tasks");
					logger.error("Fix it to /" + syncToProjectId.toString() + "/tasks");
				}
			}
			
			// check that project id in URL is same as id part in Sync id tag			
			String[] syncIdParts = s.getId().split("_");			
			if (!syncIdParts[0].equals(urlParts[urlParts.length - 2])) {
				logger.error("project id in creation factory URL must match projectId part of sync id");
				logger.error("project id in creation factory URL " + urlParts[urlParts.length - 2]);
				logger.error("project id part of Sync Id " + syncIdParts[0]);
			}			
		}		
		
		urlPart = urlParts[urlParts.length - 3]; 
		if (!urlPart.equalsIgnoreCase("projects")) {
			logger.error("missing word \"/project\" in URL, before /projectId/tasks");
			logger.error("Fix it to /projects/" + syncToProjectId.toString() + "/tasks");
		}
		
		urlPart = urlParts[urlParts.length - 4];
		if (!urlPart.equalsIgnoreCase("services")) {
			logger.error("missing word \"/services\" in URL, before /projects/projectId/tasks");
			logger.error("Fix it to /services/projects/" + syncToProjectId.toString() + "/tasks");
		}
		
		// check if URL contains port number defined in adapter.properties
		String adapterPortNumber = HansoftManager.getHansoftAdapterServerPort();
		if (StringUtils.isNotEmpty(adapterPortNumber)) {
			String adapterPortSequence = ":" + adapterPortNumber + "/";
			if (!url.contains(adapterPortSequence)) {
				logger.error("URL does not contain correct port number");
				logger.error("Expected port number is " + adapterPortNumber);
			}
		}
	}
	
	/**
	 * Checks if we have duplicate creation factory URL
	 */
	private void checkForDuplicateCreationFactoryUrl() {	
		List<Project> projects = configCtrl.getProjects(); 
		Set<String> set = new HashSet<>();
		
		for (Project p : projects) {
			List<Sync> projectSyncList = p.getSyncList();
			
			for (Sync s : projectSyncList) {
				 String url = s.getCreationFactoryURL();
				 if (StringUtils.isEmpty(url)) {
					 logger.error("Creation factory URL in project " + p.getProjectName() + " in Sync " + s.getLabel() + " is empty");
					 continue;
				 }
				
				 if (!set.contains(url)) {
					 set.add(url);
				} else {
					logger.error("Found duplicate Creation Factory URL");
					logger.error(url);
					logger.error("for project");					
					logger.error("Name: " + p.getProjectName());
					logger.error("Database: " + p.getDatabase());
					logger.error("Server: " + p.getServer());
					logger.error("in Sync");
					logger.error("Name: " + s.getProjectName());
					logger.error("Database: " + s.getDatabase());
					logger.error("Server: " + s.getServer());
				}
			}
		}
	}
	
	/**
	 * Executes XSD validation of H2HConfig.xml file
	 */
	private void validadeXSD() {
		String[] args = new String[0];
		try {
			H2HXSDValidator.main(args);
		} catch (IOException | SAXException e) {			
			e.printStackTrace();
		}
	}

	/**
	 * Check if we have duplicate scheduling triggers in one sync
	 * Check is done only for active triggers
	 *  
	 * @param s
	 */
	private void validateScheduling(Sync s) {
		Scheduler scheduler = s.getScheduler();
		
		// check if we have scheduler defined in sync
		if (scheduler == null)
			return;
		
		// if scheduler is not active, do not check triggers
		if (!scheduler.isActive())
			return;
						
		// check if we have some triggers
		List<SchedulerTrigger> triggers = scheduler.getTriggers(); 
		if (triggers == null || triggers.isEmpty()) {
			logger.warning("Scheduling is active but no triggers are defined in scheduling section");			
			return;
		}
		
		// check if we have duplicate active triggers in one scheduling section
		Set<String> set = new HashSet<>();
		for (SchedulerTrigger trigger : triggers) {
			if (trigger.isActive()) {
				String triggerName = trigger.getCronScheduleString(); 
				if (!set.contains(triggerName)) {
					set.add(triggerName);
				}
				else {
						logger.error("Found duplicate active trigger: " + triggerName);
				}
			}
		}
	}
	
	/**
	 * check if we have scheduled synchronization from mother to child
	 *  and from child to mother at same time
	 */
	private void validateScheduledTimes() {
		// first build list of structures "from, to, when"
		// e.g.  A -> B  at 10
		// B -> C at 11
		// B -> A at 10
		List<String[]> listOfSyncs = new ArrayList<String[]>();

		List<Project> projects = configCtrl.getProjects();		
		for (Project project : projects) {
			for (Sync sync : project.getSyncList()) {
				
				// if scheduler is not activated then skip
				Scheduler scheduler = sync.getScheduler();
				if (scheduler == null || !scheduler.isActive())
					continue;
					
					for (SchedulerTrigger schedulerTrigger : scheduler.getTriggers()) {
					
						// if trigger is not activated then skip
						if (!schedulerTrigger.isActive())
							continue;
						
						String[] fromToWhen = {project.getProjectName(), sync.getProjectName(), schedulerTrigger.getCronScheduleString()};
						listOfSyncs.add(fromToWhen);
					}
			}
		}
		
		// iterate our structures and check if there are records 
		// like  A -> B  and B -> A
		// if yes, log error if synchronization time is same
		for (String[] motherFromToWhen : listOfSyncs) {
			String motherFrom = motherFromToWhen[0];
			String motherTo = motherFromToWhen[1];
			String motherWhen = motherFromToWhen[2];
			
			for (String[] childFromToWhen : listOfSyncs) {
				String childFrom = childFromToWhen[0];
				String childTo = childFromToWhen[1];
				String childWhen = childFromToWhen[2];

				if (motherFrom.equalsIgnoreCase(childTo) && motherTo.equalsIgnoreCase(childFrom)) {					
					if (motherWhen.equalsIgnoreCase(childWhen)) {						
						logger.error("Wrong timing of synchronization between " + motherFrom + " and " + motherTo);						
						String errorMessage = "Synchronization from " + motherFrom + " to " + motherTo + " cannot be done at same time (" + motherWhen + ") ";
						errorMessage = errorMessage + " as opposite synchronization from " + motherTo + " to " + motherFrom;						
						logger.error(errorMessage);
					}
				}				
			}
		}
	}
	
	/**
	 * Check if mapping rules are existing
	 * @param s
	 */
	private void validateMappings(Sync s) {
	     if (s.getMappingPairList().isEmpty()) {
	    	 logger.error("Mapping rules are missing");
	    	 return;
	     }
		
		// sloupec z existuje v hansoftu odkud mapuju
		// sloupec do existuje v hanosftu kam mapuju
	}
	
	public static void main(String[] args) throws IOException, SAXException {			

		// First read adapter.properties file 
		// to check where we should create output of validation
		
		Properties props = new Properties();
		String adapterHome = System.getenv("ADAPTER_HOME");
        if (adapterHome == null) {
            // default to user home
            adapterHome = System.getProperty("user.home");
        }
        
        // Need the properties file - if not found, exit        
        String adapterServletHome = adapterHome + File.separator; // + contextPath;
        try {
            File propsPath = new File(adapterServletHome + File.separator + "adapter.properties");
            if (propsPath.exists()) {
                props.load(new FileInputStream(propsPath.toString()));
            } else {
                String message = "The adapter.properties file not found, will exit.";                
                throw new RuntimeException(message);
            }
        } catch (Exception e) {
            String message = "Failed to read the adapter.properties file, will exit.";
            message = message + Constants.LS + adapterServletHome + File.separator + "adapter.properties"; 
            throw new RuntimeException(message);
        }
        
        String h2h_config_validation_output_to_file = props.getProperty("h2h_config_validation_output_to_file", "true").trim();
        String h2h_config_validation_output_on_screen = props.getProperty("h2h_config_validation_output_on_screen", "true").trim();
        
        H2HValidator validator = new H2HValidator();
		if (h2h_config_validation_output_on_screen.equalsIgnoreCase("true")) {
			validator.setOutputToScreen(true);			
		} else {
			validator.setOutputToScreen(false);
		}
		
		if (h2h_config_validation_output_to_file.equalsIgnoreCase("true")) {
			validator.setOutputToFile(true);			
		} else {
			validator.setOutputToFile(false);
		}
		
		validator.validate();
	}
}
