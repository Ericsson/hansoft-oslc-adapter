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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.EHPMChannelFlag;
import se.hansoft.hpmsdk.EHPMDataHistoryClientOrigin;
import se.hansoft.hpmsdk.EHPMProjectDefaultColumn;
import se.hansoft.hpmsdk.EHPMProjectDetailedAccessRuleFunctionType;
import se.hansoft.hpmsdk.EHPMProjectDetailedAccessRuleType;
import se.hansoft.hpmsdk.EHPMProjectResourceFlag;
import se.hansoft.hpmsdk.EHPMResourceFlag;
import se.hansoft.hpmsdk.EHPMResourceGroupingType;
import se.hansoft.hpmsdk.EHPMTaskField;
import se.hansoft.hpmsdk.EHPMTaskType;
import se.hansoft.hpmsdk.HPMChangeCallbackData_AuthenticationResolveCredentialsResponse;
import se.hansoft.hpmsdk.HPMCommunicationChannelEnum;
import se.hansoft.hpmsdk.HPMCommunicationChannelProperties;
import se.hansoft.hpmsdk.HPMCredentialResolutionSessionIDsEnum;
import se.hansoft.hpmsdk.HPMProjectDetailedAccessRule;
import se.hansoft.hpmsdk.HPMProjectDetailedAccessRuleFunction;
import se.hansoft.hpmsdk.HPMProjectDetailedAccessRules;
import se.hansoft.hpmsdk.HPMProjectResourceProperties;
import se.hansoft.hpmsdk.HPMResourceDefinition;
import se.hansoft.hpmsdk.HPMResourceDefinitionList;
import se.hansoft.hpmsdk.HPMResourceEnum;
import se.hansoft.hpmsdk.HPMResourceProperties;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.exception.UnauthorizedException;
import com.ericsson.eif.hansoft.utils.HansoftUtils;

public class HansoftConnector {
    private static List<HansoftConnector> connectors = new ArrayList<HansoftConnector>();

    private HPMUniqueID userId;
    private HPMSdkSession session;
    private String userName;
    
    HPMProjectResourceProperties props; 

    private static final Logger logger = Logger
            .getLogger(HansoftConnector.class.getName());

    // Cache access rules for a project and a user
    private Map<HPMUniqueID, Set<Integer>> userCustomColumnAccess;
    private Map<HPMUniqueID, Set<EHPMProjectDefaultColumn>> userDefaultColumnAccess;
    private Map<HPMUniqueID, Set<EHPMTaskField>> userTaskFieldAccess;

    /**
     * @param credentials
     * @throws UnauthorizedException
     */
    static public HansoftConnector createAuthorized(Credentials credentials)
            throws UnauthorizedException {

        /*
         * From Hansoft SDK
         * 
         * If you have set the number of sessions parameter in SessionOpen to
         * zero and use virtual sessions, all virtual sessions will work towards
         * the same session. As long as your SDK program is doing mostly reads
         * and not spend to much time writing or on blocking operations you can
         * have quite a large number of virtual sessions without increasing the
         * size of the session pool. Our recommendation is that you start by
         * setting the number of sessions parameter to zero and increase it if
         * you start experiencing slowdowns.
         */

        // Code partly inspired from Login.aspx.cs from Hansoft samples

        HansoftConnector hc = null;

        try {
            HPMSdkSession m_VirtSession = HansoftManager.getVirtualSession();

            HPMCommunicationChannelEnum channels = m_VirtSession.CommunicationChannelEnum("");
            if (channels == null)
                throw new UnauthorizedException("Failed to get communication channels.");

            HPMCredentialResolutionSessionIDsEnum sessionIDs = new HPMCredentialResolutionSessionIDsEnum();
            List<Long> ids = new ArrayList<Long>();
            for (HPMCommunicationChannelProperties channelProps : channels.m_Channels) {
                if (channelProps.m_Flags.contains(EHPMChannelFlag.SupportsResolveCredentials)) {
                    ids.add(channelProps.m_OwnerSessionID);
                }
            }
            
            sessionIDs.m_SessionIDs = new long[ids.size()];
            
            for (int i = 0; i < ids.size(); i++) {
                sessionIDs.m_SessionIDs[i] = ids.get(i);
            }

            HPMChangeCallbackData_AuthenticationResolveCredentialsResponse response = m_VirtSession.AuthenticationResolveCredentialsBlock(
                            credentials.getUsername(),
                            credentials.getPassword(), sessionIDs);
            HPMUniqueID m_ResourceID = response.m_ResourceID;

            if (m_ResourceID.m_ID == -1) {
                throw new UnauthorizedException("Invalid user (" + credentials.getUsername() + ") or password.");
            }

            hc = new HansoftConnector(m_VirtSession, m_ResourceID, credentials.getUsername());

        } catch (HPMSdkJavaException e) {
            logger.error("Hansoft Java SDK Exception in createAuthorized: " + e.ErrorAsStr());
        } catch (HPMSdkException e) {
            logger.error("Hansoft SDK Exception in createAuthorized: " + e.ErrorAsStr());
        }

        return hc;
    }

