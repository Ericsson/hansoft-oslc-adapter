
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.EHPMError;
import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnType;
import se.hansoft.hpmsdk.HPMChangeCallbackData_CommunicationChannelsChanged;
import se.hansoft.hpmsdk.HPMChangeCallbackData_DynamicCustomSettingsNotification;
import se.hansoft.hpmsdk.HPMChangeCallbackData_DynamicCustomSettingsValueChanged;
import se.hansoft.hpmsdk.HPMChangeCallbackData_GlobalCustomSettingsChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_GlobalCustomSettingsValueChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectActiveDefaultColumnsChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectCustomSettingsValueChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectDetailedAccessRulesChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectResourcePropertiesChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectViewPresetsChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ResourceGroupsChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ResourceLockFlagsChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_ResourcePropertiesChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_TaskChange;
import se.hansoft.hpmsdk.HPMChangeCallbackData_TaskChangeCustomColumnData;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumnDropListItem;
import se.hansoft.hpmsdk.HPMSdkCallbacks;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.configuration.ConfigurationControllerImpl;
import com.ericsson.eif.hansoft.exception.ConfigItemNotFound;
import com.ericsson.eif.hansoft.integration.IntegrationController;
import com.ericsson.eif.hansoft.integration.RulesController;
import com.ericsson.eif.hansoft.utils.HansoftUtils;
import com.ericsson.eif.hansoft.utils.StringUtils;

public class HansoftCallbacks extends HPMSdkCallbacks {

    private static final Logger logger = Logger.getLogger(HansoftCallbacks.class.getName());
	private boolean isManuallyTriggeredEvent = false;

    /* (non-Javadoc)
     * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_ProcessError(se.hansoft.hpmsdk.EHPMError)
     */
    @Override
    public void On_ProcessError(EHPMError _Error) {
    	logger.info("On_ProcessError");

        if (_Error.equals(EHPMError.ConnectionLost)) {
            HansoftManager.clearSession();
        }

        logger.error("Error reported from Hansoft: " + HPMSdkSession.ErrorAsStr(_Error));
    }

    /* (non-Javadoc)
     * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_CommunicationChannelsChanged(se.hansoft.hpmsdk.HPMChangeCallbackData_CommunicationChannelsChanged)
     */
    @Override
    public void On_CommunicationChannelsChanged(
            HPMChangeCallbackData_CommunicationChannelsChanged _Data) {
        // This is sent during init of session. Handle just to verify callback
        // is working.
    	logger.info("On_CommunicationChannelsChanged");
        super.On_CommunicationChannelsChanged(_Data);
    }

    /* (non-Javadoc)
     * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_ProjectActiveDefaultColumnsChange(se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectActiveDefaultColumnsChange)
     */
    @Override
    public void On_ProjectActiveDefaultColumnsChange(
            HPMChangeCallbackData_ProjectActiveDefaultColumnsChange _Data) {
    	logger.info("On_ProjectActiveDefaultColumnsChange");
        super.On_ProjectActiveDefaultColumnsChange(_Data);
    }

    /* (non-Javadoc)
     * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_ProjectResourcePropertiesChange(se.hansoft.hpmsdk.HPMChangeCallbackData_ProjectResourcePropertiesChange)
     */
    @Override
    public void On_ProjectResourcePropertiesChange(
            HPMChangeCallbackData_ProjectResourcePropertiesChange _Data) {

        // Called when the properties for a Resource (user) is changed e.g.
        // access rights. So clear cached values for that user and project
        // if exists
    	logger.info("On_ProjectResourcePropertiesChange");
        HansoftConnector.releaseAccessCache(_Data.m_ResourceID, _Data.m_ProjectID);

        super.On_ProjectResourcePropertiesChange(_Data);
    }
  	
