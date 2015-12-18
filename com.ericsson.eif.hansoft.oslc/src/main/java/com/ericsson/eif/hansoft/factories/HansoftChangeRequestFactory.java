/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Russell Boykin       - initial API and implementation
 *     Alberto Giammaria    - initial API and implementation
 *     Chris Peters         - initial API and implementation
 *     Gianluca Bernardini  - initial API and implementation
 *     Michael Fiedler      - Bugzilla adapter implementation
 *******************************************************************************/
package com.ericsson.eif.hansoft.factories;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.eclipse.lyo.core.query.ComparisonTerm;
import org.eclipse.lyo.core.query.PName;
import org.eclipse.lyo.core.query.ParseException;
import org.eclipse.lyo.core.query.QueryUtils;
import org.eclipse.lyo.core.query.SimpleTerm;
import org.eclipse.lyo.core.query.Value;
import org.eclipse.lyo.core.query.WhereClause;

import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnType;
import se.hansoft.hpmsdk.EHPMTaskAgilePriorityCategory;
import se.hansoft.hpmsdk.EHPMTaskField;
import se.hansoft.hpmsdk.EHPMTaskStatus;
import se.hansoft.hpmsdk.EHPMTaskType;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumnDropListItem;
import se.hansoft.hpmsdk.HPMResourceDefinition;
import se.hansoft.hpmsdk.HPMResourceDefinitionList;
import se.hansoft.hpmsdk.HPMResourceProperties;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMSessionLock;
import se.hansoft.hpmsdk.HPMTaskCustomColumnDataEnum;
import se.hansoft.hpmsdk.HPMTaskEnum;
import se.hansoft.hpmsdk.HPMTaskResourceAllocation;
import se.hansoft.hpmsdk.HPMTaskResourceAllocationResource;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.HansoftOSLCMapper;
import com.ericsson.eif.hansoft.exception.NoAccessException;
import com.ericsson.eif.hansoft.mapping.AttributesMapper;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.resources.Person;
import com.ericsson.eif.hansoft.utils.HansoftUtils;

public class HansoftChangeRequestFactory {

	private static final Logger logger = Logger
			.getLogger(HansoftChangeRequestFactory.class.getName());

	/**
	 * Default constructor
	 */
	private HansoftChangeRequestFactory() {
		super();
	}
	
	/**
	 * @param crId
	 * @return about URI
	 */
	static public URI getAbout(String crId) {
		URI about;
		try {
			String basePath = HansoftManager.getServiceBase();
			about = new URI(basePath + "/"
					+ HansoftManager.CHANGE_REQUEST_PATH_SEGMENT + "/" + crId);
		} catch (URISyntaxException e) {
			throw new WebApplicationException(e);
		}
		return about;
	}

