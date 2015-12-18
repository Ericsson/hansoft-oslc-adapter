package com.ericsson.eif.hansoft.integration;

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnType;
import se.hansoft.hpmsdk.HPMProjectCustomColumns;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.Credentials;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.MappingPair;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.configuration.ConfigurationControllerImpl;
import com.ericsson.eif.hansoft.exception.ConfigItemNotFound;
import com.ericsson.eif.hansoft.factories.HansoftChangeRequestFactory;
import com.ericsson.eif.hansoft.factories.HansoftTaskFactory;
import com.ericsson.eif.hansoft.integration.logging.ChangeLogger;
import com.ericsson.eif.hansoft.integration.logging.ConnectionType;
import com.ericsson.eif.hansoft.integration.logging.HansoftLogger;
import com.ericsson.eif.hansoft.integration.logging.SynchronisationStatus;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.utils.HansoftUtils;
import com.ericsson.eif.hansoft.utils.HttpUtils;
import com.ericsson.eif.hansoft.utils.OSLCUtils;
import com.ericsson.eif.hansoft.utils.StringUtils;

public class HansoftIntegration {
	
	private static final Logger logger = Logger.getLogger(HansoftIntegration.class.getName());
	
	/**
	 * @param taskId
	 * @param fromProjectId
	 * @param fromProject
	 * @param hc
	 * @param syncTo
	 * @throws Exception
	 */
	public void syncTask(HPMUniqueID taskId, HPMUniqueID fromProjectId, Project project, HansoftConnector hc, Sync syncTo) throws Exception {

		HansoftChangeRequest localHansoftChangeRequest = HansoftChangeRequestFactory.getChangeRequestFromTaskBasic2(null, fromProjectId.toString(), fromProjectId, taskId, false, hc);

		// do we have already backLink for selected sync ?
		HPMProjectCustomColumnsColumn backLinkHansoftColumn = getBackLinkHansoftColumn(fromProjectId, project.getProjectName());
		String linkToChildForSelectedSync = getBackLinkForSync(taskId, syncTo.getId(), backLinkHansoftColumn); 
		if (linkToChildForSelectedSync == null) {
			// create			 
			postTask(localHansoftChangeRequest, fromProjectId, project, taskId, backLinkHansoftColumn, syncTo, hc);
		} else {
			// update
			putTask(linkToChildForSelectedSync, localHansoftChangeRequest, fromProjectId, project, taskId, syncTo, hc);
		}
	}	

	/** 
	 *  Do POST to create a task
	 *  
	 * @param url
	 * @param localHansoftChangeRequest
	 * @param fromProjectId
	 * @param fromProjectName
	 * @param taskId
	 * @param linkToChildsColumn
	 * @param syncTo
	 * @param hc
	 * @throws Exception
	 */	
	private void postTask(HansoftChangeRequest localHansoftChangeRequest, HPMUniqueID fromProjectId, Project project, 
							HPMUniqueID taskId, HPMProjectCustomColumnsColumn linkToChildsColumn, Sync syncTo, HansoftConnector hc) 
									throws Exception {
		HansoftChangeRequest remoteHansoftChangeRequest = new HansoftChangeRequest();
		remoteHansoftChangeRequest.setAbout(localHansoftChangeRequest.getAbout());
		
		addParentTask(remoteHansoftChangeRequest, project.getProjectName(), syncTo.getLabel());		
		
		// log changed columns
		ChangeLogger outgoingChangeLogger = new ChangeLogger(); 
		String toProjectName = syncTo.getProjectName();
		String username = hc.getUserName(); 
		outgoingChangeLogger.setup(project.getProjectName(), toProjectName, taskId, username, ConnectionType.Out);
		
		// mapping
		boolean isAtLeastOneAttributeSet = mapHansoftAttributesPost(localHansoftChangeRequest, remoteHansoftChangeRequest, project.getProjectName(), syncTo, outgoingChangeLogger);
		if (!isAtLeastOneAttributeSet) {
			// no need to send any create, nothing has been mapped
			return;
		}
		
		// log what we are sending		
		HansoftLogger hl = HansoftLogger.getInstance();
		hl.log("sending new task from " + project.getProjectName() + " to " + toProjectName);
		hl.log(remoteHansoftChangeRequest);		
		
		// transform to rdf here
		String data = OSLCUtils.createRDFData(remoteHansoftChangeRequest);

		//
		// send POST request
		//
		HttpResponse response = null;
		Poster poster = new Poster();
				
		try {
			// send and handle connection timeout exception
			response = poster.postTask(syncTo.getCreationFactoryURL(), data, getFriendCredentials(project.getProjectName(), syncTo.getLabel()), project.getLabel(), project.getId());		
		} catch (HttpHostConnectException ex) {
			logger.error("Connection to " + syncTo.getCreationFactoryURL() + " has timed out ", ex);
			
			try {
				HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.FAILED, hc);
			} catch (Exception e) {					
				logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
			}
			
			throw ex;
		}
		
