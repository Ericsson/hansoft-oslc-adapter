package com.ericsson.eif.hansoft.factories;

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

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.XMLLiteral;

import se.hansoft.hpmsdk.EHPMError;
import se.hansoft.hpmsdk.EHPMListIcon;
import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnAccessRights;
import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnType;
import se.hansoft.hpmsdk.EHPMTaskAgilePriorityCategory;
import se.hansoft.hpmsdk.EHPMTaskLockedType;
import se.hansoft.hpmsdk.EHPMTaskSetStatusFlag;
import se.hansoft.hpmsdk.EHPMTaskSeverity;
import se.hansoft.hpmsdk.EHPMTaskStatus;
import se.hansoft.hpmsdk.EHPMTaskType;
import se.hansoft.hpmsdk.HPMChangeCallbackData_TaskCreateUnified;
import se.hansoft.hpmsdk.HPMProjectCustomColumns;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumnDropListItem;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMSessionLock;
import se.hansoft.hpmsdk.HPMTaskCreateUnified;
import se.hansoft.hpmsdk.HPMTaskCreateUnifiedEntry;
import se.hansoft.hpmsdk.HPMTaskCreateUnifiedReference;
import se.hansoft.hpmsdk.HPMTaskEnum;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.HansoftOSLCMapper;
import com.ericsson.eif.hansoft.exception.NoAccessException;
import com.ericsson.eif.hansoft.integration.HansoftIntegration;
import com.ericsson.eif.hansoft.integration.logging.ChangeLogger;
import com.ericsson.eif.hansoft.mapping.AttributesMapper;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.utils.HansoftUtils;
import com.ericsson.eif.hansoft.utils.StringUtils;

public class HansoftTaskFactory {

	private static final Logger logger = Logger
			.getLogger(HansoftTaskFactory.class.getName());