	/**
	 * Create a list of HansoftChangeRequests for a product ID using paging
	 * 
	 * @param httpServletRequest
	 * @param productIdString
	 * @param page
	 * @param limit
	 * @param oslcWhere
	 * @param prefixMap
	 * @param propMap
	 * @param orderBy
	 * @param searchTerms
	 * 
	 * @return The list of change requests, paged if necessary
	 * @throws HPMSdkJavaException
	 * @throws HPMSdkException
	 * @throws NoAccessException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	public static List<HansoftChangeRequest> getChangeRequestsByProduct(
			final HttpServletRequest httpServletRequest,
			final String projectIdString, int page, int limit, String where,
			Map<String, String> prefixMap, Map<String, Object> properties,
			String orderBy, String searchTerms) throws NoAccessException,
			HPMSdkException, HPMSdkJavaException, UnsupportedEncodingException,
			URISyntaxException {

		List<HansoftChangeRequest> results = new ArrayList<HansoftChangeRequest>();

		final HansoftConnector hc = HansoftConnector
				.getAuthorized(httpServletRequest);
		final HPMSdkSession session = hc.getHansoftSession();

		HPMSessionLock lock = session.SessionLock();

		try {
			HPMUniqueID projectId = HansoftUtils.getProjectId(projectIdString);		
			if (projectId == null) {
				logger.error("projectId is null");
				logger.debug("returning empty results");
				return results;
			}
			
			try {
				if (!hc.isMemberOfProject(projectId))
					throw new NoAccessException();
				
			} catch (HPMSdkException e) {
				throw new NoAccessException();
			}
			
			HPMTaskEnum tasks = session.TaskEnum(projectId);
			
			for (HPMUniqueID taskId : tasks.m_Tasks) {
				EHPMTaskType type = session.TaskGetType(taskId);

				// Can be Planned or Milestone (and old SDK ...), so filtering
				// out all but Planned
				if (type == EHPMTaskType.Planned) {
					HansoftChangeRequest changeRequest = getChangeRequestFromTask(
							httpServletRequest, projectIdString, projectId,
							taskId, true);
					
					if (changeRequest != null) {
						if (changeRequest.getModified() == null) {
							// See http://www.w3.org/TR/xmlschema-2/#dateTime
							long lastModified = session.TaskGetLastUpdatedTime(taskId);
							changeRequest.setModified(new Date(lastModified / 1000));
							
						}						
						changeRequest.setDescription(session.TaskGetDescription(taskId));
						results.add(changeRequest);
					}
				}

				// Currently paging is not implemented so will return
				// *all* CRs
				// for a project, which can be too much. For IPOS project we get
				// out
				// of memory error. So for now - hard limit of 100 items.
				if (results.size() >= 100) {
					return results;
				}
			}

			// If the incoming project already is the backlog, then return
			HPMUniqueID backlogId = session.ProjectUtilGetBacklog(projectId);
			if (backlogId.m_ID == -1) {
				return results;
			}

			HPMTaskEnum backlogTasks = session.TaskEnum(backlogId);

			for (HPMUniqueID taskId : backlogTasks.m_Tasks) {
				EHPMTaskType type = session.TaskGetType(taskId);

				// Can be Planned or Milestone (and old SDK ...), so filtering
				// out all but Planned
				if (type == EHPMTaskType.Planned) {
					HansoftChangeRequest changeRequest = getChangeRequestFromTask(
							httpServletRequest, backlogId.toString(),
							backlogId, taskId, false);

					try {
						if (changeRequest != null
								&& isOslcWhereSatisfied(where, prefixMap,
										changeRequest)) {
							if (changeRequest.getModified() == null) {
								// See http://www.w3.org/TR/xmlschema-2/#dateTime
								long lastModified = session.TaskGetLastUpdatedTime(taskId);
								changeRequest.setModified(new Date(lastModified / 1000));								
							}
							changeRequest.setDescription(session.TaskGetDescription(taskId));
							results.add(changeRequest);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// Currently paging is not implemented so will return
				// *all* CRs
				// for a project, which can be too much. For IPOS project we get
				// out
				// of memory error. So for now - hard limit of 100 items.
				if (results.size() >= 100) {
					return results;
				}
			}
		} finally {
			lock.dispose();
		}

		return results;
	}
	
	/**
	 * Currently only one value is filtered no additional filters is applied (return immediately)
	 * 
	 * @param where
	 * @param prefixMap
	 * @param changeRequest
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static boolean isOslcWhereSatisfied(String where,
			Map<String, String> prefixMap, HansoftChangeRequest changeRequest)
			throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		
		if(where == null) return true;
		WhereClause whereClause = null;
		PName property = null;
		if (where != null) {
			try {
				whereClause = QueryUtils.parseWhere(where, prefixMap);
			} catch (ParseException e) {
				logger.error("Failed to parse where statement: " + where, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}

			String value = "";
			for (SimpleTerm term : whereClause.children()) {
				ComparisonTerm comparison = (ComparisonTerm) term;
				String operator;

				switch (comparison.operator()) {
				case EQUALS:
					operator = "equals";
					break;
				case NOT_EQUALS:
					operator = "notequals";
					throw new NotImplementedException(
							"NOT_EQUALS oslc.where operator not implemented");
				case LESS_THAN:
					operator = "lessthan";
					throw new NotImplementedException(
							"LESS_THAN oslc.where operator not implemented");
				case LESS_EQUALS:
					operator = "lessthaneq";
					throw new NotImplementedException(
							"LESS_EQUALS oslc.where operator not implemented");
				case GREATER_THAN:
					operator = "greaterthan";
					throw new NotImplementedException(
							"GREATER_THAN oslc.where operator not implemented");
				default:
				case GREATER_EQUALS:
					operator = "greaterhaneq";
					throw new NotImplementedException(
							"GREATER_EQUALS oslc.where operator not implemented");
				}

				property = comparison.property();
				Value operand = comparison.operand();
				value = operand.toString();
				switch (operand.type()) {
				case STRING:
				case URI_REF:
					value = value.substring(1, value.length() - 1);
					break;
				case BOOLEAN:
				case DECIMAL:
					break;
				default:
					throw new WebApplicationException(
							new UnsupportedOperationException(
									"Unsupported oslc.where comparison operand: "
											+ value), Status.BAD_REQUEST);
				}
				
				QName qName = new QName(property.namespace, property.local,
						property.prefix);
				
				if (changeRequest.getExtendedProperties().containsKey(qName)) {
					if (changeRequest.getExtendedProperties().get(qName).toString().equalsIgnoreCase(value))
						return true;
				}
				
				String compareString = property.toString().substring(
						property.toString().indexOf(":") + 1);
				String val = "get"
						+ compareString.substring(0, 1).toUpperCase()
						+ compareString.substring(1);
				Method m = changeRequest.getClass().getMethod(val,
						(Class<?>[]) null);
				
				if (m != null && (m.invoke(changeRequest).toString()
						.equalsIgnoreCase(value))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get a change request based on a project id and task id
	 * 
	 * @param httpServletRequest
	 * @param projectIdString
	 * @param taskId
	 * @param minimal
	 *            - if true, return minimal CR representation
	 * @return
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws NoAccessException
	 */
	public static HansoftChangeRequest getChangeRequestFromTask(
			HttpServletRequest httpServletRequest, String projectIdString,
			HPMUniqueID taskId, boolean minimal) throws URISyntaxException,
			UnsupportedEncodingException, HPMSdkException, HPMSdkJavaException,
			NoAccessException {

		HPMUniqueID projectId = HansoftUtils.getProjectId(projectIdString);		
		if (projectId == null) {
			logger.debug("projectId is null");
			return null;
		}
		
		return getChangeRequestFromTask(httpServletRequest, projectIdString, 
				projectId, taskId, minimal);
	}

