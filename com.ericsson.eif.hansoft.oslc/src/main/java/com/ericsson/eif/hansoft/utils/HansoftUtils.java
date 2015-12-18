package com.ericsson.eif.hansoft.utils;

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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.EHPMFilterType;
import se.hansoft.hpmsdk.EHPMFindSelectionType;
import se.hansoft.hpmsdk.EHPMReportViewType;
import se.hansoft.hpmsdk.EHPMTaskFindFlag;
import se.hansoft.hpmsdk.HPMFindCondition;
import se.hansoft.hpmsdk.HPMFindContext;
import se.hansoft.hpmsdk.HPMFindContextData;
import se.hansoft.hpmsdk.HPMProjectCustomColumns;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMProjectEnum;
import se.hansoft.hpmsdk.HPMProjectProperties;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMTaskEnum;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.integration.HansoftIntegration;
import com.ericsson.eif.hansoft.mapping.AttributesMapper;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;

/**
 * @author xluksva
 *
 */
public class HansoftUtils {

    private static final Logger logger = Logger.getLogger(HansoftUtils.class
            .getName());
    
    /** 
     * Gets project ID from project name
     * @param projectName
     * @return projectId or null in case of error or project was not found in hansoft database
     *  Comparison of projectName and project names in hansoft database is not case sensitive 
     */
    public static HPMUniqueID getProjectIdFromProjectName(String projectName) {    	    	
    	try {
    		HPMSdkSession session = HansoftManager.getMainSession();
    		HPMProjectEnum projectEnum = session.ProjectEnum();
    		
    		// iterate all projects in hansoft database and check their names
    		for (HPMUniqueID projectId : projectEnum.m_Projects) {		
				if (HansoftUtils.getProjectNameByProjectID(session, projectId)
						.equalsIgnoreCase(projectName)) {
					 return projectId;
				}
    		}
		} catch (Exception e) {
			logger.error("Error while getting projectId from project name " + projectName, e);
		}
    	
    	return null;		
    }
    
    /**
     * Gets project id from String
     * @param projectIdString  	  
     * @return ProjectId as HansoftObject created from string or null
     */    
    public static HPMUniqueID getProjectId(String projectIdString) {
        HPMUniqueID projectId = new HPMUniqueID();
        try {
            projectId.m_ID = Integer.parseInt(projectIdString);
        } catch (NumberFormatException e) {
        	logger.error("Failed to parse project id from string.", e);
            logger.debug("projectIdString " + projectIdString);
            return null;
        }    
            
        if (!projectId.IsValid()) {
        	logger.debug("Not valid projectId " + projectId);
            return null;
         }
        
        return projectId;
    }

    /**
     * Gets task Id from string
     * @param taskIdString
     * @return TaskId as HansoftObject created from string, or null in case of error
     */
    public static HPMUniqueID getTaskId(String taskIdString) {
        HPMUniqueID taskId = new HPMUniqueID();        
        try {
        	taskId.m_ID = Integer.parseInt(taskIdString);
        } catch (NumberFormatException e) {
        	logger.error("Failed to parse task id from string.", e);
            logger.debug("taskIdString " + taskIdString);
            return null;
        }        
                
        if (!HansoftUtils.taskExists(taskId)) {
        	logger.debug("TaskId " + taskId + " does not exist");
            return null;
        }
        
        return taskId;
    }
    