    /**
     * Get an authorized HansoftConnector from the HttpSession
     * 
     * The connector should be placed in the session by the CredentialsFilter
     * servlet filter.
     * 
     * @param request
     * @return connector
     */
    public static HansoftConnector getAuthorized(HttpServletRequest request) {
        // Connector should never be null if CredentialsFilter is doing its job
        HansoftConnector connector = (HansoftConnector) request.getSession()
                .getAttribute(CredentialsFilter.CONNECTOR_ATTRIBUTE);
        if (connector == null) {
            logger.error("Hansoft Connector not initialized - check adapter.properties");
        }
        return connector;
    }

//    /**
//     * protected constructor
//     * @param session
//     * @param resourceID
//     */
//    protected HansoftConnector(HPMSdkSession session, HPMUniqueID resourceID) {
//    	new HansoftConnector(session, resourceID, "");
//    }
    
    protected HansoftConnector(HPMSdkSession session, HPMUniqueID resourceID, String userName) {
    	this.userName = userName;
        this.userId = resourceID;
        this.session = session;

        userDefaultColumnAccess = new HashMap<HPMUniqueID, Set<EHPMProjectDefaultColumn>>();
        userCustomColumnAccess = new HashMap<HPMUniqueID, Set<Integer>>();
        userTaskFieldAccess = new HashMap<HPMUniqueID, Set<EHPMTaskField>>();

        synchronized (connectors) {
            connectors.add(this);
        }
    }
    
    public static HansoftConnector getHSConnectorForUser(String userName) throws Exception {
    	for (HansoftConnector connector : connectors) {
    		if (connector.getUserName().equals(userName)) {
    			return connector;
    		}
    	}
    	HPMUniqueID userId = HansoftUtils.getResourceFromName(userName);
    	HansoftConnector connector = new HansoftConnector(HansoftManager.getVirtualSession(), userId, userName);
    	connector.setImpersonate();
    	return connector;
    }

    /**
     *  releases all hansoft connectors
     */
    static public void releaseAll() {
        synchronized (connectors) {
            for (int i = connectors.size() - 1; i > 0; i--) {
                connectors.get(i).release();
            }
        }
    }

    /**
     * Release the cached info about the user access.
     * 
     * @param userId
     * @param projectId
     */
    static public void releaseAccessCache(HPMUniqueID userId,
            HPMUniqueID projectId) {
        synchronized (connectors) {
            for (int i = 0; i < connectors.size(); i++) {
                if (connectors.get(i).getUsedId().equals(userId)) {
                    connectors.get(i).releaseCachedInfo(projectId);
                }
            }
        }
    }