		// connection is ok, check response status codes
		int statusCode = response.getStatusLine().getStatusCode();			
		if (statusCode == 201 || statusCode == 200)	{
			String location = HttpUtils.getLocation(response);
			
			// update changed columns in change logger with synchronisation status and write result to log file
			outgoingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Ok);
			outgoingChangeLogger.writeToCsv();
			
			if (!StringUtils.isEmpty(location)) {				
				try {
					HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.OK, hc);
				} catch (Exception e) {					
					logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
				}
				
				// add back link
				addBackLink(location, fromProjectId, taskId, linkToChildsColumn, syncTo.getLabel(), syncTo.getId(), hc);				
			} else {
				logger.error("Location field in http response header is empty or missing.");
			}			
		} else {
				// other codes then 201 or 200
			
				// update changed columns in change logger with synchronisation status and write result to log file
				outgoingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
				outgoingChangeLogger.writeToCsv();
			
				try {
					HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.FAILED, hc);
				} catch (Exception e) {					
					logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);
				}
				
				String errorMessage = "Error STATUS:" + statusCode + ". \"" + response.getStatusLine().getReasonPhrase() + "\"";
				errorMessage = errorMessage	+ " returned as responce to POST request to " + syncTo.getCreationFactoryURL();
				System.out.println(errorMessage);
				logger.error(errorMessage);
				hl.log(errorMessage);
			    throw new Exception(errorMessage);
		}
	}

	/** 
	 *  Do PUT to update a task
	 *  
	 * @param url
	 * @param localHansoftChangeRequest
	 * @param fromProjectId
	 * @param project
	 * @param taskId
	 * @param syncTo
	 * @param hc
	 * @throws Exception
	 */
	private void putTask(String url, HansoftChangeRequest localHansoftChangeRequest, HPMUniqueID fromProjectId, Project project,
							HPMUniqueID taskId, Sync syncTo, HansoftConnector hc) 
									throws Exception {

		Credentials friendCredentials = getFriendCredentials(project.getProjectName(), syncTo.getLabel());
		Poster poster = new Poster();				
		HttpResponse response = null;
		
		//
		// GET task, handle connection timeout exception
		//
		try {
			response = poster.getTask(url, friendCredentials);
		} catch (HttpHostConnectException ex) {
			logger.error("Connection to " + url + " has timed out ", ex);
			
			try {
				HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId,  syncTo, Constants.FAILED, hc);
			} catch (Exception e) {					
				logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
			}
			
			throw ex;
		}
		
		// we were able to get task from given URL, check status code of response
		int statusCode = response.getStatusLine().getStatusCode();		
		
		if (statusCode == 200)	{
			System.out.println("GET OK " + project.getLabel());			
		} else {			
				try {
					HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId,  syncTo, Constants.FAILED, hc);
				} catch (Exception e) {					
					logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
				}
	
				String errorMessage = "Error STATUS:" + statusCode + ". \"" + response.getStatusLine().getReasonPhrase() + "\" returned as response to GET request to " + url; 
				System.out.println(errorMessage);
				logger.error(errorMessage);
			    throw new Exception(errorMessage);
		}
		
		HansoftChangeRequest remoteHansoftChangeRequest = getRemoteHansoftChangeRequest(response, url);
		
		// log changed columns
		ChangeLogger outgoingChangeLogger = new ChangeLogger();
		String toProjectName = HansoftUtils.getProjectNameFromSync(syncTo.getLabel());
		String username = hc.getUserName(); 
		outgoingChangeLogger.setup(project.getProjectName(), toProjectName, taskId, username, ConnectionType.Out); 
						
		boolean isAtLeastOneAttributeSet = mapHansoftAttributesPut(localHansoftChangeRequest, remoteHansoftChangeRequest, project.getProjectName(), syncTo, outgoingChangeLogger);
		if (!isAtLeastOneAttributeSet) {
			// no need to send any update, nothing has been changed
			return;
		}			
		
		String eTag = OSLCUtils.getETagHeader(response);
							
		// log what we are sending		
		HansoftLogger hl = HansoftLogger.getInstance();
		hl.log("sending updated task from " + project.getProjectName() + " to " + toProjectName);
		hl.log(remoteHansoftChangeRequest);
						
		// transform to rdf here
		String data = OSLCUtils.createRDFData(remoteHansoftChangeRequest);

		//
		// send PUT request here
		//
		statusCode = 200; // http status 200 = ok
		response = null;
		
		try {
			// send and handle connection timeout exceptions
			response = poster.putTask(url, data, eTag, friendCredentials, project.getLabel(), project.getId());
		} catch (HttpHostConnectException ex) {
			logger.error("Connection to " + url + " has timed out ", ex);
			
			try {
				HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.FAILED, hc);
			} catch (Exception e) {					
				logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
			}
			
			throw ex;
		}
		
		statusCode = response.getStatusLine().getStatusCode();
		
		if (statusCode == 200) {
			System.out.println("UPDATE OK");
			// update changed columns in change logger with synchronization status and write result to log file
			outgoingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Ok);
			outgoingChangeLogger.writeToCsv();
			
			try {
				HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.OK, hc);
			} catch (Exception e) {					
				logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
			}
			
		} else {
			// other codes then 200

			// update changed columns in change logger with synchronisation status and write result to log file
			outgoingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			outgoingChangeLogger.writeToCsv();

			try {
				HansoftIntegration.updateSynchronizationStatusOfBackLink(fromProjectId, project.getProjectName(), taskId, syncTo, Constants.FAILED, hc);
			} catch (Exception e) {					
				logger.error("Error adding status to backlink label of task " + taskId + " in project named " + project.getProjectName() + " projectId " + fromProjectId, e);					
			}

			String errorMessage = "Error STATUS" + statusCode + ". \"" + response.getStatusLine().getReasonPhrase() + "\" returned as responce to PUT request to " + url; 
			System.out.println(errorMessage);
			logger.error(errorMessage);
			hl.log(errorMessage);
			throw new Exception(errorMessage);
		}		
	}
	
	/**
	 * gets friend credentials
	 * @param projectName
	 * @param syncLabel
	 * @return
	 * @throws ConfigItemNotFound
	 */
	private Credentials getFriendCredentials(String projectName, String syncLabel) throws ConfigItemNotFound {
		String friendName = ConfigurationControllerImpl.getInstance().getFriendName(projectName, syncLabel);
		String userName = ConfigurationControllerImpl.getInstance().getFriendUserName(friendName);
		String psswd = ConfigurationControllerImpl.getInstance().getFriendPassword(friendName);
		return new Credentials(userName, psswd);
	}

	/**
	 * @param response
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public HansoftChangeRequest getRemoteHansoftChangeRequest(HttpResponse response, String url) throws Exception {
		String content = HttpUtils.getContent(response);
		Object[] tasks = OSLCUtils.createTaskFromRDFData(content);

		HansoftChangeRequest remoteHansoftChangeRequest;
		if (tasks != null && tasks.length >= 1) {
			remoteHansoftChangeRequest = (HansoftChangeRequest) tasks[0];
		} else {
			logger.error("Failed to GET task from URL " + url);
			throw new Exception("Failed to GET task from URL " + url);
		}

		return remoteHansoftChangeRequest;
	}

	/**
	 * adds parent task to hansoft change request
	 * @param hansoftChangeRequest
	 * @param projectName
	 * @param syncTo
	 */
	private void addParentTask(HansoftChangeRequest hansoftChangeRequest, String projectName, String syncTo) {
		try {
			String parentTaskId = ConfigurationControllerImpl.getInstance().getParentTaskId(projectName, syncTo);
			hansoftChangeRequest.setParentTask(parentTaskId);
		} catch (ConfigItemNotFound e) {
			logger.info(e.getMessage(), e);
		}
	}

	/**
	 * @param localHansoftChangeRequest
	 * @param remoteHansoftChangeRequest
	 * @param mappingPairs
	 * @param outgoingChangeLogger
	 * @throws ConfigItemNotFound
	 */
	private boolean mapHansoftAttributes(HansoftChangeRequest localHansoftChangeRequest, HansoftChangeRequest remoteHansoftChangeRequest, List<MappingPair> mappingPairs, ChangeLogger outgoingChangeLogger) throws ConfigItemNotFound {
		boolean setAtLeastOneAttribute = false;
		for (MappingPair mappingPair : mappingPairs) {
			Object value = HansoftUtils.getValueOfAttribute(localHansoftChangeRequest, mappingPair.getLocalPart(), mappingPair.getNameSpace());
			
			if (value != null) {
				Object oldValue = HansoftUtils.getValueOfAttribute(remoteHansoftChangeRequest, mappingPair.getDestinationLocalPart(), mappingPair.getDestinationNameSpace());				
				boolean wasSet = HansoftUtils.setValueOfAttribute(remoteHansoftChangeRequest, mappingPair.getDestinationLocalPart(), mappingPair.getDestinationNameSpace(), value);
				
				if (wasSet) {
					String columnName = mappingPair.getDestinationLocalPart();
					outgoingChangeLogger.log(columnName, oldValue, value);
				}
				
				if (setAtLeastOneAttribute == false) {
					setAtLeastOneAttribute = wasSet;
				}
			}
		}
		return setAtLeastOneAttribute;
	}
	
	/**
	 * @param localHansoftChangeRequest
	 * @param remoteHansoftChangeRequest
	 * @param projectName
	 * @param syncTo
	 * @param outgoingChangeLogger
	 * @throws ConfigItemNotFound
	 */
	private boolean mapHansoftAttributesPost(HansoftChangeRequest localHansoftChangeRequest, HansoftChangeRequest remoteHansoftChangeRequest, String projectName, Sync syncTo, ChangeLogger outgoingChangeLogger) throws ConfigItemNotFound {
		List<MappingPair> mappingPairsPost = syncTo.getMappingPairListPost();
		return mapHansoftAttributes(localHansoftChangeRequest, remoteHansoftChangeRequest, mappingPairsPost, outgoingChangeLogger);
	}
	
	/**
	 * @param localHansoftChangeRequest
	 * @param remoteHansoftChangeRequest
	 * @param projectName
	 * @param syncTo
	 * @param outgoingChangeLogger
	 * @throws ConfigItemNotFound
	 */
	private boolean mapHansoftAttributesPut(HansoftChangeRequest localHansoftChangeRequest, HansoftChangeRequest remoteHansoftChangeRequest, String projectName, Sync syncTo, ChangeLogger outgoingChangeLogger) throws ConfigItemNotFound {
		List<MappingPair> mappingPairsPut = syncTo.getMappingPairListPut();
		return mapHansoftAttributes(localHansoftChangeRequest, remoteHansoftChangeRequest, mappingPairsPut, outgoingChangeLogger);
	}	
	
	/**
     * Gets all backLinks of task which have correct URL format
     * backLink starts with tag specified in Constants.BACKLINK_URL_START and contains url with parameter connectionId
     * 
     * example of backLink 
     * 
     *  <URL=http://<Hansoft server address>:8443/test/services/tasks/2635?connectionId=123456>
     *		.(Database: OSLC_Adapter_Test, Project: xpromarProject).
     *	</URL>
     *
     * @param taskId
     * @param connectionId
     * @param backLinkColumn
     * @return String with backLink URL, not the whole backLink   
     **/
    private String getBackLinkForSync(HPMUniqueID taskId, String connectionId, HPMProjectCustomColumnsColumn backLinkColumn) throws Exception {
	
		// get all backLinks of task 
		// example of format <URL=http://www.example.com?connectionId=123456><COLOR=80,138,255><UNDERLINE><(Database: OSLC_Adapter_Test, Project: xpromarProject)></UNDERLINE></COLOR></URL>
		List<String> backLinkItems = getBackLinkItemsOfTask(taskId, backLinkColumn);
		
		for (String item : backLinkItems) {
			// if empty or it is faked backLink (does not contain <URL=....>) 
			if (StringUtils.isEmpty(item) || !item.startsWith(Constants.BACKLINK_URL_TAG_START)) {
				continue;
			}
			
			// get connectionId from backLink URL parameter
			String itemConnectionId = HansoftUtils.getConnectionIdFromBackLink(item);
			if (StringUtils.isEmpty(itemConnectionId)) {
				continue;
			}
		
			if (itemConnectionId.equalsIgnoreCase(connectionId)) {				
				int indexOfURLStart = item.indexOf(Constants.BACKLINK_URL_TAG_START);
				int indexOfURLEnd = item.indexOf('>');			
				String url = item.substring(indexOfURLStart + Constants.BACKLINK_URL_TAG_START.length(), indexOfURLEnd);				
				return url;
			}		
		}
		return null;
    }

	/**
	 * add backLink to task
	 * 
	 * example of backLink
     * 
     *  <URL=http://<Hansoft server address>:8443/test/services/tasks/2635?connectionId=11>
     *		.(Database: OSLC_Adapter_Test, Project: xpromarProject).
     *	</URL> 
	 * @param location
	 * @param projectId
	 * @param taskId
	 * @param backLinkColumn
	 * @param selectedSync
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static void addBackLink(String location, HPMUniqueID projectId, HPMUniqueID taskId, 
			HPMProjectCustomColumnsColumn backLinkColumn, String syncLabel, String connectionId, HansoftConnector hc) 
					throws HPMSdkException, HPMSdkJavaException {

		String encodedConnectionId = "";
		try {					
			// encode connectionID, can happen that it contains Swedish characters
			encodedConnectionId =  URLEncoder.encode(connectionId, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("Error during encoding connectionId and adding it to backLink url", e);
			logger.debug("encodedConnectionId is " + encodedConnectionId);
		}

		// first check if connectionId is already in current backLinks
		List<String> currentBackLinks = getBackLinkItemsOfTask(taskId, backLinkColumn);
		for (String currentBackLink : currentBackLinks) {
			
			// check if backLink does not contain string connectionId=123>   - this means End of http link
			// or string  connectionId=123& - this means next url parameter separator
			if (currentBackLink.indexOf(Constants.SELECTED_SYNC_URL_PARAMETER + encodedConnectionId + ">") != -1
				|| currentBackLink.indexOf(Constants.SELECTED_SYNC_URL_PARAMETER + encodedConnectionId + "&") != -1) {		
						
				// backLink we are trying to add already exists
				// nothing more to do
				return;
			}
		}
				
		// add new backLink
		StringBuffer newBackLinkBuffer = new StringBuffer();
		newBackLinkBuffer.append(Constants.BACKLINK_URL_TAG_START);
		newBackLinkBuffer.append(location);
				
		if (location.indexOf("?") != -1)
			newBackLinkBuffer.append("&" + Constants.SELECTED_SYNC_URL_PARAMETER + encodedConnectionId);
		else 
			newBackLinkBuffer.append("?" + Constants.SELECTED_SYNC_URL_PARAMETER + encodedConnectionId);			

		newBackLinkBuffer.append(Constants.BACKLINK_URL_END);
		newBackLinkBuffer.append(syncLabel);
		newBackLinkBuffer.append(Constants.BACKLINK_URL_TAG_END);
		
		String newBackLink = newBackLinkBuffer.toString();
			
		// add new back link to all links
		currentBackLinks.add(newBackLink);
		// put links back to column
		StringBuffer buffer = new StringBuffer();
		for (String backLink : currentBackLinks) {
			buffer.append(backLink);
			buffer.append(Constants.LS);
		}
		
		HansoftTaskFactory.updateCustomColumnData(projectId, hc.getHansoftSession(), backLinkColumn, false, taskId, buffer.toString(), "", null);			
	}
	
	/**
	 * updates backLink with synchronization status
	 * 
	 * method adds only status FAILED  to backLink column when update or create of task has failed.
	 * method removes status FAILED when update or create of task was successful 
	 * 
	 * @param projectId
	 * @param projectName
	 * @param taskId
	 * @param selectedSync
	 * @param synchronizationStatus
	 * @throws Exception
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static void updateSynchronizationStatusOfBackLink(HPMUniqueID fromProjectId, String fromProjectName, HPMUniqueID taskId, Sync selectedSync, String synchronizationStatus, HansoftConnector hc) throws Exception, HPMSdkException, HPMSdkJavaException {
		HPMProjectCustomColumnsColumn backLinkColumn = getBackLinkHansoftColumn(fromProjectId, fromProjectName);
		List<String> backLinks = getBackLinkItemsOfTask(taskId, backLinkColumn);
		
		// case when no valid backLinks were created yet e.g. because we are creating new task
		if (backLinks.isEmpty())
		{
			// synchronization failed, create faked backLink and with word FAILED
			if (synchronizationStatus == Constants.FAILED) {
				String status = selectedSync.getLabel() + " " + Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED + Constants.LS;
				HansoftTaskFactory.updateCustomColumnData(fromProjectId, hc.getHansoftSession(), backLinkColumn, false, taskId, status, "", null);
				return;
			}
			
			// synchronization is successful, new task was created, no need to do anything as backLink has not been created yet 
			if (synchronizationStatus == Constants.OK) {
				return;
			}
		}
		
		// case when we have some backLinks
		Boolean saveChanges = false;
		Boolean selectedSyncFound = false;
		StringBuffer backLinkBuffer = new StringBuffer();
		
		for (String backLink : backLinks) {			
			
			// synchronization failed, add word FAILED to backLink
			if (synchronizationStatus == Constants.FAILED) {
					
				if (HansoftUtils.getConnectionIdFromBackLink(backLink).equalsIgnoreCase(selectedSync.getId())) {											
					selectedSyncFound = true;					

					// if status FAILED is already at backLink, keep it, do not add another one
					// if status is missing, add it
					if (!backLink.contains(Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED)) {				
						String target = Constants.BACKLINK_URL_TAG_END;
						String replacement = Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED + Constants.BACKLINK_URL_TAG_END;
						backLink = backLink.replace(target, replacement);
						saveChanges = true;
					}
				}
				// add word FAILED to faked backLink if not there
				else if (backLink.contains(selectedSync.getLabel())) {	
					selectedSyncFound = true;
					
					if (!backLink.contains(Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED)) {						
						backLink = backLink + "	" + Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED + Constants.LS;
						saveChanges = true;
					}
				}
			}
			else {
				// synchronization was successful
				// remove word FAILED from backLink				
				if (HansoftUtils.getConnectionIdFromBackLink(backLink).equalsIgnoreCase(selectedSync.getId())
						&& backLink.startsWith(Constants.BACKLINK_URL_TAG_START)) {
					
					selectedSyncFound = true;
					
					if (backLink.contains(Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED)) {				
						String target = Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED;
						String replacement = "";
						backLink = backLink.replace(target, replacement);
						saveChanges = true;
					}
				}
				else  {
 					 // synchronization was successful
					 // case when we have faked backLink, remove it completely
					if (backLink.contains(selectedSync.getLabel())) {
						backLink = "";						
						saveChanges = true;
					}
				}
			}		
							
			if (backLink != "") {
				backLinkBuffer.append(backLink);
				backLinkBuffer.append(Constants.LS);
			}
		}
		
		// case when we have some backLinks, but we did not found the one where we want to sync now
		if (selectedSyncFound == false)
		{
			// synchronization failed, create faked backLink and with word FAILED
			if (synchronizationStatus == Constants.FAILED)
			{
				String status = selectedSync.getLabel() + " " + Constants.BACKLINK_SYNCHRONIZATION_STATUS_FAILED + Constants.LS;
				backLinkBuffer.append(status);
				backLinkBuffer.append(Constants.LS);
				saveChanges = true;
			}
		}		
		
		// only update backLink column if we have changed something
		if (saveChanges == true) {
			HansoftTaskFactory.updateCustomColumnData(fromProjectId, hc.getHansoftSession(), backLinkColumn, false, taskId, backLinkBuffer.toString(), "", null);
		}
	}
	    
	/**
	 * Get column as hansoft object where backLinks are stored
	 * @param projectId
	 * @param projectName
	 * @return
	 * @throws Exception
	 */
	public static HPMProjectCustomColumnsColumn getBackLinkHansoftColumn(HPMUniqueID projectId, String projectName) throws Exception {
		
		// get back link column name from configuration
		String backLinkColumnName;
		try {
			backLinkColumnName = ConfigurationControllerImpl.getInstance().getBacklinkColumnName(projectName);
		} catch (ConfigItemNotFound e) {
			logger.error("BackLink column name is not defined in Hansoft in project " + projectName, e);
			logger.info("Type of this column must be Multiline text");
			e.printStackTrace();
			throw e;
		}		
		
		List<HPMProjectCustomColumnsColumn> allCustomColumns = new ArrayList<HPMProjectCustomColumnsColumn>();
		// get all custom columns of project
		HPMProjectCustomColumns customColumns;
		try {
			customColumns = HansoftManager.getMainSession().ProjectCustomColumnsGet(projectId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Can't get Hansoft custom columns for project " + projectName, e);
			e.printStackTrace();
			throw e;
		}
		allCustomColumns.addAll(customColumns.m_ShowingColumns);
		allCustomColumns.addAll(customColumns.m_HiddenColumns);
		
		// find back link column in custom columns
		for (HPMProjectCustomColumnsColumn customColumn : allCustomColumns) {
			if (customColumn.m_Name.equals(backLinkColumnName)) {
				
				// if backLink column is found but is not defined as multiline text
				if (customColumn.m_Type !=  EHPMProjectCustomColumnsColumnType.MultiLineText) {
					String errorMessage = "Column " + backLinkColumnName + " used for backlinks is not configured as MultiLineText in project " + projectName;					
					logger.error(errorMessage);
					logger.debug("ProjectID " + projectId);
					logger.debug(backLinkColumnName + " is configured as type " + customColumn.m_Type);
					throw new Exception(errorMessage);
				}
				
				return customColumn;
			}
		}
		
		// if back link column was not found in custom columns
		String errorMessage = "BackLink custom column called " + backLinkColumnName + " is not defined in project called " + projectName; 
		logger.error(errorMessage);
		throw new Exception(errorMessage);
	}
	
	/**
	 * Returns backLink items of task
	 * @param taskId
	 * @param backLinkColumn 
	 * @return backLink items of task
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static List<String> getBackLinkItemsOfTask(HPMUniqueID taskId, HPMProjectCustomColumnsColumn backLinkColumn) throws HPMSdkException, HPMSdkJavaException {
		List<String> backLinkItems = new ArrayList<String>();

		String rawData = HansoftManager.getMainSession().TaskGetCustomColumnData(taskId, backLinkColumn.m_Hash);
		if (StringUtils.isEmpty(rawData)) 
			return backLinkItems;		

		// backLink is in following format
		// <URL=http://www.example.com?connectionId=123456><COLOR=80,138,255><UNDERLINE>link to item in LBB/646543</UNDERLINE></COLOR></URL>		
		// there can be more backLinks on one line so it is better to use some regular expression to find them 
		String urlTagPattern = "(?i)<URL=([^>]+)>(.+?)</URL>";
		Pattern pattern = Pattern.compile(urlTagPattern, Pattern.CASE_INSENSITIVE);			
		Matcher matcherTag = pattern.matcher(rawData);
		while (matcherTag.find()) {				
			String backLink = matcherTag.group();
			backLinkItems.add(backLink);
		}
		
		return backLinkItems;
	}

	/** 
	 * Adds error message to task
	 * 
	 * @param projectId
	 * @param projectName
	 * @param taskId
	 * @param message
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static void addErrorMessageToTask(HPMUniqueID projectId, String projectName, HPMUniqueID taskId, String message, HansoftConnector hc) throws HPMSdkException, HPMSdkJavaException {		 
		HPMProjectCustomColumnsColumn errorColumnName = null;
		try {
			errorColumnName = HansoftIntegration.getErrorHansoftColumn(projectId, projectName);
		} catch (Exception e) {
			logger.error("Error getting error column name", e);			
		}
		
		if (errorColumnName == null) {		
			logger.error("Stop writting error to task with ID " + taskId + " in project " + projectName + " because value of error column name is null");
			return;
		}
		
		List<String> errors = getErrorItemsOfTask(taskId, errorColumnName);
		String currentDateTime = new SimpleDateFormat(Constants.DATE_TIME_FORMAT).format(new Date()).toString();
		errors.add(currentDateTime + " " + message);
		
		// put errors back to column
		StringBuffer buffer = new StringBuffer();
		for (String error : errors) {
			buffer.append(error);
			buffer.append(Constants.LS);
		}
		
		HansoftTaskFactory.updateCustomColumnData(projectId, hc.getHansoftSession(), errorColumnName, false, taskId, buffer.toString(), "", null);
	}
	
	/**
	 * 	Get column as hansoft object where error messages are stored
	 * @param projectId
	 * @param projectName
	 * @return
	 * @throws Exception
	 */
	public static HPMProjectCustomColumnsColumn getErrorHansoftColumn(HPMUniqueID projectId, String projectName) 
			throws Exception {
		
		// get error column name from configuration
		String errorColumnName;
		try {
			errorColumnName = ConfigurationControllerImpl.getInstance().getErrorColumnName(projectName);
		} catch (ConfigItemNotFound e) {
			logger.error("Columns for errors is not defined in Hansonf in project " + projectName, e);
			logger.info("Type of this column must be Multiline text");
			e.printStackTrace();
			throw e;
		}		
		
		List<HPMProjectCustomColumnsColumn> allCustomColumns = new ArrayList<HPMProjectCustomColumnsColumn>();
		// get all custom columns of project
		HPMProjectCustomColumns customColumns;
		try {
			customColumns = HansoftManager.getMainSession().ProjectCustomColumnsGet(projectId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Can't get Hansoft custom columns for project " + projectName, e);
			e.printStackTrace();
			throw e;
		}
		allCustomColumns.addAll(customColumns.m_ShowingColumns);
		allCustomColumns.addAll(customColumns.m_HiddenColumns);
		
		// find error column in custom columns
		for (HPMProjectCustomColumnsColumn customColumn : allCustomColumns) {
			if (customColumn.m_Name.equals(errorColumnName)) {
				return customColumn;
			}
		}
		
		// if column not found in custom columns, log error message
		String errorMessage = "Custom column called " + errorColumnName + " is not defined in project called " + projectName; 
		logger.error(errorMessage);
		throw new Exception(errorMessage);
	}

	/**
	 * Returns error messages of task
	 * @param taskId
	 * @param errorColumn
	 * @return
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	private static List<String> getErrorItemsOfTask(HPMUniqueID taskId, HPMProjectCustomColumnsColumn errorColumn) throws HPMSdkException, HPMSdkJavaException {
		List<String> errorItems = new ArrayList<String>();

		String rawData = HansoftManager.getMainSession().TaskGetCustomColumnData(taskId, errorColumn.m_Hash);
		if (StringUtils.isEmpty(rawData)) {
			return errorItems;
		}

		if (errorColumn.m_Type == EHPMProjectCustomColumnsColumnType.MultiLineText) {
			String[] lines = rawData.split(Constants.LS);
			for (String line : lines) {
				if (line.trim().equals("")) {
					continue;
				}
				errorItems.add(line.trim());
			}
		}
		return errorItems;
	}  
}