    /**
     * Utility for finding all tasks in a project (agile backlog) with the
     * specified title.
     * 
     * @param title
     * @param session
     * @param taskId
     * @param projectId
     * @return tasks in project
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public static List<HPMUniqueID> findTasks(String title,
            HPMSdkSession session, HPMUniqueID projectId)
            throws HPMSdkException, HPMSdkJavaException {

        HPMFindContext context = new HPMFindContext();
        HPMFindCondition condition = new HPMFindCondition();
        condition.m_SelectionType = EHPMFindSelectionType.Fixed;
        condition.m_SelectionID = EHPMFilterType.AreBacklogItems.ordinal();
        ArrayList<HPMFindCondition> findConditions = new ArrayList<HPMFindCondition>();
        findConditions.add(condition);
        context.m_Conditions = findConditions;

        HPMFindContextData contextData = session.UtilPrepareFindContext(title,
                projectId, EHPMReportViewType.AgileBacklog, context);

        // None or/and Archived
        EnumSet<EHPMTaskFindFlag> findFlag = EnumSet.of(EHPMTaskFindFlag.None);
        HPMTaskEnum findResult = session.TaskFind(contextData, findFlag);

        List<HPMUniqueID> results = new ArrayList<HPMUniqueID>();
        for (HPMUniqueID taskId2 : findResult.m_Tasks) {
            results.add(taskId2);
        }

        return results;
    }
    
    /**
     * @param session
     * @param projectId
     * @return Project properties
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public static HPMProjectProperties getProjectPropertiesByProjectID(HPMSdkSession session, 
    		HPMUniqueID projectId) throws HPMSdkException, HPMSdkJavaException {   	
    	    
    	if (!projectId.IsValid()) {
    		logger.debug("Project with Id " + projectId + " is not valid");
    		return null;    				
    	}
    	
    	HPMProjectProperties projectProp = null;
    	try {
    		 projectProp = session.ProjectGetProperties(projectId);
    	}
    	catch(HPMSdkException | HPMSdkJavaException e) {    		
    		logger.error("Error while getting project properties for project id " + projectId, e);
    		logger.debug("Session  " + session);
			logger.debug("Project Id " + projectId);    		    		
    		throw e;
    	}    	
    	
    	return projectProp;
    }    
    
    /**
     * @param session
     * @param projectId
     * @return project name as string 
     * @throws Exception
     */
    public static String getProjectNameByProjectID(HPMSdkSession session, HPMUniqueID projectId) throws Exception {
    	    	
        if (!projectId.IsValid()) {
        	logger.debug("Not valid projectId " + projectId);
            return null;
         }
        
        HPMProjectProperties projectProp = null;
        
		try {
			projectProp = HansoftUtils.getProjectPropertiesByProjectID(session, projectId);
		} catch (Exception e) {
			logger.error("Error while getting project name from project id " + projectId, e);
			logger.debug("Session  " + session);
			logger.debug("Project Id " + projectId);			
			throw e;
		}
		
    	return projectProp.m_NiceName;
    }
    