    /**
     * release http session
     * @param session
     */
    static public void release(HttpSession session) {
        HansoftConnector hc = (HansoftConnector) session
                .getAttribute(CredentialsFilter.CONNECTOR_ATTRIBUTE);
        if (hc != null)
            hc.release();
    }

    /**
     * releases connectors
     */
    public void release() {
        synchronized (connectors) {
            if (connectors.contains(this)) {
                connectors.remove(this);
            }
        }
    }

    /*
     * From Hansoft SDK
     * 
     * EHPMResourceFlag_None TBD EHPMResourceFlag_ActiveAccount The resource
     * account is active. When the account is active the resource can log on to
     * the server, when inactive the resource can not log in.
     * EHPMResourceFlag_AdminAccess The resource has administrative access and
     * is able to manage resources and projects in the database.
     * EHPMResourceFlag_ResourceAllocationAccess The resource has access to
     * resource allocation pane in client.
     * EHPMResourceFlag_DocumentManagementAccess The resource has access to
     * document management pane in client.
     * EHPMResourceFlag_AuthenticationProvider For SDK users only: the resource
     * is allowed to provide custom authentication services. The SDK is not
     * allowed to set this flag. EHPMResourceFlag_CredentialCheckProvider For
     * SDK users only: the resource is allowed to provide credential check
     * services. The SDK is not allowed to set this flag.
     * EHPMResourceFlag_AvatarManagementAccess The resource is able to manage
     * resource avatars EHPMResourceFlag_SdkChatUser For SDK users only: the SDK
     * user will be available for chat. EHPMResourceFlag_ChatAccess The resource
     * has access to chat.
     */
    public boolean isAdmin() throws HPMSdkException, HPMSdkJavaException {
        HPMResourceProperties props = session.ResourceGetProperties(userId);
        return props.m_Flags.contains(EHPMResourceFlag.AdminAccess);
    }

    /**
     * @param projectId 
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean hasLimitedVisibility(HPMUniqueID projectId)
            throws HPMSdkException, HPMSdkJavaException {
        if (props == null) {
            HPMUniqueID mainProjectId = session.UtilGetRealProjectIDFromProjectID(projectId);
            props = getHansoftSession()
                    .ProjectResourceGetProperties(mainProjectId, userId);

        }
        
        return props.m_Flags.contains(EHPMProjectResourceFlag.LimitedVisibility);
    }
    
    /**
     * @param projectId
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isMainManager(HPMUniqueID projectId)
           throws HPMSdkException, HPMSdkJavaException {
    	if (props == null) {
            HPMUniqueID mainProjectId = session.UtilGetRealProjectIDFromProjectID(projectId);
            props = getHansoftSession()
                    .ProjectResourceGetProperties(mainProjectId, userId);

        }
    	
        return props.m_Flags.contains(EHPMProjectResourceFlag.IsMainProjectManager);
    }

    /**
     * @return userId
     */
    public HPMUniqueID getUsedId() {
        return userId;
    }