	/**
	 * Get a change request based on a project id and task id string
	 * 
	 * @param httpServletRequest
	 * @param projectIdString
	 * @param taskIdString
	 * @param minimal
	 *            - if true, return minimal CR representation
	 * @return
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws NoAccessException
	 */
	public static HansoftChangeRequest getChangeRequestFromTask(
			HttpServletRequest httpServletRequest, String projectIdString,
			String taskIdString, boolean minimal)
			throws UnsupportedEncodingException, URISyntaxException,
			HPMSdkException, HPMSdkJavaException, NoAccessException {

		HPMUniqueID taskId = HansoftUtils.getTaskId(taskIdString);
		if (taskId == null) {
			logger.debug("taskId is null");
			return null;
		}
		
		HPMUniqueID projectId = HansoftUtils.getProjectId(projectIdString);		
		if (projectId == null) {
			logger.debug("projectId is null");
			return null;
		}
		
		return getChangeRequestFromTask(httpServletRequest, projectIdString,
				projectId, taskId, minimal);
	}

	/**
	 * Get change request based on a task id
	 * 
	 * @param httpServletRequest
	 * @param taskIdString
	 * @param minimal
	 *            - if true, return minimal CR representation
	 * @return
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 * @throws NoAccessException
	 */
	public static HansoftChangeRequest getChangeRequestFromTask(
			HttpServletRequest httpServletRequest, String taskIdString,
			boolean minimal) throws URISyntaxException,
			UnsupportedEncodingException, HPMSdkException, HPMSdkJavaException,
			NoAccessException {

		final HansoftConnector hc = HansoftConnector
				.getAuthorized(httpServletRequest);
		final HPMSdkSession session = hc.getHansoftSession();
		
		HPMUniqueID taskId = HansoftUtils.getTaskId(taskIdString);
		if (taskId == null) {
			logger.debug("taskId is null");
			return null;
		}

		HPMSessionLock lock = session.SessionLock();
		try {
			// Check preconditions
			if (!hc.isChangeRequestTask(taskId)) {
				logger.debug("Task is not change request task");
				logger.debug("taskId " + taskId);
				return null;
			}

			HPMUniqueID projectId = session.TaskGetContainer(taskId);
			return getChangeRequestFromTask(httpServletRequest,
					projectId.toString(), projectId, taskId, minimal);
		} finally {
			lock.dispose();
		}
	}