	/**
	 * Create a new Task from a HansoftChangeRequest
	 * 
	 * @param httpServletRequest
	 * @param changeRequest
	 * @param productIdString
	 * @param incomingChangeLogger
	 * @return id of the new Task or null
	 * @throws HPMSdkJavaException
	 * @throws HPMSdkException
	 * @throws NoAccessException
	 */
	public static HPMUniqueID createTask(HttpServletRequest httpServletRequest,
			final HansoftChangeRequest changeRequest,
			final String projectIdString, ChangeLogger incomingChangeLogger) throws HPMSdkException,
			HPMSdkJavaException, NoAccessException {

		HPMUniqueID ourTaskID = null;
		HPMUniqueID projectId = HansoftUtils.getProjectId(projectIdString);		
		if (projectId == null) {
			logger.debug("projectId is null");
			return null;
		}	
		
		HansoftConnector hc = HansoftConnector.getAuthorized(httpServletRequest);		
		if (!hc.isMemberOfProject(projectId)) {
			logger.error("User is not member in project.");
			throw new NoAccessException();
		}

		HPMSdkSession session = hc.getHansoftSession();

		// According to requirement the item should be created in the backlog
		HPMUniqueID backlogId = session.ProjectOpenBacklogProjectBlock(projectId);

		// If user does not have Main Project Manager rights, the user must have
		// a sub-project i.e. a item that is delegated to the user. And if the
		// parentTask is set, user need to have rights for this task.

		HPMUniqueID parentTaskRef = getParentTaskRef(changeRequest, hc, projectId);
		if (!hc.isMainManager(projectId)) {
			if (parentTaskRef != null) {
				HPMUniqueID parentTask = session.TaskRefGetTask(parentTaskRef);
				// If we have a parent task set - check if we can write
				if (!hc.isTaskDelegated(parentTask)) {
					logger.warn("User is not allowed to write");
					throw new NoAccessException();
				}
			} else {
				// Find a delegated parent task where we can write
				HPMTaskEnum tasks = session.TaskEnum(backlogId);
				for (HPMUniqueID task : tasks.m_Tasks) {
					if (hc.isTaskDelegated(task)) {
						parentTaskRef = session.TaskGetMainReference(task);
						break;
					}
				}
				if (parentTaskRef == null) {
					logger.warn("User is not allowed to write");
					throw new NoAccessException();
				}
			}
		}

		hc.setImpersonate();

		HPMTaskCreateUnified createData = new HPMTaskCreateUnified();
		HPMTaskCreateUnifiedEntry entry = new HPMTaskCreateUnifiedEntry();

		// Set previous to -1 to make it the top task.
		HPMTaskCreateUnifiedReference prevRefID = new HPMTaskCreateUnifiedReference();
		prevRefID.m_RefID = new HPMUniqueID();
		entry.m_PreviousRefID = prevRefID;
		
		// This is to handle task color coding so it used existing backlog setting
		entry.m_TaskType = EHPMTaskType.Planned;
		entry.m_TaskLockedType = EHPMTaskLockedType.BacklogItem;
	
		// From SDK documentation for entry.m_ParentRefIDs
		//
		// Here you should specify the whole list of parent references for the
		// place where you want to create this task in the schedule disposition.
		// The first entry should be the task closest to the root of the
		// schedule, the last entry the immediate parent of the task you are
		// creating. You must at least specify the closest parent. The whole
		// parent chain is only used if a parent is deleted before the create
		// message reaches the server. If the chain is sent and a parent is
		// deleted, the new task will end up as close as possible to the
		// intended location, otherwise it will end up at the root level.

		if (parentTaskRef != null) {
			HPMTaskCreateUnifiedReference parentRefId = new HPMTaskCreateUnifiedReference();
			parentRefId.m_RefID = parentTaskRef;
			ArrayList<HPMTaskCreateUnifiedReference> parentRefs = new ArrayList<HPMTaskCreateUnifiedReference>();
			parentRefs.add(parentRefId);
			entry.m_ParentRefIDs = parentRefs;
		}

		HPMTaskCreateUnifiedReference prevWorkPrioRefID = new HPMTaskCreateUnifiedReference();
		prevWorkPrioRefID.m_RefID = new HPMUniqueID(-2);
		entry.m_PreviousWorkPrioRefID = prevWorkPrioRefID;

		entry.m_LocalID = new HPMUniqueID(1);

		createData.m_Tasks.add(entry);

		HPMChangeCallbackData_TaskCreateUnified taskCreateReturn = session
				.TaskCreateUnifiedBlock(backlogId, createData);

		if (taskCreateReturn.m_Tasks.size() != 1) {
			throw new HPMSdkException(EHPMError.OtherError,
					"Failed to create Task.");
		}

		// The returned is a task ref in the project container. We need the task
		// id not the reference id.
		HPMUniqueID ourTaskRefID = taskCreateReturn.m_Tasks.get(0).m_TaskRefID;
		ourTaskID = session.TaskRefGetTask(ourTaskRefID);

		updateTaskBasic(httpServletRequest, changeRequest, ourTaskID, backlogId, hc, incomingChangeLogger);
		updateTaskExtension(changeRequest, ourTaskID, hc, httpServletRequest, projectId);

		// When we set fully created the task becomes visible to users.
		session.TaskSetFullyCreated(ourTaskID);

		logger.info("Created task ID=" + ourTaskID + " in project ID=" + projectIdString);
		return ourTaskID;
	}