    /**
     * @param resourceName
     * @return Hansoft object from resource name  or null
     * @throws Exception
     */
    public static HPMUniqueID getResourceFromName(String resourceName) throws Exception {
    	HPMUniqueID resourceID = null;
    	HPMSdkSession session = null;
    	
    	try {
    		session = HansoftManager.getMainSession();
			resourceID = session.ResourceGetResourceFromName(resourceName);
		} catch (Exception e) {
			logger.error("Error while getting resource from name " + resourceName, e);
			logger.debug("Session  " + session);
			logger.debug("Resource Name  " + resourceName);			
			throw e;
		}
    	
    	return resourceID;
    }

    
	/**
	 * @param projectId
	 * @param projectName
	 * @param columnName
	 * @return Hansoft Column objects or null
	 * @throws Exception
	 */
	public static HPMProjectCustomColumnsColumn getHansoftCustomColumnByName(
			HPMUniqueID projectId, String projectName, String columnName)
			throws Exception {

		List<HPMProjectCustomColumnsColumn> allCustomColumns = new ArrayList<HPMProjectCustomColumnsColumn>();
		// get all custom columns of project
		HPMProjectCustomColumns customColumns;
		
		try {
			customColumns = HansoftManager.getMainSession().ProjectCustomColumnsGet(projectId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Can't get Hansoft custom columns for project " + projectName, e);
			logger.debug("Project Id " + projectId);
			logger.debug("Project Name " + projectName);
			logger.debug("Column Name " + columnName);			
			throw e;
		}
		
		allCustomColumns.addAll(customColumns.m_ShowingColumns);
		allCustomColumns.addAll(customColumns.m_HiddenColumns);

		for (HPMProjectCustomColumnsColumn customColumn : allCustomColumns) {
			if (customColumn.m_Name.equals(columnName)) {
				return customColumn;
			}
		}
		
		return null;
	}
	
	/**
	 * @param projectId
	 * @param columnHash
	 * @return Hansoft Column objects or null
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static HPMProjectCustomColumnsColumn getHansoftCustomColumnByHash(HPMUniqueID projectId, int columnHash) throws HPMSdkException, HPMSdkJavaException {
		HPMProjectCustomColumnsColumn column = null;		
		
		try {
			column = HansoftManager.getMainSession().ProjectGetCustomColumn(projectId, columnHash);		
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Can't get Hansoft custom columns by hash.", e);
			logger.debug("Project Id " + projectId);
			logger.debug("Column Hash " + columnHash);
			throw e;
		}
		
		return column;
	}

	/**
	 * @param taskId
	 * @return project id as Hansoft object or null
	 *  see Hansoft API documentation for difference between project id and real project id
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static HPMUniqueID getProjectIdOfTask(HPMUniqueID taskId) throws HPMSdkException, HPMSdkJavaException {	
		
		if (!HansoftUtils.taskExists(taskId)) {
			logger.debug("Task " + taskId + "is not existing");
            return null;
		}
		
		HPMUniqueID projectId = null;		
		
		try {
			projectId = HansoftManager.getMainSession().TaskGetContainer(taskId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Error while getting project Id from task Id " + taskId, e);
			logger.debug("Task Id " + taskId);
			throw e;
		}
		
		return projectId;
	}
	
	/**
	 * @param projectId
	 * @return real project id or null
	 * see Hansoft API documentation for difference between project id and real project id
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static HPMUniqueID getRealProjectIdOfProjectId(HPMUniqueID projectId) throws HPMSdkException, HPMSdkJavaException {
		HPMUniqueID projectIdReal = null;
		
		try {
			projectIdReal = HansoftManager.getMainSession().UtilGetRealProjectIDFromProjectID(projectId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Error while getting real project Id from project Id " + projectId, e);
			logger.debug("Project Id " + projectId);
			throw e;
		}
		
		return projectIdReal;
	}
	
	/**
	 * @param taskId
	 * @return task main reference or null
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static HPMUniqueID getTaskReference(HPMUniqueID taskId) throws HPMSdkException, HPMSdkJavaException {
		
		if (!HansoftUtils.taskExists(taskId)) {
			logger.debug("Task " + taskId + " is not existing");
            return null;
		}			
		
		HPMUniqueID taskRefMain = null;
		
		try {
			taskRefMain = HansoftManager.getMainSession().TaskGetMainReference(taskId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Error while getting task reference from task Id " + taskId, e);
			logger.debug("Task Id " + taskId);
			throw e;
		}
		
		return taskRefMain;
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param attributeLabel
	 * @param attributeNS
	 * @return value of given attribute
	 */
	public static Object getValueOfAttribute(HansoftChangeRequest hansoftChangeRequest, String attributeLabel, String attributeNS) {
		Object value = getValueOfNativeAttribute(hansoftChangeRequest, attributeLabel, attributeNS);
		if (value == null) {
			value = getValueOfExtendedProperty(hansoftChangeRequest, AttributesMapper.getInstance().getPropertyNameFromColumnName(attributeLabel), attributeNS); 
		}
		return value;
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param attributeName
	 * @param attributeNS
	 * @return value of hansoft native attribute or null in case attribute is not native
	 */
	private static Object getValueOfNativeAttribute(HansoftChangeRequest hansoftChangeRequest, String attributeName, String attributeNS) {
		String completePath = attributeNS + attributeName;
		
		// Item name
		if(Constants.COLUMN_ITEM_NAME.equalsIgnoreCase(completePath) || Constants.DCTERMS_TITLE.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getTitle();
	    // Description
		} else if (Constants.COLUMN_DESCRIPTION.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getDescription();
	    // Priority
		} else if (Constants.COLUMN_PRIORITY.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getPriority();
		// Severity
		} else if (Constants.COLUMN_SEVERITY.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getSeverity();
		// Status
		} else if (Constants.COLUMN_STATUS.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getStatus();
		// WorkRemaining
		} else if (Constants.COLUMN_WORK_REMAINING.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getWorkRemaining();
		} else if (Constants.DCTERMS_IDENTIFIER.equalsIgnoreCase(completePath)) {
			return hansoftChangeRequest.getIdentifier();
		}
		
		return null;
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param propertyName
	 * @param propertyNS
	 * @return value of property or null
	 */
	private static Object getValueOfExtendedProperty(HansoftChangeRequest hansoftChangeRequest, String propertyName, String propertyNS) {
		Map<QName, Object> extProps = hansoftChangeRequest.getExtendedProperties();
		Set<QName> allQNames = extProps.keySet();

		for (QName qname : allQNames) {
			if (qname.getLocalPart().equalsIgnoreCase(propertyName) && qname.getNamespaceURI().equals(propertyNS)) {
				return extProps.get(qname);
			}
		}
		
		return null;
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param attributeLabel
	 * @param attributeNS
	 * @param value
	 */
	public static boolean setValueOfAttribute(HansoftChangeRequest hansoftChangeRequest, String attributeLabel, String attributeNS, Object value) {
		String completeName = attributeNS + attributeLabel;
		
		// setTitle
		if(Constants.COLUMN_ITEM_NAME.equalsIgnoreCase(completeName) || Constants.DCTERMS_TITLE.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getTitle();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setTitle((String) value);
					return true;
				}
			}
			return false;
	    // setDescription
		} else if (Constants.COLUMN_DESCRIPTION.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getDescription();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setDescription((String) value);
					return true;
				}
			}
			return false;
		// setPriority
		} else if (Constants.COLUMN_PRIORITY.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getPriority();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setPriority((String) value);
					return true;
				}
			}
			return false;
			// setSeverity
		} else if (Constants.COLUMN_SEVERITY.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getSeverity();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setSeverity((String) value);
					return true;
				}
			}
			return false;
			// setStatus
		} else if (Constants.COLUMN_STATUS.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getStatus();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setStatus((String) value);
					return true;
				}
			}
			return false;
			// setWorkRemaining
		} else if (Constants.COLUMN_WORK_REMAINING
				.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof Double) {
				Double originalValue = hansoftChangeRequest.getWorkRemaining();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setWorkRemaining((Double) value);
					return true;
				}
			}
			return false;
		} else if (Constants.DCTERMS_IDENTIFIER.equalsIgnoreCase(completeName)) {
			if (value != null && value instanceof String) {
				String originalValue = hansoftChangeRequest.getIdentifier();
				if (!value.equals(originalValue)) {
					hansoftChangeRequest.setIdentifier((String) value);
					return true;
				}
			}
			return false;
		}
		
		String encodedAttributeLabel = AttributesMapper.getInstance().getPropertyNameFromColumnName(attributeLabel);
		
		Map<QName, Object> extProps = hansoftChangeRequest.getExtendedProperties();
		Set<QName> allQNames = extProps.keySet();

		for (QName qname : allQNames) {
			if (qname.getLocalPart().equals(encodedAttributeLabel) && qname.getNamespaceURI().equals(attributeNS)) {
				Object originalValue = extProps.get(qname);
				if (value != null && !value.equals(originalValue)) {
					extProps.put(qname, value);
					return true;
				}
				return false;
			}
		}

		// if property was not found in hansoftChangeRequest, it has to be created
		QName qname = new QName(attributeNS, encodedAttributeLabel);
		hansoftChangeRequest.getExtendedProperties().put(qname, value);		
		return true;
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param propertyName
	 * @param propertyNS
	 * @param propertyValue
	 */
	private static void setValueOfExtendedProperty(HansoftChangeRequest hansoftChangeRequest, String propertyName, String propertyNS, Object propertyValue) {
		Map<QName, Object> extProps = hansoftChangeRequest.getExtendedProperties();
		Set<QName> allQNames = extProps.keySet();

		for (QName qname : allQNames) {
			if (qname.getLocalPart().equals(propertyName) && qname.getNamespaceURI().equals(propertyNS)) {
				extProps.put(qname, propertyValue);
				return;
			}
		}

		// if property was not found in hansoftChangeRequest, it has to be created
		QName qname = new QName(propertyNS, propertyName);
		hansoftChangeRequest.getExtendedProperties().put(qname, propertyValue);		
	}
	
	/**
	 * @param hansoftChangeRequest
	 * @param attributeName
	 * @param attributeNS
	 * @param value
	 * @return true, if value was set, otherwise false
	 */
	private static boolean setValueOfNativeAttribute(HansoftChangeRequest hansoftChangeRequest, String attributeName, String attributeNS, Object value) {
		String completeName = attributeNS + attributeName;
		
		// setTitle
		if(Constants.COLUMN_ITEM_NAME.equalsIgnoreCase(completeName) || Constants.DCTERMS_TITLE.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setTitle((String) value);
				return true;
			}
	    // setDescription
		} else if (Constants.COLUMN_DESCRIPTION.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setDescription((String) value);
				return true;
			}
		// setPriority
		} else if (Constants.COLUMN_PRIORITY.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setPriority((String) value);
				return true;
			}
		// setSeverity
		} else if (Constants.COLUMN_SEVERITY.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setSeverity((String) value);
				return true;
			}
		// setStatus				
		} else if (Constants.COLUMN_STATUS.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setStatus((String) value);
				return true;
			}
		// setWorkRemaining
		} else if (Constants.COLUMN_WORK_REMAINING.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof Double) {
				hansoftChangeRequest.setWorkRemaining((Double) value);
				return true;
			}
		} else if (Constants.DCTERMS_IDENTIFIER.equalsIgnoreCase(completeName)) {
			if (value == null || value instanceof String) {
				hansoftChangeRequest.setIdentifier((String) value);
				return true;
			}
		}
		
		return false;
	}
		
	/**
	 *  gets value of parameter connectionId from backLink
	 * @param backLink
	 *  
	 *  backLink is usually in following format
	 *  <URL=http://www.google.com?connectionId=123456><COLOR=80,138,255><UNDERLINE> some text</UNDERLINE></COLOR></URL>
	 *  
     * @return value of URL parameter connectionId or empty string
	 */
	public static String getConnectionIdFromBackLink(String backLink) {	
		if (StringUtils.isEmpty(backLink)) {
			logger.error("Error while getting connectionId from backLink " + backLink);
			logger.debug("backLink is null or empty");
			return "";
		}
		
		String decodedBackLink = "";
		try {
			decodedBackLink = URLDecoder.decode(backLink, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("Error while getting connectionId from backLink " + backLink);
			logger.error("Error during decoding backLink ", e);			
			return "";		
		}
		
		int selectedSyncIndex = decodedBackLink.indexOf(Constants.SELECTED_SYNC_URL_PARAMETER);
		if (selectedSyncIndex == -1) {
			logger.error("Error while getting connectionId from backLink " + backLink);
			logger.debug("backLink URL does not contain parameter " + Constants.SELECTED_SYNC_URL_PARAMETER);
			return "";
		}
		
		decodedBackLink = decodedBackLink.substring(selectedSyncIndex);
		// decoded backlink looks now like this  connectionId=1234><COLOR=80,138,255><UNDERLINE> some text</UNDERLINE></COLOR></URL>
		// or connectionId=1234&anotherParam=anothervalue><COLOR=80,138,255><UNDERLINE> some text</UNDERLINE></COLOR></URL>
		// now take vlaue of connectionId		
		StringBuilder sb = new StringBuilder();

		// skip characters from string connectionId= 
		int start = Constants.SELECTED_SYNC_URL_PARAMETER.length(); 
		for (int i = start; i < decodedBackLink.length(); i++){
		    char c = decodedBackLink.charAt(i);		    
		    if (c == '&' || c == '>') {
		    	break;
		    }	
		    
		    sb.append(c);
		}
		
		return sb.toString();
	}
	
	/**
	 *  .(OSLC_Adapter_Test3, ChildProject, <Hansoft server address>).
	 * @param syncTo
	 * @return Name of project from syncTo string
	 */
	public static String getProjectNameFromSync(String syncTo) {
		if (StringUtils.isEmpty(syncTo))
			return "";
		
		String[] parts = syncTo.split(",");
		return parts[1].trim();
	}

	/**
	 * Checks if task with given TaskId is existing in hansoft database.
	 * This is our own replacement of hansoft method taskId.IsValid()
	 * because even if task was deleted, taskId.IsValid() still returns TRUE.
	 * @param taskId
	 * @return true if task is still existing, otherwise false. (if task was deleted or is not existing)
	 */
	public static boolean taskExists(HPMUniqueID taskId) {
        try {
			HansoftManager.getMainSession().TaskGetMainReference(taskId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			return false;
		}
        return true;
	}
}