	/**
	 * Converts a Task to an OSLC-CM HansoftChangeRequest.
	 * 
	 * @param projectIdString
	 * @param projectId
	 * @param taskId
	 * @param minimal
	 *            - if true, return minimal CR representation
	 * 
	 * @return the ChangeRequest to be serialized
	 * @throws URISyntaxException
	 *             on errors setting the bug URI
	 * @throws UnsupportedEncodingException
	 * @throws HPMSdkJavaException
	 * @throws HPMSdkException
	 * @throws NoAccessException
	 */
	public static HansoftChangeRequest getChangeRequestFromTask(
			HttpServletRequest httpServletRequest, String projectIdString,
			HPMUniqueID projectId, HPMUniqueID taskId, boolean minimal)
			throws URISyntaxException, UnsupportedEncodingException,
			HPMSdkException, HPMSdkJavaException, NoAccessException {

		final HansoftConnector hc = HansoftConnector
				.getAuthorized(httpServletRequest);
		final HPMSdkSession session = hc.getHansoftSession();

		HPMSessionLock lock = session.SessionLock();
		try {
			// Check preconditions
			if (!hc.isMemberOfProject(projectId)) {
	        	logger.error("user is not member of projectt " + projectId);
				throw new NoAccessException();
			}

			if (!hc.isChangeRequestTask(taskId)) {
				logger.debug("Task is not change request task");
				logger.debug("taskId " + taskId);				
				return null;
			}

			if (!hc.isTaskVisible(projectId, taskId)) {
				logger.debug("Task is not visible.");
				logger.debug("taskId "  + taskId);
				logger.debug("projectId " + projectId);
				return null;
			}

			return getChangeRequestFromTaskBasic(httpServletRequest,
					projectIdString, projectId, taskId, minimal);
		} finally {
			lock.dispose();
		}
	}

	/**
	 * Converts a Task to an OSLC-CM HansoftChangeRequest.
	 * 
	 * @param projectIdString
	 *            - Id to project of task container, can be Main, Backlog or QA
	 * @param projectId
	 *            - Id to project of task container, can be Main, Backlog or QA
	 * @param taskId
	 * @param minimal
	 *            - If true only attributes to display the CR in a list is
	 *            returned
	 * 
	 * @return the ChangeRequest to be serialized
	 * @throws URISyntaxException
	 *             on errors setting the bug URI
	 * @throws UnsupportedEncodingException
	 * @throws HPMSdkJavaException
	 * @throws HPMSdkException
	 */
	private static HansoftChangeRequest getChangeRequestFromTaskBasic(
			HttpServletRequest httpServletRequest, String projectIdString,
			HPMUniqueID projectId, HPMUniqueID taskId, boolean minimal)
			throws URISyntaxException, UnsupportedEncodingException,
			HPMSdkException, HPMSdkJavaException {

		final HansoftConnector hc = HansoftConnector
				.getAuthorized(httpServletRequest);

		return getChangeRequestFromTaskBasic2(httpServletRequest,
				projectIdString, projectId, taskId, minimal, hc);
	}