	/**
	 * updates task extension 
	 * add backLink to mother in case of H2H integration
	 * @param changeRequest
	 * @param ourTaskID
	 * @param backlogId
	 * @param hc
	 * @param httpServletRequest
	 * @param projectId
	 */
	private static void updateTaskExtension(HansoftChangeRequest changeRequest,
			HPMUniqueID ourTaskID, HansoftConnector hc,
			HttpServletRequest httpServletRequest, HPMUniqueID projectId) {

		// add backLink to mother in case of H2H integration
		String integrationType = httpServletRequest.getHeader(Constants.INTEGRATION_TYPE);
						
		if (Constants.H2H.equalsIgnoreCase(integrationType)) {
			
			String syncFromLabel = httpServletRequest.getHeader(Constants.SYNC_FROM_LABEL);			
			String syncFromProjectId = httpServletRequest.getHeader(Constants.SYNC_FROM_PROJECT_ID);
			try {
				String projectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), projectId);
				URI about = changeRequest.getAbout();
				if (about != null) {
					String location = about.toString();
					HPMProjectCustomColumnsColumn backLinkColumn = HansoftIntegration.getBackLinkHansoftColumn(projectId, projectName);					
					HansoftIntegration.addBackLink(location, projectId, ourTaskID, backLinkColumn, syncFromLabel, syncFromProjectId, hc); 
				}
			} catch (Throwable t) {
				logger.error("H2H integration failed to add mother's backlink.", t);
			}
		}		
	}

	/**
	 * @param changeRequest
	 * @param hc
	 * @param projectId 
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	private static HPMUniqueID getParentTaskRef(
			HansoftChangeRequest changeRequest, HansoftConnector hc,
			HPMUniqueID projectId) throws HPMSdkException, HPMSdkJavaException {
		
		HPMSdkSession session = hc.getHansoftSession();
		String parentTaskIdStr = changeRequest.getParentTask();
		
		if (StringUtils.isNotEmpty(parentTaskIdStr)) {
			HPMUniqueID parentTaskId = HansoftUtils.getTaskId(parentTaskIdStr);			
			if (parentTaskId == null) {
				logger.warn("No parent task found with id: " + parentTaskId);				
				return null;
			}
			
			try {
				return session.TaskGetMainReference(parentTaskId);
			} catch (HPMSdkException e) {
				logger.error("Error when calling session.TaskGetMainReference for parentTaskId: " + parentTaskId, e);
			}
		}
		
		return null;
	}

	/**
	 * Task is updated in the context it exist, i.e. if moved to Main Project or
	 * QA needed custom columns will be updated there.
	 * 
	 * @param httpServletRequest
	 * @param changeRequest
	 * @throws IOException
	 * @throws ServletException
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws NoAccessException
	 */
	public static void updateTask(HttpServletRequest httpServletRequest,
			final HansoftChangeRequest changeRequest, ChangeLogger incomingChangeLogger) throws IOException,
			ServletException, HPMSdkException, HPMSdkJavaException,
			NoAccessException {

		HansoftConnector hc = HansoftConnector
				.getAuthorized(httpServletRequest);
		HPMSdkSession session = hc.getHansoftSession();

		String taskIdString = changeRequest.getIdentifier();
		HPMUniqueID taskId = HansoftUtils.getTaskId(taskIdString);
		if (taskId == null) {
			logger.debug("taskId is null");
			return;
		}
		
		// Check preconditions
		if (!hc.isChangeRequestTask(taskId)) {
			return;
		}

		HPMUniqueID projectId = session.TaskGetContainer(taskId);
		HPMUniqueID realProjectId = session
				.UtilGetRealProjectIDFromProjectID(projectId);
		if (!hc.isMemberOfProject(realProjectId)) {
			logger.error("User is not member in project.");
			throw new NoAccessException();
		}

		if (!hc.isMainManager(projectId)) {
			HPMUniqueID taskRef = session.TaskGetMainReference(taskId);
			HPMUniqueID taskParentRef = session.TaskRefUtilGetParent(taskRef);
			HPMUniqueID taskParent = session.TaskRefGetTask(taskParentRef);

			// If we have a parent task set - check if we can write
			if (!hc.isTaskDelegated(taskParent)) {
				logger.warn("User is not allowed to write.");
				throw new NoAccessException();
			}
		}

		hc.setImpersonate();

		// Acquire lock to make update atomic operation
		HPMSessionLock lock = session.SessionLock();
		try {
			updateTaskBasic(httpServletRequest, changeRequest, taskId, projectId, hc, incomingChangeLogger);
			updateTaskExtension(changeRequest, taskId, hc, httpServletRequest, realProjectId);
		} finally {
			lock.dispose();
		}
	}

	/**
	 * Create a new Task from a HansoftChangeRequest.
	 * 
	 * Precondition: Resource Impersonate needs to be called before access.
	 * 
	 * @param changeRequest
	 * @param taskId
	 * @param projectId
	 *            - can be QA, Backlog or Main project
	 * @param hc
	 * @param incomingChangeLogger
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws NoAccessException
	 */
	private static void updateTaskBasic(
			final HttpServletRequest httpServletRequest,
			final HansoftChangeRequest changeRequest, final HPMUniqueID taskId,
			final HPMUniqueID projectId, final HansoftConnector hc, ChangeLogger incomingChangeLogger)
			throws HPMSdkException, HPMSdkJavaException, NoAccessException {
		
		String integrationType = httpServletRequest.getHeader(Constants.INTEGRATION_TYPE);
		HPMSdkSession session = hc.getHansoftSession();
		

		String title = changeRequest.getTitle();
		if (title != null && !title.startsWith(Constants.FP_FIX_PREFIX)) {				
			String taskTitle = session.TaskGetDescription(taskId);
			title = StringEscapeUtils.unescapeHtml(title);
			
			if (!title.equals(taskTitle)) {
				session.TaskSetDescription(taskId, title);
									
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Item name", taskTitle, title);
				}
			}
		}

		// Set the OSLC back reference or backlink.
		// This is key to enable the dialogs
		String hyperlink = changeRequest.getHyperlink();
		if (hyperlink != null) {
			HPMProjectCustomColumns customColumns = session.ProjectCustomColumnsGet(projectId);
			HPMProjectCustomColumnsColumn extRefCol = null;
			for (HPMProjectCustomColumnsColumn column : customColumns.m_ShowingColumns) {
				if (column.m_Name.equals(HansoftManager.OSLC_BACKLINK_COL_NAME)) {
					extRefCol = column;
					break;
				}
			}
			//If that particular column does not exist in project, we create with proper access rights
			if (extRefCol == null) {
				extRefCol = new HPMProjectCustomColumnsColumn();
				extRefCol.m_Name = HansoftManager.OSLC_BACKLINK_COL_NAME;
				extRefCol.m_Type = EHPMProjectCustomColumnsColumnType.Hyperlink;
				extRefCol.m_AccessRights = EHPMProjectCustomColumnsColumnAccessRights.AllProjectMembers;
				session.ProjectCustomColumnsCreate(projectId, extRefCol);

				// From documentation:
				// The column hash is used to identify a custom column. When
				// new columns are created you need to make sure that the
				// hash value doesn't collide with any other column hashes
				// in all projects of the database, otherwise unexpected
				// things will happen when data is copy/pasted between
				// projects.

				// Check with Hansoft how to best do this. Given hash
				// is created based on created column - do you
				// have to do trial and error here until an unique hash is
				// returned?

				// Hash needs to be calculated explicitly
				extRefCol.m_Hash = session.UtilGetColumnHash(extRefCol);
			}

			String taskHyperlink = session.TaskGetCustomColumnData(taskId, extRefCol.m_Hash);
			if (!hyperlink.equals(taskHyperlink)) {
				session.TaskSetCustomColumnData(taskId, extRefCol.m_Hash, hyperlink, false);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log(extRefCol.m_Name, taskHyperlink, hyperlink);
				}
			}
		}

		String detailed = changeRequest.getDescription();
		if (StringUtils.isNotEmpty(detailed) && !detailed.startsWith(Constants.FP_FIX_PREFIX)) {
			String taskDetailed = session.TaskGetDetailedDescription(taskId);
			if (!detailed.equals(taskDetailed)) {
				session.TaskSetDetailedDescription(taskId, detailed);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Description", taskDetailed, detailed);
				}
			}
		}
	
		String prio = changeRequest.getPriority();
		if (prio != null) {
			EHPMTaskAgilePriorityCategory prioAsTaskPrio = HansoftOSLCMapper.getHsPriority(prio);
			EHPMTaskAgilePriorityCategory taskPrio = session.TaskGetAgilePriorityCategory(taskId);
			if (prioAsTaskPrio.compareTo(taskPrio) != 0) {
				session.TaskSetAgilePriorityCategory(taskId, prioAsTaskPrio);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Priority", taskPrio.toString(), prioAsTaskPrio.toString());
				}
			}
		}

		String severity = changeRequest.getSeverity();
		if (severity != null && !severity.startsWith(Constants.FP_FIX_PREFIX)) {
			EHPMTaskSeverity severityAsTaskSeverity = HansoftOSLCMapper.getHsSeverity(severity);
			EHPMTaskSeverity taskSeverity = session.TaskGetSeverity(taskId);
			if (severityAsTaskSeverity.compareTo(taskSeverity) != 0) {
				session.TaskSetSeverity(taskId, severityAsTaskSeverity);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Severity", taskSeverity.toString(), severityAsTaskSeverity.toString());
				}
			}
		}

		String status = changeRequest.getStatus();
		if (status != null && !status.startsWith(Constants.FP_FIX_PREFIX)) {
			EHPMTaskStatus statusAsTaskStatus = HansoftOSLCMapper.getHsStatus(status);
			EHPMTaskStatus taskStatus = session.TaskGetStatus(taskId);
			if (statusAsTaskStatus.compareTo(taskStatus) != 0) {
				EnumSet<EHPMTaskSetStatusFlag> enumSet = EHPMTaskSetStatusFlag.toEnumSet(0);
				session.TaskSetStatus(taskId, statusAsTaskStatus, false, enumSet);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Status", taskStatus.toString(), statusAsTaskStatus.toString());
				}
			}
		}

		Double workRemaining = changeRequest.getWorkRemaining();
		if (workRemaining != null) {
			float workRemainingAsFloat = workRemaining.floatValue();
			float taskWorkRemaining = session.TaskGetWorkRemaining(taskId);
			if (workRemainingAsFloat != taskWorkRemaining) {
				session.TaskSetWorkRemaining(taskId, workRemainingAsFloat);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log("Work Remaining", Float.toString(taskWorkRemaining), Float.toString(workRemainingAsFloat));
				}
			}
		}

		// Map Hansoft extended properties
		mapExtendedProperties(projectId, session, changeRequest, taskId, integrationType, incomingChangeLogger);

	}

	/**
	 * @param projectId
	 * @param session
	 * @param changeRequest
	 * @param taskId
	 * @param integrationType
	 * @param incomingChangeLogger
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	private static void mapExtendedProperties(final HPMUniqueID projectId,
			final HPMSdkSession session,
			final HansoftChangeRequest changeRequest, final HPMUniqueID taskId, String integrationType, ChangeLogger incomingChangeLogger)
			throws HPMSdkException, HPMSdkJavaException {

		Map<QName, Object> extProps = changeRequest.getExtendedProperties();
		if (extProps == null || extProps.isEmpty()) {
			return;
		}

		HPMProjectCustomColumns customColumns = session.ProjectCustomColumnsGet(projectId);
		Set<QName> keys = extProps.keySet();
		for (QName qName : keys) {
			boolean mapped = false;
			String dataString = null;
			Object dataObj = extProps.get(qName);
			if (dataObj == null) {
				continue;
			}

			//In order to preserve all special characters in RDF, i use attribute rdf:parseType = "Literal" which maps XMLLiteral
			if (dataObj instanceof XMLLiteral ) {
				  dataString = ((XMLLiteral) dataObj).getValue();
			} else if (!(dataObj instanceof String)) {
				logger.warn("Indata not of type String: " + dataObj);
				continue;
			} else {
				dataString = (String) dataObj;
			}
			
			//Data decoding is required especially for unknown content to prevent them
            //from being altered by consumer application especially during PUT transaction
			//Do not apply during as those data do not exist
			String data = dataString;
			String colName = AttributesMapper.getInstance().getColumnNameFromPropertyName(qName.getLocalPart());
			boolean columnsChanged = false;
			for (HPMProjectCustomColumnsColumn column : customColumns.m_ShowingColumns) {
				if (column.m_Name.equals(colName)) {
					columnsChanged = updateCustomColumnData(projectId, session,	column, false, taskId, data, integrationType, incomingChangeLogger);
					mapped = true;
					break;
				}
			}
			
			if (!mapped) {
				for (HPMProjectCustomColumnsColumn column : customColumns.m_HiddenColumns) {
					if (column.m_Name.equals(colName)) {
						columnsChanged = updateCustomColumnData(projectId, session, column, true, taskId, data, integrationType, incomingChangeLogger);
						break;
					}
				}
			}
			
			if (columnsChanged) {
				customColumns = session.ProjectCustomColumnsGet(projectId);
			}
		}
	}

	/**
	 * Update the data in a custom column. If needing to change any of the
	 * custom columns e.g. add a item in a drop down list, the columns will be
	 * changed. This is signaled by the return value.
	 * 
	 * @param projectId
	 * @param session
	 * @param column
	 * @param hidden
	 * @param taskId
	 * @param data
	 * @param integrationType
	 * @param incomingChangeLogger
	 * @return - If the column is changed
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static boolean updateCustomColumnData(HPMUniqueID projectId,
			HPMSdkSession session, HPMProjectCustomColumnsColumn column,
			boolean hidden, HPMUniqueID taskId, String data, String integrationType, ChangeLogger incomingChangeLogger)
			throws HPMSdkException, HPMSdkJavaException {
		
		if (data != null && data.startsWith(Constants.FP_FIX_PREFIX))	{
			return false;
		}
				
		String taskData = session.TaskGetCustomColumnData(taskId, column.m_Hash);
		if (column.m_Type == EHPMProjectCustomColumnsColumnType.DateTime) {

			//Sometimes the field date when empty equals "-" so checking length
			if (StringUtils.isEmpty(data)|| data.length() < 4 ) {
				// Empty date field = clear our any existing date
				if (!taskData.isEmpty()) {					
					session.TaskSetCustomColumnData(taskId, column.m_Hash, "", false);
				}
				return false;
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date dataAsDate = null;
			try {
				dataAsDate = dateFormat.parse(data);
			} catch (ParseException e) {
				logger.warn("Failed to parse indata: " + data + " as a date.");
				return false;
			}

			// Review the date conversion. Passing in a date in format
			// yyyy-MM-dd, convert to microsec and setting will make the date
			// be -1 day compared to given date. Hence adding 12h.
			// But microsec value for HS date value is not same. Need to
			// figure out how a date (yyyy-MM-dd) is converted to microsec
			// by HS code.

			long midDayOffset = 12L * 60L * 60L * 1000L * 1000L;
			long dataAsMicrosecs = dataAsDate.getTime() * 1000L + midDayOffset;
			long taskDataDecoded = session.UtilDecodeCustomColumnDateTimeValue(taskData);

			if (dataAsMicrosecs != taskDataDecoded) {
				String utilEncodeCustomColumnDateTimeValue = session.UtilEncodeCustomColumnDateTimeValue(dataAsMicrosecs);
				session.TaskSetCustomColumnData(taskId, column.m_Hash, utilEncodeCustomColumnDateTimeValue, false);
				
				if (Constants.H2H.equalsIgnoreCase(integrationType)) {
					incomingChangeLogger.log(column.m_Name, session.UtilEncodeCustomColumnDateTimeValue(taskDataDecoded), session.UtilEncodeCustomColumnDateTimeValue(dataAsMicrosecs));
				}
			}
		} else if (column.m_Type == EHPMProjectCustomColumnsColumnType.DropList) {
			ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = column.m_DropListItems;
			for (HPMProjectCustomColumnsColumnDropListItem item : items) {
				if (item.m_Name.equals(data)) {
					String dataId = String.valueOf(item.m_Id);
					if (!taskData.equals(dataId)) {
						session.TaskSetCustomColumnData(taskId, column.m_Hash, dataId, false);
						
						if (Constants.H2H.equalsIgnoreCase(integrationType)) {
							incomingChangeLogger.log(column.m_Name, taskData, dataId);
  					    }
					}
					// List item found and updated - we can return
					return false;
				}
			}
			// List item not found - add the new drop down list item to the
			// column's drop down list. This will change the hash of the column.			
			updateCustomColumnDropDownList(projectId, session, column.m_Hash, hidden, taskId, data);
			return true;

		} 
		else if (column.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
		
			// This list represents the actual list of items in the this column
			ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = column.m_DropListItems;
						
			// The raw data is the id's (NOT index) of items in droplist separated with ";"
    		String[] dataValues = data.split(";");
    		String dataIds = "";
    		for (int i = 0; i < dataValues.length; i++) {
        		for (HPMProjectCustomColumnsColumnDropListItem item : items) {
    				if (item.m_Name.equals(dataValues[i])) {
    					String dataId = String.valueOf(item.m_Id);
    					dataIds += dataIds.isEmpty()? dataId : ";" + dataId;
    					break;
    				}
    			}				
			}
    		
    		// Make sure not order makes compare not work - e.g. "2;3;5" != "3;2;5"
    		if (!taskData.equals(dataIds)) {
    		    session.TaskSetCustomColumnData(taskId, column.m_Hash, dataIds, false);
        		
        		if (Constants.H2H.equalsIgnoreCase(integrationType)) {
        			incomingChangeLogger.log(column.m_Name, taskData, dataIds);
			    }
    		}
			
    		return false;
		} else if (!data.equals(taskData)) {			
			session.TaskSetCustomColumnData(taskId, column.m_Hash, data, false);
			
			if (Constants.H2H.equalsIgnoreCase(integrationType)) {
				incomingChangeLogger.log(column.m_Name, taskData, data);
		    }
		}
		return false;
	}

	/**
	 * The method will update a drop down list of a column with the data
	 * element. This update will make the hash of the column to update, i.e. it
	 * will be a new column. So need to move the data from old to new column and
	 * also update the list of columns in the backlog project. And if the old
	 * column (check hash) is present in the main project, replace the old
	 * column also there.
	 * 
	 * @param projectId
	 * @param session
	 * @param oldColHash
	 * @param hidden
	 * @param taskId
	 * @param data
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	private static void updateCustomColumnDropDownList(HPMUniqueID projectId,
			HPMSdkSession session, int oldColHash, boolean hidden,
			HPMUniqueID taskId, String data) throws HPMSdkException,
			HPMSdkJavaException {

		HPMProjectCustomColumns columns = session.ProjectCustomColumnsGet(projectId);
		ArrayList<HPMProjectCustomColumnsColumn> columnsList;
		if (hidden) 
			columnsList = columns.m_HiddenColumns;
		else 
			columnsList = columns.m_ShowingColumns;
		
		ArrayList<HPMProjectCustomColumnsColumn> newColumnsList = new ArrayList<HPMProjectCustomColumnsColumn>();

		int newHash = 0;
		int newId = 0;
		HPMProjectCustomColumnsColumn newColumn = null;
		for (int i = 0; i < columnsList.size(); i++) {
			HPMProjectCustomColumnsColumn column = columnsList.get(i);
			if (column.m_Hash == oldColHash) {

				// Get a new id for the drop down item.
				ArrayList<HPMProjectCustomColumnsColumnDropListItem> listItems = column.m_DropListItems;
				for (HPMProjectCustomColumnsColumnDropListItem listItem : listItems) {
					if (listItem.m_Id > newId) {
						newId = listItem.m_Id;
					}
				}
				newId = newId + 1;

				HPMProjectCustomColumnsColumnDropListItem newItem = new HPMProjectCustomColumnsColumnDropListItem();
				newItem.m_Name = data;
				newItem.m_Icon = EHPMListIcon.WhiteBox; // Default in UI
				newItem.m_Id = newId;

				logger.info("added new item to drop list task ID=" + taskId + " in project ID=" + projectId + " column=" + column.m_Name + " data=" + newItem.m_Name);
				column.m_DropListItems.add(newItem);

				newHash = session.UtilGetColumnHash(column);
				newColumn = column;
			}
			newColumnsList.add(column);
		}

		if (hidden) {
			columns.m_HiddenColumns = newColumnsList;
		} else {
			columns.m_ShowingColumns = newColumnsList;
		}

		// Re-set the custom columns for the backlog project
		session.ProjectCustomColumnsSet(projectId, columns);

		// Move the data to the new column. Will also make the column position
		// in UI remain.
		session.ProjectCustomColumnsRenameTaskData(projectId, oldColHash, newHash);

		// Set value of the drop down data to the newly added element
		session.TaskSetCustomColumnData(taskId, newHash, String.valueOf(newId), false);

		// If the column is present also in the main project we need to column
		// entry with old hash with the column with the new hash

		HPMUniqueID mainProjectId = session.UtilGetRealProjectIDFromProjectID(projectId);
		HPMProjectCustomColumnsColumn mainCustomColumn = session
				.ProjectGetCustomColumn(mainProjectId, oldColHash);
		if (mainCustomColumn == null) {
			// The updated column was not present in the main project - return
			return;
		}

		// The updated column is present in the main project - it could be in
		// either the showing or the hidden columns.

		HPMProjectCustomColumns mainColumns = session.ProjectCustomColumnsGet(mainProjectId);

		boolean found = false;

		// First check the showing columns
		ArrayList<HPMProjectCustomColumnsColumn> newMainColumnsList = new ArrayList<HPMProjectCustomColumnsColumn>();
		ArrayList<HPMProjectCustomColumnsColumn> mainColumnsList = mainColumns.m_ShowingColumns;
		for (int i = 0; i < mainColumnsList.size(); i++) {
			HPMProjectCustomColumnsColumn column = mainColumnsList.get(i);
			if (column.m_Hash == oldColHash) {
				newMainColumnsList.add(newColumn);
				found = true;
			} else {
				newMainColumnsList.add(column);
			}
		}

		if (found) {
			mainColumns.m_ShowingColumns = newMainColumnsList;
		} else {
			// If not found among the showing - it is among the hidden columns
			newMainColumnsList = new ArrayList<HPMProjectCustomColumnsColumn>();
			mainColumnsList = mainColumns.m_HiddenColumns;
			for (int i = 0; i < mainColumnsList.size(); i++) {
				HPMProjectCustomColumnsColumn column = mainColumnsList.get(i);
				if (column.m_Hash == oldColHash) {
					newMainColumnsList.add(newColumn);
				} else {
					newMainColumnsList.add(column);
				}
			}
			mainColumns.m_HiddenColumns = newMainColumnsList;
		}

		// Re-set the custom columns for the main project
		session.ProjectCustomColumnsSet(mainProjectId, mainColumns);

		// Move the data to the new column. Will also make the column position
		// in UI remain.
		session.ProjectCustomColumnsRenameTaskData(mainProjectId, oldColHash, newHash);
	}

	/**
	 * @param buffer
	 * @param linkType
	 * @param links
	 */
	protected static void addLinkComment(final StringBuffer buffer,
			final String linkType, final Link[] links) {
		
		if (links != null && (links.length != 0)) {
			buffer.append(linkType);
			buffer.append(":\n\n");
			for (Link link : links) {
				buffer.append(link.getValue().toString());
				buffer.append("\n");
			}
		}
	}

	/**
	 * @param cr
	 * @return
	 */
	protected static String getLinksComment(final HansoftChangeRequest cr) {
		final StringBuffer b = new StringBuffer();

		addLinkComment(b, "Affected by Defect", cr.getAffectedByDefects());
		addLinkComment(b, "Affects Plan Item", cr.getAffectsPlanItems());
		addLinkComment(b, "Affects Requirement", cr.getAffectsRequirements());
		addLinkComment(b, "Affects Test Result", cr.getAffectsTestResults());
		addLinkComment(b, "Blocks Test Execution Record",
				cr.getBlocksTestExecutionRecords());
		addLinkComment(b, "Implements Requirement",
				cr.getImplementsRequirements());
		addLinkComment(b, "Related Change Request",
				cr.getRelatedChangeRequests());
		addLinkComment(b, "Related Test Execution Record",
				cr.getRelatedTestExecutionRecords());
		addLinkComment(b, "Related Test Plane", cr.getRelatedTestPlans());
		addLinkComment(b, "Related Test Script", cr.getRelatedTestScripts());
		addLinkComment(b, "Tested by Test Case", cr.getTestedByTestCases());
		addLinkComment(b, "Tracks Change Set", cr.getTracksChangeSets());
		addLinkComment(b, "Tracks Requirement", cr.getTracksRequirements());

		return b.toString();
	}

	static final Map<String, Object> toplevelQueryProperties = new HashMap<String, Object>();

	static {
		Map<String, Object> nestedQueryProperties = new HashMap<String, Object>(
				1);

		nestedQueryProperties.put(Constants.FOAF_NAMESPACE + "mbox",
				"assigned_to");

		toplevelQueryProperties.put(OslcConstants.DCTERMS_NAMESPACE
				+ "contributor", nestedQueryProperties);

		toplevelQueryProperties.put(
				OslcConstants.DCTERMS_NAMESPACE + "created", "creation_ts");
		toplevelQueryProperties.put(OslcConstants.DCTERMS_NAMESPACE
				+ "modified", "delta_ts");
		toplevelQueryProperties.put(Constants.HANSOFT_NAMESPACE + "version",
				"version");
		toplevelQueryProperties.put(Constants.HANSOFT_NAMESPACE + "priority",
				"priority");
	}
}