    /* (non-Javadoc)
     * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_TaskChangeCustomColumnData(se.hansoft.hpmsdk.HPMChangeCallbackData_TaskChangeCustomColumnData)
     */
    @Override
    public void On_TaskChangeCustomColumnData(HPMChangeCallbackData_TaskChangeCustomColumnData _Data) {
  	super.On_TaskChangeCustomColumnData(_Data);

    if (!HansoftUtils.taskExists(_Data.m_TaskID)) {
    	return;
    }
    
  	HPMUniqueID taskId = null;
  	HPMProjectCustomColumnsColumn column = null;
	String projectName = "";
    	try {
    		HPMUniqueID userId = _Data.m_ChangedByResourceID;
    		HPMUniqueID impersonatedUserId = _Data.m_ChangedByImpersonatedResourceID;
    		taskId = _Data.m_TaskID;
    		HPMUniqueID projectId = HansoftUtils.getProjectIdOfTask(taskId);
    		HPMUniqueID projectIdReal = HansoftUtils.getRealProjectIdOfProjectId(projectId);
    		projectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), projectIdReal);

    		// ignore changes in project that we do not have in H2HConfig.xml 
    		if (!ConfigurationControllerImpl.getInstance().isProjectInConfig(projectName)) {
    			return;
    		}
    		
    		Project project = ConfigurationControllerImpl.getInstance().getProjectByName(projectName);
    		column = HansoftUtils.getHansoftCustomColumnByHash(projectId, _Data.m_ColumnHash);
    		String changedColumnName = column.m_Name;
    		logger.debug("Trying to synchronize changed column " + changedColumnName);