	/**
	 * @param httpServletRequest
	 * @param projectIdString
	 * @param projectId
	 * @param taskId
	 * @param minimal
	 * @param hc
	 * @return
	 * @throws URISyntaxException
	 * @throws HPMSdkException
	 * @throws HPMSdkJavaException
	 */
	public static HansoftChangeRequest getChangeRequestFromTaskBasic2(
			HttpServletRequest httpServletRequest, String projectIdString,
			HPMUniqueID projectId, HPMUniqueID taskId, boolean minimal,
			HansoftConnector hc) throws URISyntaxException, HPMSdkException, HPMSdkJavaException {

		String prefix = HansoftManager.getPrefix(httpServletRequest);
		HansoftChangeRequest changeRequest = new HansoftChangeRequest();

		if (hc == null) {
			hc = HansoftConnector.getAuthorized(httpServletRequest);
		}
		
		final HPMSdkSession session = hc.getHansoftSession();

		HPMSessionLock lock = session.SessionLock();
		try {
			changeRequest.setIdentifier(taskId.toString());
			URI spAbout = HansoftServiceProviderFactory
					.getAbout(projectIdString);
			changeRequest.setServiceProvider(spAbout);
			changeRequest.setAbout(getAbout(changeRequest.getIdentifier()));

			String title = hc.isTaskFieldAccessibleTo(projectId,
					EHPMTaskField.Description) ? session
					.TaskGetDescription(taskId)
					: "<Access or visibility restricted>";
			changeRequest.setTitle(prefix + title);

			// If minimal, return only the minimal attributes for e.g. list
			if (minimal) {
				return changeRequest;
			}

			// Valid for QA Projects
			String details = hc.isTaskFieldAccessibleTo(projectId,
					EHPMTaskField.DetailedDescription) ? session
					.TaskGetDetailedDescription(taskId)
					: "<Access or visibility restricted>";
			changeRequest.setDescription(prefix + details);

			HPMTaskResourceAllocation assigned = session
					.TaskGetResourceAllocation(taskId);
			if (assigned.m_Resources.size() > 0) {
				ArrayList<Person> contributors = new ArrayList<Person>();

				for (HPMTaskResourceAllocationResource resource : assigned.m_Resources) {
					if (!resource.m_ResourceID.IsValid())
						continue;

					HPMResourceProperties resourceProps = session
							.ResourceGetProperties(resource.m_ResourceID);

					Person contributor = new Person();
					contributor.setName(resourceProps.m_Name);
					contributor.setMbox(resourceProps.m_EmailAddress);
				    contributors.add(contributor);
				}

				if (!contributors.isEmpty())
					changeRequest.setContributors(contributors);
			}

			// See http://www.w3.org/TR/xmlschema-2/#dateTime
			long lastModified = session.TaskGetLastUpdatedTime(taskId);
			changeRequest.setModified(new Date(lastModified / 1000));

			EHPMTaskStatus status = session.TaskGetStatus(taskId);
			changeRequest.setStatus(prefix + HansoftOSLCMapper.getOSLCStatus(status));

			// No need to prefix priority, because it will not contain special
			// characters (FP bug)
			EHPMTaskAgilePriorityCategory priority = session
					.TaskGetAgilePriorityCategory(taskId);
			changeRequest.setPriority(HansoftOSLCMapper
					.getOSLCPriority(priority));

			float workRemaining = session.TaskGetWorkRemaining(taskId);
			changeRequest.setWorkRemaining((double) workRemaining);

			HPMUniqueID taskRefId = session.TaskGetMainReference(taskId);
			HPMUniqueID parentRefId = session.TaskRefUtilGetParent(taskRefId);

			// Check if parent is a Task Ref - if not, it's the main project
			// and we should not set the property as it's not a Task.
			if (session.UtilIsIDTaskRef(parentRefId)) {
				HPMUniqueID parentId = session.TaskRefGetTask(parentRefId);
				changeRequest.setParentTask(parentId.toString());
			}

			// Map Hansoft extended properties
			HPMTaskCustomColumnDataEnum customData = session
					.TaskEnumCustomColumnData(taskId);
			Map<QName, Object> extProps = new HashMap<QName, Object>(
					customData.m_Hashes.length);
			
			for (int i = 0; i < customData.m_Hashes.length; i++) {
				int hash = customData.m_Hashes[i];

				// Check if custom data is accessible to user
				if (!hc.isCustomColumnAccessibleTo(projectId, hash))
					continue;

				String rawData = session.TaskGetCustomColumnData(taskId, hash);
				HPMProjectCustomColumnsColumn column = session
						.ProjectGetCustomColumn(projectId, hash);
				
				// Data encoding is required especially for unknown content to
				// prevent them
				// from being altered by consumer application especially during
				// PUT transaction
				// String data = URLEncoder.encode(getColumnData(session,
				// column, rawData), "UTF-8");
				String data = getColumnData(session, column, rawData);
				if (data != null) {
					data = prefix + data;
				}
				
				String propName = AttributesMapper.getInstance()
						.getPropertyNameFromColumnName(column.m_Name);
				extProps.put(new QName(Constants.HANSOFT_NAMESPACE_EXT,
						propName), data);
			}
			changeRequest.setExtendedProperties(extProps);
		} finally {
			lock.dispose();
		}

		return changeRequest;
	}

	
	/**
	 * Translate from Hansoft column data to readable data
	 * @param session
	 * @param column
	 * @param rawData
	 * @return
	 */
	private static String getColumnData(HPMSdkSession session,
			HPMProjectCustomColumnsColumn column, String rawData) {

		if (rawData.isEmpty()) {
			return rawData;
		}

		if (column.m_Type == EHPMProjectCustomColumnsColumnType.DropList) {

			int id = Integer.parseInt(rawData);
			ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = column.m_DropListItems;
			for (HPMProjectCustomColumnsColumnDropListItem item : items) {
				if (item.m_Id == id) {			
					return item.m_Name;
				}
			}
		} else if (column.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
			String[] ids = rawData.split(";");
			String data = "";
			for (int i = 0; i < ids.length; i++) {
				if (ids[i].trim().equals("")) {
					continue;
				}
				
				if (!com.ericsson.eif.hansoft.utils.StringUtils.isNumeric(ids[i])) {
					continue;	
				}
				
				int id = Integer.parseInt(ids[i]);
				ArrayList<HPMProjectCustomColumnsColumnDropListItem> items = column.m_DropListItems;
				for (HPMProjectCustomColumnsColumnDropListItem item : items) {
					if (item.m_Id == id) {
						data = (data.isEmpty() ? item.m_Name : data + ";"
								+ item.m_Name);
						break;
					}
				}				
			}
			
			return data;
			
		} else if (column.m_Type == EHPMProjectCustomColumnsColumnType.Resources) {
			try {
				String data = "";
				HPMResourceDefinitionList list = session
						.UtilDecodeCustomColumnResourcesValue(rawData);
				for (HPMResourceDefinition resource : list.m_Resources) {
					String name = session
							.ResourceGetNameFromResource(resource.m_ID);
					data = (data.isEmpty() ? name : data + ";" + name);
				}				
				return data;				
			} catch (HPMSdkException | HPMSdkJavaException e) {
				logger.debug("Failed to get resource for " + column.m_Name
						+ " raw data: " + rawData, e);
			}
		} else if (column.m_Type == EHPMProjectCustomColumnsColumnType.DateTimeWithTime) {
			try {
				long dateTime = session.UtilDecodeCustomColumnDateTimeValue(rawData);
				Date date = new Date(dateTime / 1000);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
				return sdf.format(date);
			} catch (HPMSdkException | HPMSdkJavaException e) {
				logger.debug("Failed to get date for " + column.m_Name
						+ " raw data: " + rawData, e);
			}
		} else if (column.m_Type == EHPMProjectCustomColumnsColumnType.DateTime) {
			try {
				long dateTime = session.UtilDecodeCustomColumnDateTimeValue(rawData);
				Date date = new Date(dateTime / 1000);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				return sdf.format(date);
			} catch (HPMSdkException | HPMSdkJavaException e) {
				logger.debug("Failed to get date for " + column.m_Name
						+ " raw data: " + rawData, e);
			}
		}

		return rawData;
	}
}