    /**
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean setImpersonate() throws HPMSdkException, HPMSdkJavaException {
        getHansoftSession().ResourceImpersonate(
                userId,
                EHPMDataHistoryClientOrigin.CustomSDK,
                getHansoftSession()
                        .LocalizationCreateUntranslatedStringFromString(
                                "Updated from OSLC client"));
        return true;
    }

    /**
     * Return the Hansoft SDK virtual session. This might be invalid, to check
     * call isConnected.
     * 
     * @return HPMSdkSession
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public HPMSdkSession getHansoftSession() throws HPMSdkException,
            HPMSdkJavaException {
        return session;
    }


    /**
     * Check if user is member of the project - if not, skip as 
     * it should not be visible
     * 
     * @param projectId
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isMemberOfProject(HPMUniqueID projectId)
            throws HPMSdkException, HPMSdkJavaException {
        HPMUniqueID mainProjectId = session.UtilGetRealProjectIDFromProjectID(projectId);        
        if (!session.UtilIsIDProject(mainProjectId)) {
            logger.warn("Project id: " + mainProjectId + " is not a valid project id.");
            return false;
        }
        return session.ProjectResourceUtilIsMember(mainProjectId, userId);
    }

    /**
     * Check if type of task is Planned
     * 
     * @param taskId
     * @return true if type of task is Planned, otherwise false
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isChangeRequestTask(HPMUniqueID taskId)
            throws HPMSdkException, HPMSdkJavaException {
        EHPMTaskType type = session.TaskGetType(taskId);
        return (type == EHPMTaskType.Planned);
    }

    /**
     * Check if a Task is visible to a specific user
     * 
     * @param projectId
     * @param taskId
     * @return true if task is visible to a specific user, otherwise false
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isTaskVisible(HPMUniqueID projectId, HPMUniqueID taskId)
            throws HPMSdkException, HPMSdkJavaException {

        if (!hasLimitedVisibility(projectId))
            return true;

        HPMResourceDefinitionList resources = getHansoftSession()
                .TaskGetVisibleTo(taskId);
        boolean isVisible = isResourceAffected(resources.m_Resources);
        return isVisible;
    }

    /**
     * Check if a Task is delegated to a specific user
     * 
     * @param taskId
     * @return true if Task is delegated to a specific user, otherwise false
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isTaskDelegated(HPMUniqueID taskId)
            throws HPMSdkException, HPMSdkJavaException {

        HPMResourceDefinitionList resources = getHansoftSession()
                .TaskGetDelegateTo(taskId);
        return isResourceAffected(resources.m_Resources);
    }

    /**
     * Check if the custom column is accessible to the user in this project.
     * Assume user has access to project.
     * 
     * @param projectId
     * @param columnHash
     * @return true if the custom column is accessible to the user in this project, otherwise false
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isCustomColumnAccessibleTo(HPMUniqueID projectId,
            int columnHash) throws HPMSdkException, HPMSdkJavaException {
        if (!userCustomColumnAccess.containsKey(projectId)) {
            initColumnAccess(projectId);
        }

        return !userCustomColumnAccess.get(projectId).contains(columnHash);
    }

    /**
     * 
     * Check if the default column is accessible to the user in this project.
     * Assume user has access to project.
     * 
     * @param projectId
     * @param column
     * @return true if the default column is accessible to the user in this project, otherwise false
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isDefaultColumnAccessibleTo(HPMUniqueID projectId,
            EHPMProjectDefaultColumn column) throws HPMSdkException,
            HPMSdkJavaException {
        if (!userDefaultColumnAccess.containsKey(projectId)) {
            initColumnAccess(projectId);
        }

        return !userDefaultColumnAccess.get(projectId).contains(column);
    }

    /**
     * Check if the field is accessible to the user in this project. Assume user
     * has access to project.
     * 
     * @param projectId
     * @param taskField
     * @return
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public boolean isTaskFieldAccessibleTo(HPMUniqueID projectId,
            EHPMTaskField taskField) throws HPMSdkException,
            HPMSdkJavaException {
        if (!userTaskFieldAccess.containsKey(projectId)) {
            initColumnAccess(projectId);
        }

        return !userTaskFieldAccess.get(projectId).contains(taskField);
    }

    /**
     * As I understand documentation ProjectGetDetailedAccessRules give
     * information on what attributes (Columns and Fields) that are available
     * for a specific user - and to some extent what operations are available
     * for a user (resource). This applies to all Tasks. This method will
     * initiate three sets with id's for all columns and fields that are
     * available / restricted (TBD: What is default) to a specific user
     * 
     * @param projectId
     * @return
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    private void initColumnAccess(HPMUniqueID projectId)
            throws HPMSdkException, HPMSdkJavaException {

        // Create entries for this project
        Set<Integer> customColumnSet = new HashSet<Integer>();
        userCustomColumnAccess.put(projectId, customColumnSet);
        Set<EHPMProjectDefaultColumn> defaultColumnSet = new HashSet<EHPMProjectDefaultColumn>();
        userDefaultColumnAccess.put(projectId, defaultColumnSet);
        Set<EHPMTaskField> taskFieldSet = new HashSet<EHPMTaskField>();
        userTaskFieldAccess.put(projectId, taskFieldSet);

        HPMProjectDetailedAccessRules rules = getHansoftSession()
                .ProjectGetDetailedAccessRules(projectId);

        for (HPMProjectDetailedAccessRule rule : rules.m_Rules) {

            // From Hansoft SDK documentation: types of rule:
            //
            // EHPMProjectDetailedAccessRuleType_NewVersionOfSDKRequired = 0,
            // EHPMProjectDetailedAccessRuleType_GiveAccess = 1,
            // EHPMProjectDetailedAccessRuleType_RestrictAccess = 2,
            // EHPMProjectDetailedAccessRuleType_Hide = 3,

            // Note: Default is to have access, so keeping entries only to
            // signal no access (hidden or restricted).
            // Hence assume EHPMProjectDetailedAccessRuleType_GiveAccess can be
            // ignored here.
            if (rule.m_RuleType
                    .equals(EHPMProjectDetailedAccessRuleType.GiveAccess))
                continue;

            // Check if user is affected by rule - if not, check next
            if (!isResourceAffected(rule.m_Resources))
                continue;

            for (HPMProjectDetailedAccessRuleFunction ruleFunc : rule.m_Functions) {

                if (ruleFunc.m_FunctionType == EHPMProjectDetailedAccessRuleFunctionType.DefalutColumn) {
                    // The m_FunctionID is set to one of the
                    // EHPMProjectDefaultColumn enums
                    EHPMProjectDefaultColumn column = EHPMProjectDefaultColumn
                            .get(ruleFunc.m_FunctionID);
                    defaultColumnSet.add(column);
                } else if (ruleFunc.m_FunctionType == EHPMProjectDetailedAccessRuleFunctionType.CustomColumn) {
                    // The m_FunctionID is set to the hash of the CustomColumn
                    customColumnSet.add(ruleFunc.m_FunctionID);
                } else if (ruleFunc.m_FunctionType == EHPMProjectDetailedAccessRuleFunctionType.TaskField) {
                    // m_FunctionID should be set to one of EHPMTaskField enum.
                    // Currently only EHPMTaskField_Description,
                    // EHPMTaskField_DetailedDescription and
                    // EHPMTaskField_StepsToReproduce is used.
                    // These are fields that are associated to a Task, but not
                    // as a Column.
                    EHPMTaskField taskField = EHPMTaskField.get(ruleFunc.m_FunctionID);
                    taskFieldSet.add(taskField);
                }
            }
        }
    }

    /**
     * releases cached info
     * @param projectId
     */
    private void releaseCachedInfo(HPMUniqueID projectId) {
        userCustomColumnAccess.remove(projectId);
        userDefaultColumnAccess.remove(projectId);
        userTaskFieldAccess.remove(projectId);
        
        props = null;
    }

    /**
     * @param resourceDefinitions
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    private boolean isResourceAffected(
            List<HPMResourceDefinition> resourceDefinitions)
            throws HPMSdkException, HPMSdkJavaException {
        for (HPMResourceDefinition resourceDef : resourceDefinitions) {
            if (resourceDef.m_GroupingType == EHPMResourceGroupingType.AllProjectMembers) {
                return true;
            } else if (resourceDef.m_GroupingType == EHPMResourceGroupingType.Resource) {
                if (resourceDef.m_ID.equals(userId))
                    return true;
            } else if (resourceDef.m_GroupingType == EHPMResourceGroupingType.ResourceGroup) {
                HPMResourceEnum resources = getHansoftSession()
                        .ResourceGroupGetResources(resourceDef.m_ID);
                if (resources.m_Resources.contains(userId))
                    return true;
            }
        }
        return false;
    }

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