    		taskChanged(project, changedColumnName, userId, impersonatedUserId, taskId, projectIdReal);
    	
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		} finally {
			// if event was change of manual trigger column
			if (isManuallyTriggeredEvent) {
				isManuallyTriggeredEvent = false;
				logger.debug("isManuallyTriggeredEvent set to false");
				try {
					// un-check manual trigger column
					String projectUserName = ConfigurationControllerImpl.getInstance().getProjectUserName(projectName);
					String data = HansoftConnector.getHSConnectorForUser(projectUserName).getHansoftSession().TaskGetCustomColumnData(taskId, column.m_Hash);
					if (StringUtils.isNotEmpty(data)) {
						logger.debug("Manual Trigger data: " + data);
						HansoftConnector.getHSConnectorForUser(projectUserName).getHansoftSession().TaskSetCustomColumnData(taskId, column.m_Hash, "", false);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					e.printStackTrace();
				}
			}
		}
    }
    
	/* (non-Javadoc)
	 * @see se.hansoft.hpmsdk.HPMSdkCallbacks#On_TaskChange(se.hansoft.hpmsdk.HPMChangeCallbackData_TaskChange)
	 */
	@Override
    public void On_TaskChange(HPMChangeCallbackData_TaskChange _Data) {
        super.On_TaskChange(_Data);

        if (!HansoftUtils.taskExists(_Data.m_TaskID)) {
        	return;
        }
        
    	try {    		
    		HPMUniqueID userId = _Data.m_ChangedByResourceID;
    		HPMUniqueID impersonatedUserId = _Data.m_ChangedByImpersonatedResourceID;
    		HPMUniqueID taskId = _Data.m_TaskID;
    		HPMUniqueID projectId = HansoftUtils.getProjectIdOfTask(taskId);
    		HPMUniqueID projectIdReal = HansoftUtils.getRealProjectIdOfProjectId(projectId);
    		String projectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), projectIdReal);
    		
    		// ignore changes in project that we do not have in H2HConfig.xml 
    		if (!ConfigurationControllerImpl.getInstance().isProjectInConfig(projectName)) {
    			return;
    		}
    		    		
    		String changedColumnName = _Data.m_FieldChanged.name();
    		logger.debug("Trying to synchronize changed column " + changedColumnName);
    		
    		Project project = ConfigurationControllerImpl.getInstance().getProjectByName(projectName);
   			taskChanged(project, changedColumnName, userId, impersonatedUserId, taskId, projectIdReal);
    	
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
    }
    
    /**
     * @param m_ChangedByImpersonatedResourceID
     * @return user name or empty string in case of error
     */
    private String getUserName(HPMUniqueID m_ChangedByImpersonatedResourceID) {
    	try {
			return HansoftManager.getMainSession().ResourceGetNameFromResource(m_ChangedByImpersonatedResourceID);
		} catch (Exception e) {
		}
    	return "";
	}

	/**
	 * @param project
	 * @param columnName
	 * @param userId
	 * @param impersonatedUserId
	 * @param taskId
	 * @param realProjectId
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws Exception
	 */
	private void taskChanged(Project project, String columnName, HPMUniqueID userId, HPMUniqueID impersonatedUserId,
			HPMUniqueID taskId, HPMUniqueID realProjectId) throws HPMSdkException, HPMSdkJavaException, Exception {

		if (isSyncEvent(project, columnName, userId.m_ID, impersonatedUserId)) {
			syncTask(project, columnName, taskId, realProjectId);
		}
	}
	
	/**
	 * @param project
	 * @param changedColumnName
	 * @param userId
	 * @param taskId
	 * @param realProjectId
	 * @param calledFromScheduler
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws Exception
	 */
	public void syncTask(Project project, String changedColumnName, HPMUniqueID taskId, HPMUniqueID realProjectId) 
					throws HPMSdkException,	HPMSdkJavaException, Exception {

		String manualTriggerColumnName = "";
		isManuallyTriggeredEvent = false;
		
		try {
			manualTriggerColumnName = ConfigurationControllerImpl.getInstance().getManualSyncColumnName(project.getProjectName());
			if (changedColumnName.equalsIgnoreCase(manualTriggerColumnName)) {
				isManuallyTriggeredEvent = true;
				logger.debug("manuallyTriggered event");
			}
		} catch (ConfigItemNotFound e) {
		}
		
		List<Sync> allSyncs = ConfigurationControllerImpl.getInstance().getSyncsForProject(project.getProjectName());
		List<Sync> syncCandidates = new ArrayList<Sync>();

		for (Sync sync : allSyncs) {
			if (isManuallyTriggeredEvent && Constants.TRIGGER_MANUAL.equalsIgnoreCase(sync.getTrigger())) {
				syncCandidates.add(sync); 
			} else if (!isManuallyTriggeredEvent && ((Constants.TRIGGER_AUTOMATIC.equalsIgnoreCase(sync.getTrigger()) && !sync.hasActiveScheduler()) || sync.isKeptInSync())) {
				syncCandidates.add(sync);
			}
		}

		String projectUserName = ConfigurationControllerImpl.getInstance().getProjectUserName(project.getProjectName());
		HansoftConnector hc = HansoftConnector.getHSConnectorForUser(projectUserName);
		
		RulesController rulesController = new RulesController();
		List<Sync> selectedSyncs = rulesController.filterSyncs(taskId, realProjectId, project.getProjectName(), hc, syncCandidates, false); 
		
		if (selectedSyncs.isEmpty()) {
			// just log 
			logger.debug("column " + changedColumnName + " will not be synced");
			logger.debug("rulesController did not return any project");
		} else {
			// sync task
			IntegrationController integration = new IntegrationController();
			integration.syncTask(taskId, realProjectId, project, changedColumnName, hc, selectedSyncs);
		}
	}
	
	/**
	 * @param projectName
	 * @param columnName
	 * @param userId
	 * @param impersonatedUserId
	 * @return
	 * @throws Exception
	 */
	private boolean isSyncEvent(Project project, String columnName, int userId, HPMUniqueID impersonatedUserId) throws Exception {		

		if (userId == -1) {
			logger.debug("userId is -1, this means no synchronization will be done for column " + columnName);
        	return false;
		}
		
		if (isInternalHansoftColumn(columnName)) {
			logger.debug("column " + columnName + " was recognized as internal hansoft column and will not be synced");			
			return false;
		}		
		
		String backLinkColumnName = ConfigurationControllerImpl.getInstance().getBacklinkColumnName(project.getProjectName());
		if (columnName.equalsIgnoreCase(backLinkColumnName)) {
			logger.debug("column " + columnName + " is used for backlinks and will not be synced");			
			return false;
		}
		
		String errorColumnName = ConfigurationControllerImpl.getInstance().getErrorColumnName(project.getProjectName());
		if (columnName.equalsIgnoreCase(errorColumnName)) {
			logger.debug("column " + columnName + " is used for errors and will not be synced");
			return false;
		}
				
		
		String manualTriggerColumnName = ConfigurationControllerImpl.getInstance().getManualSyncColumnName(project.getProjectName());
		// check if changed column is mapped into any project where it is possible to sync from given project
		if (!columnName.equalsIgnoreCase(manualTriggerColumnName) && !isColumnInMapping(columnName, project)) {
			logger.debug("column " + columnName + " is not mapped in source project and will not be synced");
			return false;
		}
		
		return true;
	}

	/**
	 * Checks if column is mapped into any project where it is possible to sync from given project
	 * @param columnName
	 * @param project from which we want to sync
	 * @return true, if column is in mapping, otherwise false 
	 */
	private boolean isColumnInMapping(String columnName, Project project) {		
		for (Sync s : project.getSyncList()) {
			 for (MappingPair mp : s.getMappingPairList()) {
				  if (mp.getLocalPart().equals(columnName)) {
					  return true;
				  }
			 }
		}
		return false;
	}
	
	/**
	 * Check if column name is internal hansoft column
	 * - means it is set automatically by hansoft SDK when task is created or updated
	 * - if any of these column is changed in mother project, we do not need to send an update
	 *   to child project because this will be done automatically by hansoft on child side  
	 * @param columnName
	 * @return
	 */
	private boolean isInternalHansoftColumn(String columnName) {
		if (columnName.equalsIgnoreCase("LockedBy")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("TotalDuration")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("TimeZones")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("OriginallyCreatedBy")) {		
			return true;
		}
				
		if (columnName.equalsIgnoreCase("LastUserInterfaceAction")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("WallItemPositions")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("WallItemColor")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("Undefined")) {			
			return true;
		}
		
		if (columnName.equalsIgnoreCase("FullyCreated")) {			
			return true;
		}
		
		return false;
	}


	/**
     * check if changed column is manual sync column
     * 
     * @param projectId
     * @param column
     * @throws Exception
     */
	private boolean isManualSyncChanged(String projectName, HPMProjectCustomColumnsColumn column) throws Exception {
		String manualSyncColumnName = ConfigurationControllerImpl.getInstance().getManualSyncColumnName(projectName);
		if (StringUtils.isEmpty(manualSyncColumnName)) {
			return false;
		}
		
		if (column.m_Name.contentEquals(manualSyncColumnName) && column.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
			return true;
		}
		
		return false;
	}
	
	/**
     * check if changed column is auto sync column
     * 
	 * @param projectName
	 * @param column
	 * @throws Exception
	 */
	private boolean isAutoSyncChanged(String projectName, HPMProjectCustomColumnsColumn column) throws Exception {
		String autoSyncColumnName = ConfigurationControllerImpl.getInstance().getAutoSyncColumnName(projectName);
		if (StringUtils.isEmpty(autoSyncColumnName)) {
			return false;
		}
		
		if (column.m_Name.contentEquals(autoSyncColumnName) && column.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param taskId
	 * @param projectId
	 * @param projectName 
	 * @throws Exception
	 */
	private List<String> getAutoSyncSelectedValues(HPMUniqueID taskId, HPMUniqueID projectId, String projectName) throws Exception {
		
		String autoSyncColumnName;
		try {
			autoSyncColumnName = ConfigurationControllerImpl.getInstance().getAutoSyncColumnName(projectName);
		} catch (ConfigItemNotFound e) {
			logger.error("Auto Sync column name not found in configuration.", e);
			e.printStackTrace();
			throw e;
		}
		
		HPMProjectCustomColumnsColumn autoSyncColumn = HansoftUtils.getHansoftCustomColumnByName(projectId, projectName, autoSyncColumnName);
		if (autoSyncColumn == null) {
			throw new Exception("Custom column called " + autoSyncColumnName + " is no defined in project called " + projectName);
		}
		
    	List<String> selectedValues = new ArrayList<String>();

    	String rawData = HansoftManager.getMainSession().TaskGetCustomColumnData(taskId, autoSyncColumn.m_Hash);
    	if (StringUtils.isEmpty(rawData)) {
    		return selectedValues;
    	}
    	
    	if (autoSyncColumn.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
			String[] ids = rawData.split(";");
	
			for (String idString : ids) {
				int id;
				try {
					id = Integer.parseInt(idString);
				} catch (NullPointerException e) {
					continue;
				}
	    		ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = autoSyncColumn.m_DropListItems;
				for (HPMProjectCustomColumnsColumnDropListItem item : items) {
					if (item.m_Id == id) {
						selectedValues.add(item.m_Name);
						break;
					}
				}				
			}
    	}
		
    	return selectedValues;
	}

	/**
	 * @param taskId
	 * @param columnHash
	 * @param column
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	private List<String> getSelectedValues(HPMUniqueID taskId, int columnHash, HPMProjectCustomColumnsColumn column) throws HPMSdkException, HPMSdkJavaException {
    	List<String> selectedValues = new ArrayList<String>();

    	String rawData = HansoftManager.getMainSession().TaskGetCustomColumnData(taskId, columnHash);
    	if (StringUtils.isEmpty(rawData)) {
    		return selectedValues;
    	}
    	
    	if (column.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
			String[] ids = rawData.split(";");
	
			for (String idString : ids) {
				int id;
				try {
					id = Integer.parseInt(idString);
				} catch (NullPointerException e) {
					continue;
				}
	    		ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = column.m_DropListItems;
				for (HPMProjectCustomColumnsColumnDropListItem item : items) {
					if (item.m_Id == id) {
						selectedValues.add(item.m_Name);
						break;
					}
				}				
			}
    	}
		
    	return selectedValues;
	}
    
	/**********************************************************************************
     * Callbacks below are for testing purposes - remove when not needed
     **********************************************************************************/

    @Override
    public void On_ProjectDetailedAccessRulesChange(
            HPMChangeCallbackData_ProjectDetailedAccessRulesChange _Data) {
    	logger.info("On_ProjectDetailedAccessRulesChange");
        super.On_ProjectDetailedAccessRulesChange(_Data);
    }

    @Override
    public void On_ResourceGroupsChange(
            HPMChangeCallbackData_ResourceGroupsChange _Data) {
    	logger.info("On_ResourceGroupsChange");
        super.On_ResourceGroupsChange(_Data);
    }

    @Override
    public void On_ResourcePropertiesChange(
            HPMChangeCallbackData_ResourcePropertiesChange _Data) {
    	logger.info("On_ResourcePropertiesChange");
        super.On_ResourcePropertiesChange(_Data);
    }

    @Override
    public void On_DynamicCustomSettingsNotification(
            HPMChangeCallbackData_DynamicCustomSettingsNotification _Data) {
    	logger.info("On_DynamicCustomSettingsNotification");
        super.On_DynamicCustomSettingsNotification(_Data);
    }

    @Override
    public void On_DynamicCustomSettingsValueChanged(
            HPMChangeCallbackData_DynamicCustomSettingsValueChanged _Data) {
    	logger.info("On_DynamicCustomSettingsValueChanged");
        super.On_DynamicCustomSettingsValueChanged(_Data);
    }

    @Override
    public void On_GlobalCustomSettingsChange(
            HPMChangeCallbackData_GlobalCustomSettingsChange _Data) {
    	logger.info("On_GlobalCustomSettingsChange");
        super.On_GlobalCustomSettingsChange(_Data);
    }

    @Override
    public void On_GlobalCustomSettingsValueChange(
            HPMChangeCallbackData_GlobalCustomSettingsValueChange _Data) {
    	logger.info("On_GlobalCustomSettingsValueChange");
        super.On_GlobalCustomSettingsValueChange(_Data);
    }

    @Override
    public void On_ProjectCustomSettingsValueChange(
            HPMChangeCallbackData_ProjectCustomSettingsValueChange _Data) {
    	logger.info("On_ProjectCustomSettingsValueChange");
        super.On_ProjectCustomSettingsValueChange(_Data);
    }

    @Override
    public void On_ProjectViewPresetsChange(
            HPMChangeCallbackData_ProjectViewPresetsChange _Data) {
    	logger.info("On_ProjectViewPresetsChange");
        super.On_ProjectViewPresetsChange(_Data);
    }

    @Override
    public void On_ResourceLockFlagsChange(
            HPMChangeCallbackData_ResourceLockFlagsChange _Data) {
    	logger.info("On_ResourceLockFlagsChange");
        super.On_ResourceLockFlagsChange(_Data);
    }
}
