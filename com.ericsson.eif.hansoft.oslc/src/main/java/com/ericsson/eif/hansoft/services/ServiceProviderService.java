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
 *     Michael Fiedler      - implementation for Bugzilla adapter
 *     Nils Kronqvist		- adapted for Hansoft adapter
 *******************************************************************************/
package com.ericsson.eif.hansoft.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.eclipse.lyo.core.query.ParseException;
import org.eclipse.lyo.core.query.Properties;
import org.eclipse.lyo.core.query.QueryUtils;
import org.eclipse.lyo.oslc4j.core.OSLC4JConstants;
import org.eclipse.lyo.oslc4j.core.annotation.OslcCreationFactory;
import org.eclipse.lyo.oslc4j.core.annotation.OslcDialog;
import org.eclipse.lyo.oslc4j.core.annotation.OslcDialogs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespaceDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcQueryCapability;
import org.eclipse.lyo.oslc4j.core.annotation.OslcSchema;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.exception.NoAccessException;
import com.ericsson.eif.hansoft.factories.HansoftChangeRequestFactory;
import com.ericsson.eif.hansoft.factories.HansoftServiceProviderFactory;
import com.ericsson.eif.hansoft.factories.HansoftTaskFactory;
import com.ericsson.eif.hansoft.integration.logging.ChangeLogger;
import com.ericsson.eif.hansoft.integration.logging.ConnectionType;
import com.ericsson.eif.hansoft.integration.logging.HansoftLogger;
import com.ericsson.eif.hansoft.integration.logging.SynchronisationStatus;
import com.ericsson.eif.hansoft.resources.ChangeRequest;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.utils.HansoftUtils;
import com.ericsson.eif.hansoft.utils.HttpUtils;
import com.ericsson.eif.hansoft.utils.OSLCUtils;
import com.ericsson.eif.hansoft.utils.StringUtils;

import se.hansoft.hpmsdk.EHPMError;
import se.hansoft.hpmsdk.EHPMReportViewType;
import se.hansoft.hpmsdk.EHPMTaskFindFlag;
import se.hansoft.hpmsdk.HPMFindContext;
import se.hansoft.hpmsdk.HPMFindContextData;
import se.hansoft.hpmsdk.HPMProjectProperties;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMTaskEnum;
import se.hansoft.hpmsdk.HPMUniqueID;

@OslcService(OslcConstants.OSLC_CORE_DOMAIN)
@Path(HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT + "/{productId}")
public class ServiceProviderService {

	private static final Logger logger = Logger
			.getLogger(ServiceProviderService.class.getName());

	@Context
	private HttpServletRequest httpServletRequest;
	@Context
	private HttpServletResponse httpServletResponse;
	@Context
	private UriInfo uriInfo;

	@OslcDialogs({ @OslcDialog(title = "Change Request Selection Dialog", label = "Hansoft Change Request Selection Dialog", uri = HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT
			+ "/{productId}/"
			+ HansoftManager.CHANGE_REQUEST_PATH_SEGMENT
			+ "/selector", hintWidth = "525px", hintHeight = "325px", resourceTypes = { Constants.TYPE_CHANGE_REQUEST }, usages = { OslcConstants.OSLC_USAGE_DEFAULT }) })
	/**
	 * RDF/XML, XML and JSON representations of a single OSLC Service Provider
	 * 
	 * @param serviceProviderId
	 * @throws WebApplicationException
	 */	
	@GET
	@Produces({ OslcMediaType.APPLICATION_RDF_XML,
			OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
	public void getServiceProvider(
			@PathParam("productId") final String productId)
			throws WebApplicationException {

		dispatchServiceProvider(productId, "/cm/serviceprovider_rdfxml.jsp");
	}	
	
	/**
	 * OSLC compact XML representation of a single OSLC Service Provider
	 * 
	 * @param serviceProviderId
	 * @throws WebApplicationException
	 */
	@GET
	@Produces({ OslcMediaType.APPLICATION_X_OSLC_COMPACT_XML,
			OslcMediaType.APPLICATION_X_OSLC_COMPACT_JSON })
	public Compact getCompact(@PathParam("productId") final String productId)
			throws WebApplicationException {

		ServiceProvider serviceProvider = createServiceProvider(httpServletRequest, productId);		
		if (serviceProvider == null) {
			logger.error("Failed to create a Service Provider.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		final Compact compact = new Compact();

		compact.setAbout(serviceProvider.getAbout());
		compact.setShortTitle(serviceProvider.getTitle());
		compact.setTitle(serviceProvider.getTitle());

		httpServletResponse.addHeader("OSLC-Core-Version", "2.0");
		httpServletResponse.addHeader("Content-Type", "application/x-oslc-compact+xml");
		return compact;
	}

	/**
	 * HTML representation of a single OSLC Service Provider
	 * 
	 * Forwards to serviceprovider_html.jsp to create the html document
	 * 
	 * @param serviceProviderId
	 * @throws WebApplicationException 
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public void getHtmlServiceProvider(
			@PathParam("productId") final String productId)
			throws WebApplicationException {

		dispatchServiceProvider(productId, "/cm/serviceprovider_html.jsp");
	}

	/**
	 * CSV representation of change request collection
	 * 
	 * @param productId
	 * @param fromDateString
	 * @param columnString
	 */
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
	@Produces(MediaType.TEXT_PLAIN)
	public String getCSVChangeRequests(
			@PathParam("productId") final String productId,
			@QueryParam("fromDate") final String fromDateString,
			@QueryParam("columns") final String columnString) {

		final String separator = ",";
		
		Date dateFrom = null;
		try {
			if (fromDateString != null) {
				dateFrom = new SimpleDateFormat("MM/dd/yyyy/HH:mm:ss",
						Locale.ENGLISH).parse(fromDateString);
			}
		} catch (java.text.ParseException e) {
			logger.error("Problem parsing date String (MM/dd/yyyy/HH:mm:ss):"
					+ fromDateString + " getCSVTextServiceProvider"
					+ this.getClass().getName(), e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}

		String[] columns = null;
		if (columnString != null) {
			columns = columnString.split(",");
		}

		boolean found = false;
		StringBuilder result = new StringBuilder();
		List<HansoftChangeRequest> changeRequests = getChangeRequests(
				productId, null, null, null, null, null, null, null, null);
		if (columns != null) {
			// first line with column names 
			for (String column : columns) {
				result.append(column + separator);
			}
			result.append("\n");
			result.append("\n");
			for (ChangeRequest changeRequest : changeRequests) {
				if (dateFrom != null && changeRequest.getModified() != null && changeRequest.getModified().after(dateFrom)) {
					for (String column : columns) {

						QName qName = new QName(
								Constants.HANSOFT_NAMESPACE_EXT, column, "");
						if (changeRequest.getExtendedProperties().containsKey(
								qName)) {
							found = true;
							result.append(changeRequest.getExtendedProperties()
									.get(qName).toString()
									+ separator);

						} else {

							String val = "get"
									+ column.substring(0, 1).toUpperCase()
									+ column.substring(1);
							try {
								Method m = changeRequest.getClass().getMethod(
										val, (Class<?>[]) null);
								if (m != null) {
									found = true;
									result.append(m.invoke(changeRequest)
											.toString() + separator);
								}
							} catch (Exception e) {
								result.append(separator);
								logger.warn("Problem getting field value:"
										+ column + " getCSVChangeRequests"
										+ this.getClass().getName(), e);
							}
						}
					}
				}
				result.append("\n");
			}
		}
		if (!found) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		return result.toString();
	}

	public List<HansoftChangeRequest> getChangeRequests(HttpServletRequest httpServletRequest,
			@PathParam("productId") final String productId,
			@QueryParam("oslc.where") final String where,
			@QueryParam("oslc.select") final String select,
			@QueryParam("oslc.prefix") final String prefix,
			@QueryParam("page") final String pageString,
			@QueryParam("oslc.orderBy") final String orderBy,
			@QueryParam("oslc.searchTerms") final String searchTerms,
			@QueryParam("oslc.paging") final String paging,
			@QueryParam("oslc.pageSize") final String pageSize)
			throws WebApplicationException {
		boolean isPaging = false;
		if (paging != null) {
			isPaging = Boolean.parseBoolean(paging);
		}

		int page = 0;
		if (StringUtils.isNotEmpty(pageString)) {	
			try {
				page = Integer.parseInt(pageString);
			} catch (NumberFormatException e) {
				logger.error("Failed to parse pageString." + pageString, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}			
		}

		int limit = 10;
		if (isPaging && StringUtils.isNotEmpty(pageSize)) {
			try {
				limit = Integer.parseInt(pageSize);
			} catch (NumberFormatException e) {
				logger.error("Failed to parse pageSize." + pageSize, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}
		}

		Map<String, String> prefixMap;
		try {
			prefixMap = QueryUtils.parsePrefixes(prefix);
		} catch (ParseException e) {
			logger.error("Failed to parse prefixes." + prefix, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}

		addDefaultPrefixes(prefixMap);

		Properties properties;
		if (select == null) {
			properties = QueryUtils.WILDCARD_PROPERTY_LIST;
		} else {
			try {
				properties = QueryUtils.parseSelect(select, prefixMap);
			} catch (ParseException e) {
				logger.error("Failed to parse select statement: " + select, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}
		}

		Map<String, Object> propMap = QueryUtils.invertSelectedProperties(properties);
		
		List<HansoftChangeRequest> results = getChangeRequestsByProduct ( httpServletRequest, 
				productId, page, limit, where, prefixMap, propMap, orderBy, searchTerms);

		Object nextPageAttr = httpServletRequest.getAttribute(Constants.NEXT_PAGE);

		if (!isPaging && nextPageAttr != null) {
			try {
				String location = uriInfo.getBaseUri().toString()
						+ uriInfo.getPath()
						+ '?'
						+ (where != null ? ("oslc.where="
								+ URLEncoder.encode(where, "UTF-8") + '&') : "")
						+ (select != null ? ("oslc.select="
								+ URLEncoder.encode(select, "UTF-8") + '&')
								: "")
						+ (prefix != null ? ("oslc.prefix="
								+ URLEncoder.encode(prefix, "UTF-8") + '&')
								: "")
						+ (orderBy != null ? ("oslc.orderBy="
								+ URLEncoder.encode(orderBy, "UTF-8") + '&')
								: "")
						+ (searchTerms != null ? ("oslc.searchTerms="
								+ URLEncoder.encode(searchTerms, "UTF-8") + '&')
								: "") + "oslc.paging=true&oslc.pageSize="
						+ limit;

				throw new WebApplicationException(Response.temporaryRedirect(
						new URI(location)).build());
			} catch (URISyntaxException | UnsupportedEncodingException e) {
				logger.error("Illegal state.", e);
				throw new IllegalStateException(e);
			}
		}

		httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_SELECTED_PROPERTIES, propMap);

		if (nextPageAttr != null) {
			String location = "";
			try {
				location = uriInfo.getBaseUri().toString()
						+ uriInfo.getPath()
						+ '?'
						+ (where != null ? ("oslc.where="
								+ URLEncoder.encode(where, "UTF-8") + '&') : "")
						+ (select != null ? ("oslc.select="
								+ URLEncoder.encode(select, "UTF-8") + '&')
								: "")
						+ (prefix != null ? ("oslc.prefix="
								+ URLEncoder.encode(prefix, "UTF-8") + '&')
								: "")
						+ (orderBy != null ? ("oslc.orderBy="
								+ URLEncoder.encode(orderBy, "UTF-8") + '&')
								: "")
						+ (searchTerms != null ? ("oslc.searchTerms="
								+ URLEncoder.encode(searchTerms, "UTF-8") + '&')
								: "") + "oslc.paging=true&oslc.pageSize="
						+ limit + "&page=" + nextPageAttr;
			} catch (UnsupportedEncodingException e) {
				logger.error("Illegal state.", e);
				throw new IllegalStateException(e);
			}
			
			httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_NEXT_PAGE, location);
		}

		return results;
	}

	/**
	 * HTML representation of change request collection
	 * 
	 * Forwards to changerequest_collection_html.jsp to build the html page
	 * 
	 * @param productId
	 * @param where
	 * @param prefix
	 * @param pageString
	 * @param orderBy
	 * @param searchTerms
	 * @throws WebApplicationException
	 */
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
	@Produces({ MediaType.TEXT_HTML })
	public Response getHtmlChangeRequests(
			@PathParam("productId") final String productId,
			@QueryParam("oslc.where") final String where,
			@QueryParam("oslc.prefix") final String prefix,
			@QueryParam("page") final String pageString,
			@QueryParam("oslc.orderBy") final String orderBy,
			@QueryParam("oslc.searchTerms") final String searchTerms)
			throws WebApplicationException {
		
		int page = 0;
		if (StringUtils.isNotEmpty(pageString)) {
			try {
				page = Integer.parseInt(pageString);
			} catch (NumberFormatException e) {
				logger.error("Failed to parse pageString." + pageString, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}
		}

		int limit = 20;

		Map<String, String> prefixMap;
		try {
			prefixMap = QueryUtils.parsePrefixes(prefix);
		} catch (ParseException e) {
			logger.error("Failed to parse prefixes " + prefix, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}

		addDefaultPrefixes(prefixMap);

		Properties properties;
		try {
			properties = QueryUtils.parseSelect("dcterms:title", prefixMap);
		} catch (ParseException e) {
			logger.error("Failed to parse select statement.", e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}

		Map<String, Object> propMap = QueryUtils.invertSelectedProperties(properties);

		List<HansoftChangeRequest> results = getChangeRequestsByProduct(httpServletRequest, 
				productId, page, limit, where, prefixMap, propMap, orderBy, searchTerms);		
		
		httpServletRequest.setAttribute("results", results);
		httpServletRequest.setAttribute("queryUri", uriInfo.getAbsolutePath()
				.toString() + "?oslc.paging=true");

		Object nextPageAttr = httpServletRequest.getAttribute(Constants.NEXT_PAGE);

		if (nextPageAttr != null) {
			httpServletRequest.setAttribute("nextPageUri", uriInfo
					.getAbsolutePath().toString()
					+ "?oslc.paging=true&amp;page=" + nextPageAttr);
		}

		ServiceProvider serviceProvider = createServiceProvider(httpServletRequest, productId);		
		if (serviceProvider == null) {
			logger.error("Failed to create a Service Provider.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		httpServletRequest.setAttribute("serviceProvider", serviceProvider);

		RequestDispatcher rd = httpServletRequest
				.getRequestDispatcher("/cm/changerequest_collection_html.jsp");
		try {
			rd.forward(httpServletRequest, httpServletResponse);
		} catch (ServletException | IOException e) {
			logger.error("Failed to forward to jsp processing.", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	/**
	 * OSLC delegated selection dialog for change requests
	 * 
	 * If called without a "terms" parameter, forwards to
	 * changerequest_selector.jsp to build the html for the IFrame
	 * 
	 * If called with a "terms" parameter, sends a Bug search to Hansoft and
	 * then forwards to changerequest_filtered_json.jsp to build a JSON response
	 * 
	 * 
	 * @param terms
	 * @param productId
	 * @throws WebApplicationException
	 */
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT + "/selector")
	@Consumes({ MediaType.TEXT_HTML, MediaType.WILDCARD })
	public void changeRequestSelector(@QueryParam("terms") final String terms,
			@PathParam("productId") final String productId)
			throws WebApplicationException {
		
		int productIdNum = 0;
		if (StringUtils.isNotEmpty(productId)) {
			try {
				productIdNum = Integer.parseInt(productId);
			} catch (NumberFormatException e) {
				logger.error("Failed to parse productId." + productId, e);
				throw new WebApplicationException(e, Status.BAD_REQUEST);
			}
		}

		HansoftConnector hc = HansoftConnector.getAuthorized(httpServletRequest);
		HPMSdkSession session;
		try {
			session = hc.getHansoftSession();
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Failed to get a Hansoft session.", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}

		
		HPMUniqueID projectId = HansoftUtils.getProjectId(productId);
		if (projectId == null) {
			String errorMessage = "Failed to get project Id from productId " + productId; 
			logger.error(errorMessage);
			throw new WebApplicationException(new Exception(errorMessage), Status.BAD_REQUEST);
		}
		
		HPMProjectProperties projectProp = null;		
		try {
			projectProp = HansoftUtils.getProjectPropertiesByProjectID(session, projectId);
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Failed to get project properties for projectId " + projectId , e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
		
		if (projectProp != null)
			httpServletRequest.setAttribute("productName", projectProp.m_NiceName);
		else {
			logger.debug("productName is empty, because projectProperties are null");
			httpServletRequest.setAttribute("productName", "");
		}
		
		httpServletRequest.setAttribute("productId", productIdNum);
		httpServletRequest.setAttribute("selectionUri", uriInfo.getAbsolutePath().toString());

		if (terms != null) {
			httpServletRequest.setAttribute("terms", terms);
			try {
				sendFilteredBugsReponse(httpServletRequest, productId, terms);
			} catch (Exception e) {
				logger.error("Error while doing the search in Hansoft.", e);
				logger.debug("productId: " + productId, e);
				throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
			}
		} else {
			RequestDispatcher rd = httpServletRequest
					.getRequestDispatcher("/cm/changerequest_selector.jsp");
			try {
				rd.forward(httpServletRequest, httpServletResponse);
			} catch (ServletException | IOException e) {
				logger.error( "Error while forwarding the changeRequestSelector to jsp.", e);
				throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * Create and run a Hansoft search and return the result.
	 * 
	 * Forwards to changerequest_filtered_json.jsp to create the JSON response
	 * 
	 * @param httpServletRequest
	 * @param productId
	 * @param terms
	 * @throws Exception 
	 * @throws WebApplicationException 
	 */
	private void sendFilteredBugsReponse(
			final HttpServletRequest httpServletRequest,
			final String productId, final String terms)
			throws WebApplicationException, Exception {

		HansoftConnector hc = HansoftConnector.getAuthorized(httpServletRequest);
		if (hc == null) {
			logger.error("Hansoft session not found.");
			throw new ServletException("Session not found");
		}
		
		HPMUniqueID mainProjectId = HansoftUtils.getProjectId(productId);

		// We will search only in the Product Backlog
		HPMUniqueID projectId = hc.getHansoftSession()
				.ProjectOpenBacklogProjectBlock(mainProjectId);

		// To search in the Backlog the backlog id needs to be
		// specified - the EHPMReportViewType does not seem to affect this.
		HPMFindContext context = new HPMFindContext();	

		HPMFindContextData contextData = hc.getHansoftSession()
				.UtilPrepareFindContext(terms, projectId,
						EHPMReportViewType.GlobalFind, context);

		// None or/and Archived
		EnumSet<EHPMTaskFindFlag> findFlag = EnumSet.of(EHPMTaskFindFlag.None);

		// Handle paging of result etc. Also if syntax of query is not
		// correct SDK will throw parse error which currently not fed back to
		// user in search dialog. Allow result to also handle error message and
		// e.g. "Nothing found" for better UX. See also
		// http://www.hansoft.com/manuals/71/English/#find_find-query-language.htm
		HPMTaskEnum findResult = hc.getHansoftSession().TaskFind(contextData, findFlag);
		List<HansoftChangeRequest> results = new ArrayList<HansoftChangeRequest>();
		
		for (HPMUniqueID taskId : findResult.m_Tasks) {			
				HansoftChangeRequest hcr = getChangeRequestFromTask(
						httpServletRequest,	productId, projectId, taskId, true);
				
				if (hcr != null)
					results.add(hcr);			
		}
		
		httpServletRequest.setAttribute("results", results);

		RequestDispatcher rd = httpServletRequest
				.getRequestDispatcher("/cm/changerequest_filtered_json.jsp");
		try {
			rd.forward(httpServletRequest, httpServletResponse);
		} catch (ServletException | IOException e) {
			logger.error("Failed to forward to jsp processing.", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
	}	
	
	/**
	 * Create a single BugzillaChangeRequest via RDF/XML, XML or JSON POST
	 * 
	 * @param productId
	 * @param changeRequest
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	@OslcDialog(title = "Change Request Creation Dialog", label = "Hansoft Change Request Creation Dialog", uri = HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT
			+ "/{productId}/"
			+ HansoftManager.CHANGE_REQUEST_PATH_SEGMENT
			+ "/creator", hintWidth = "600px", hintHeight = "375px", resourceTypes = { Constants.TYPE_CHANGE_REQUEST }, usages = { OslcConstants.OSLC_USAGE_DEFAULT })
	@OslcCreationFactory(title = "Change Request Creation Factory", label = "Hansoft Change Request Creation", resourceShapes = { OslcConstants.PATH_RESOURCE_SHAPES
			+ "/" + Constants.PATH_CHANGE_REQUEST }, resourceTypes = { Constants.TYPE_CHANGE_REQUEST }, usages = { OslcConstants.OSLC_USAGE_DEFAULT })
	@POST
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
	@Consumes({ OslcMediaType.APPLICATION_RDF_XML,
			OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
	@Produces({ OslcMediaType.APPLICATION_RDF_XML,
			OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
	public Response addChangeRequest(@PathParam("productId") final String productId, 
			final HansoftChangeRequest changeRequest) throws WebApplicationException {
		// Create a new Task from the incoming change request, retrieve the task
		// and then convert to a HansoftChangeRequest
		
		// log only H2H requests
		String integrationType = httpServletRequest.getHeader(Constants.INTEGRATION_TYPE);
		if (Constants.H2H.equalsIgnoreCase(integrationType)) {
			HansoftLogger hl = HansoftLogger.getInstance();
			hl.log("addChangeRequest service received new change request for productId " + productId);		
			hl.log(changeRequest);
		}
		
		HansoftChangeRequest newChangeRequest;
		HPMUniqueID newTaskId;				
		String toProjectName = "";
		
		ChangeLogger incomingChangeLogger = new ChangeLogger();
		
		try {	

			// setup change logger		
			if (Constants.H2H.equalsIgnoreCase(integrationType)) {				
				String syncFrom = httpServletRequest.getHeader(Constants.SYNC_FROM_LABEL);
				String fromProject = HansoftUtils.getProjectNameFromSync(syncFrom);				
				HPMUniqueID toProjectId = HansoftUtils.getProjectId(productId);
				try {
					toProjectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), toProjectId);
				} catch (Exception e) {
					logger.error ("Error getting project name from id "+ toProjectId, e);
				}
				
				String username = HttpUtils.getUsername(httpServletRequest);
				incomingChangeLogger.setup(fromProject, toProjectName, null, username, ConnectionType.In);
			}
			
			newTaskId = HansoftTaskFactory.createTask(httpServletRequest, changeRequest, productId, incomingChangeLogger);			
			incomingChangeLogger.setTaskId(newTaskId.toString());
			
			// temporary workaround to fix problem with wrong eTags in update service
			// problem is that we create task and immediately we fetch this task, but this task is not fully created yet
			// (FullyCreated flag was not set yet)
			// which causes exceptions when we want to get or update task.			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			newChangeRequest = HansoftChangeRequestFactory.getChangeRequestFromTask(httpServletRequest, productId, newTaskId, false);
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Ok);
			
		} catch (NoAccessException e) {
            incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
            
            String username = HttpUtils.getUsername(httpServletRequest);        	
        	String message = "Error while creating task for productId " + productId + ", user " + username + " does not have access to project " + toProjectName;
        	logger.error(message);        	
        	e.setMessage(message);
            throw new WebApplicationException(e, Status.FORBIDDEN);            
		} catch (URISyntaxException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			logger.error("Error while creating task for productId " + productId, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		} catch (HPMSdkException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			if (e.GetError().equals(EHPMError.InvalidID)) {
	            logger.error("Invalid ID", e);
	            logger.debug("productId: " + productId);
	            throw new WebApplicationException(e, Status.NOT_FOUND);
	        }
			else {
				logger.error("Error while getting or creating change request for productId " + productId, e);
				throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
			}
		}
		catch (HPMSdkJavaException | UnsupportedEncodingException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			logger.error("Error while getting or creating change request for productId " + productId, e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
		finally {
			if (Constants.H2H.equalsIgnoreCase(integrationType)) {
				incomingChangeLogger.writeToCsv();
			}
		}

		OSLCUtils.setETagHeader(OSLCUtils.getETagFromChangeRequest(newChangeRequest), httpServletResponse);
		
		return Response.created(newChangeRequest.getAbout()).entity(newChangeRequest).build();
	}

	/**
	 * OSLC delegated creation dialog for a single change request
	 * 
	 * Forwards to changerequest_creator.jsp to build the html form
	 * 
	 * @param productId
	 * @throws Exception, WebApplicationException 
	 */
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT + "/creator{name:.*}")
	@Consumes({ MediaType.TEXT_HTML, MediaType.WILDCARD })
	public void changeRequestCreator(
			@PathParam("productId") final String productId,
			@PathParam("name") String name) throws Exception, WebApplicationException {

		HansoftConnector hc = HansoftConnector.getAuthorized(httpServletRequest);
		HPMSdkSession session;
		try {
			session = hc.getHansoftSession();
		} catch (HPMSdkException | HPMSdkJavaException e) {
			logger.error("Failed to get a Hansoft session.", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}

		HPMProjectProperties projectProp = null;
		HPMUniqueID projectId = HansoftUtils.getProjectId(productId);
		if (projectId != null)
			projectProp = HansoftUtils.getProjectPropertiesByProjectID(session, projectId);
		
		if (projectProp != null) 
			httpServletRequest.setAttribute("productName", projectProp.m_NiceName);	
		else  {
			httpServletRequest.setAttribute("productName", "");
			logger.debug("not possible to get productName, projectId or projectProperties are null");
		}
		
		httpServletRequest.setAttribute("creatorUri", uriInfo.getAbsolutePath().toString());
		
		// Whenever creator is called with some queryString, we store in
		// variable below
		// This is should better to clearly capture those parameter (name
		// and description)
		// For now we merge them and forward to the view to prepoluate the form
		if (name != null) {
			httpServletRequest.setAttribute("externalName", name);
		} else {
			httpServletRequest.setAttribute("externalName", " ");
		}

		RequestDispatcher rd = httpServletRequest
				.getRequestDispatcher("/cm/changerequest_creator.jsp");
		try {
			rd.forward(httpServletRequest, httpServletResponse);
		} catch (ServletException | IOException e) {
			logger.error("Failed to forward to jsp processing.", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Backend creator for the OSLC delegated creation dialog.
	 * 
	 * Accepts the input in FormParams and returns a small JSON response
	 * 
	 * @param productId
	 * @param component
	 * @param version
	 * @param summary
	 * @param op_sys
	 * @param platform
	 * @param description
	 * @throws Exception, WebApplicationException	  
	 */
	@POST
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT + "/creator{name:.*}")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public void createHtmlChangeRequest(
			@PathParam("productId") final String productId,
			@FormParam("summary") final String summary,
			@FormParam("description") final String description)
			throws WebApplicationException {

		HansoftChangeRequest changeRequest;
		
		String integrationType = httpServletRequest.getHeader(Constants.INTEGRATION_TYPE);		
		ChangeLogger incomingChangeLogger = new ChangeLogger();
		
		try {
			changeRequest = new HansoftChangeRequest();
			changeRequest.setTitle(summary);
			changeRequest.setDescription(description);

			// setup change logger
			if (Constants.H2H.equalsIgnoreCase(integrationType)) {
				String syncFrom = httpServletRequest.getHeader(Constants.SYNC_FROM_LABEL);
				String fromProject = HansoftUtils.getProjectNameFromSync(syncFrom);
				HPMUniqueID toProjectId = HansoftUtils.getProjectId(productId);
				String toProjectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), toProjectId);
				String username = HttpUtils.getUsername(httpServletRequest);
				incomingChangeLogger.setup(fromProject, toProjectName, null, username, ConnectionType.In);
			}			
			
			final HPMUniqueID newTaskId = HansoftTaskFactory.createTask(httpServletRequest, changeRequest, productId, incomingChangeLogger);
			incomingChangeLogger.setTaskId(newTaskId.toString());
			
			final HansoftChangeRequest newChangeRequest = HansoftChangeRequestFactory
					.getChangeRequestFromTask(httpServletRequest, productId, newTaskId, false);
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Ok);

			httpServletRequest.setAttribute("changeRequest", newChangeRequest);
			httpServletRequest.setAttribute("changeRequestUri", newChangeRequest.getAbout().toString());

			// Send back to the form a small JSON response
			httpServletResponse.setContentType("application/json");
			httpServletResponse.setStatus(Status.CREATED.getStatusCode());
			httpServletResponse.addHeader("Location", newChangeRequest.getAbout().toString());
			
			PrintWriter out = httpServletResponse.getWriter();
			String changeRequestLinkLabel = "Task " + newChangeRequest.getIdentifier() + ": " + summary;
			out.print("{\"title\": \"" + changeRequestLinkLabel + "\","
					+ "\"resource\" : \"" + newChangeRequest.getAbout() + "\"}");
			out.close();
		} catch (NoAccessException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			logger.error("Failed to create a change request using the creator dialog for productId " + productId + " - no access.", e);
			throw new WebApplicationException(e, Status.FORBIDDEN);
		} catch (URISyntaxException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			logger.error("Failed to create a change request using the creator dialog for productId " + productId, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		} catch (HPMSdkException e) {
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			if (e.GetError().equals(EHPMError.InvalidID)) {
	            logger.error("Invalid ID", e);
	            logger.debug("productId: " + productId);		        
	            throw new WebApplicationException(e, Status.NOT_FOUND);
	        }
			else {
				logger.error("Failed to create a change request using the creator dialog for productId " + productId, e);
				throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {			            
			incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
			logger.error("Failed to create a change request using the creator dialog for productId " + productId, e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}		
		finally {
			if (Constants.H2H.equalsIgnoreCase(integrationType)) {
				incomingChangeLogger.writeToCsv();
			}
		}
	}

	/**
	 * add default prefixes
	 * @param prefixMap
	 */
	private static void addDefaultPrefixes(final Map<String, String> prefixMap) {
		recursivelyCollectNamespaceMappings(prefixMap,
				HansoftChangeRequest.class);
	}

	/**
	 * recursively collect namespace mappings
	 * @param prefixMap
	 * @param resourceClass
	 */
	private static void recursivelyCollectNamespaceMappings(
			final Map<String, String> prefixMap,
			final Class<? extends Object> resourceClass) {
		
		final OslcSchema oslcSchemaAnnotation = resourceClass.getPackage()
				.getAnnotation(OslcSchema.class);

		if (oslcSchemaAnnotation != null) {
			final OslcNamespaceDefinition[] oslcNamespaceDefinitionAnnotations = oslcSchemaAnnotation
					.value();

			for (final OslcNamespaceDefinition oslcNamespaceDefinitionAnnotation : oslcNamespaceDefinitionAnnotations) {
				final String prefix = oslcNamespaceDefinitionAnnotation
						.prefix();
				final String namespaceURI = oslcNamespaceDefinitionAnnotation
						.namespaceURI();

				prefixMap.put(prefix, namespaceURI);
			}
		}

		final Class<?> superClass = resourceClass.getSuperclass();
		if (superClass != null) {
			recursivelyCollectNamespaceMappings(prefixMap, superClass);
		}

		final Class<?>[] interfaces = resourceClass.getInterfaces();
		if (interfaces != null) {
			for (final Class<?> interfac : interfaces) {
				recursivelyCollectNamespaceMappings(prefixMap, interfac);
			}
		}
	}

	/**
	 * @param productId
	 * @param jspPage
	 * @throws WebApplicationException
	 */
	private void dispatchServiceProvider(final String productId,
			final String jspPage) throws WebApplicationException {

		ServiceProvider serviceProvider = createServiceProvider(httpServletRequest, productId);
		if (serviceProvider == null) {
			logger.error("Failed to create a Service Provider.");
			return;
		}

		Service[] services = serviceProvider.getServices();
		if (services == null || services.length == 0) {
			logger.error("Service Provider does not include a service");
			return;
		}

		if (services.length > 1) {
			// Hansoft adapter should only have one Service per ServiceProvider
			logger.warn("Service Provider should only include one service");
		}

		httpServletRequest.setAttribute("service", services[0]);
		httpServletRequest.setAttribute("serviceProvider", serviceProvider);

		httpServletResponse.addHeader("Content-Type", "application/rdf+xml");

		RequestDispatcher rd = httpServletRequest.getRequestDispatcher(jspPage);
		try {
			rd.forward(httpServletRequest, httpServletResponse);		
		} catch (ServletException | IOException e) {
			logger.error("Error while getting the Service Provider", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**  creates service provider
	 * @param httpServletRequest
	 * @param productId
	 * @throws WebApplicationException
	 */
	private ServiceProvider createServiceProvider(HttpServletRequest httpServletRequest, final String productId) 
			throws WebApplicationException {
		
		ServiceProvider serviceProvider = null;
		try {
			serviceProvider = HansoftServiceProviderFactory
					.createServiceProvider(httpServletRequest, productId);
		} catch (NumberFormatException e) {
			logger.error("Error while parsing productId: " + productId, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}
		catch (NoAccessException e) {
			logger.error("Error while creating service provider for productId: " + productId + " - no access", e);			
			throw new WebApplicationException(e, Status.FORBIDDEN);
		}
		catch (URISyntaxException e) {
			logger.error("Error while creating service provider for productId: " + productId, e);
			throw new WebApplicationException(e, Status.BAD_REQUEST);			
		} catch (Exception e) {
			logger.error("Error while creating service provider for productId: " + productId, e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
		
		return serviceProvider;
	}
		
	/**
	 *  Find change request by product
	 * @param httpServletRequest
	 * @param productId
	 * @param page
	 * @param limit
	 * @param where
	 * @param prefixMap
	 * @param properties
	 * @param orderBy
	 * @param searchTerms
	 * @throws WebApplicationException
	 */
	private List<HansoftChangeRequest> getChangeRequestsByProduct(
			final HttpServletRequest httpServletRequest, final String productId, 
			int page, int limit, String where, Map<String, String> prefixMap, 
			Map<String, Object> properties,	String orderBy, String searchTerms) 
					throws WebApplicationException {
		
		List<HansoftChangeRequest> results = new ArrayList<HansoftChangeRequest>();
		
		try {
			results = HansoftChangeRequestFactory.getChangeRequestsByProduct(
					httpServletRequest, productId, page, limit, where,
					prefixMap, properties, orderBy, searchTerms);
			
		} catch (NoAccessException e1) {
			logger.error("Error while calling the Hansoft search by product " + productId + " - no access.", e1);
			throw new WebApplicationException(e1, Status.FORBIDDEN);
		} catch (URISyntaxException e2) {
			logger.error("Error while calling the Hansoft search.", e2);
			throw new WebApplicationException(e2, Status.BAD_REQUEST);
		} catch (HPMSdkException e3) {
			if (e3.GetError().equals(EHPMError.InvalidID)) {
	            logger.error("Invalid productId: " + productId, e3);
	            throw new WebApplicationException(e3, Status.NOT_FOUND);
	        }
			else {
				logger.error("Error while calling the Hansoft search by product " + productId, e3);
				throw new WebApplicationException(e3, Status.INTERNAL_SERVER_ERROR);
			}
		}
		catch (HPMSdkJavaException | UnsupportedEncodingException e4) {			            
			logger.error("Error while calling the Hansoft search by product " + productId, e4);
			throw new WebApplicationException(e4, Status.INTERNAL_SERVER_ERROR);
		}
		
		return results;
	}
	
	/**
	 * @param httpServletRequest
	 * @param productId
	 * @param projectId
	 * @param taskId
	 * @param minimal
	 * @throws WebApplicationException
	 */
	private HansoftChangeRequest getChangeRequestFromTask (HttpServletRequest httpServletRequest, 
			String productId, HPMUniqueID projectId, HPMUniqueID taskId, boolean minimal) 
					throws WebApplicationException {
		
		HansoftChangeRequest hcr = null;
		
		try {
			hcr = HansoftChangeRequestFactory
					.getChangeRequestFromTask(httpServletRequest,
							productId, projectId, taskId, true);
		} catch (NoAccessException e1) {
			logger.error("Error while getting change request from task " + taskId + " - no access.", e1);
			throw new WebApplicationException(e1, Status.FORBIDDEN);
		} catch (URISyntaxException e2) {
			logger.error("Error while getting change request from task " + taskId, e2);
			throw new WebApplicationException(e2, Status.BAD_REQUEST);
		} catch (HPMSdkException e3) {
			if (e3.GetError().equals(EHPMError.InvalidID)) {
	            logger.error("Invalid taskId: " + taskId, e3);
	            throw new WebApplicationException(e3, Status.NOT_FOUND);
	        }
			else {
				logger.error("Error while getting change request from task " + taskId, e3);
				throw new WebApplicationException(e3, Status.INTERNAL_SERVER_ERROR);
			}
		} catch (HPMSdkJavaException | UnsupportedEncodingException e4) {			            
			logger.error("Error while getting change request from task " + taskId, e4);
			throw new WebApplicationException(e4, Status.INTERNAL_SERVER_ERROR);
		}
		
		return hcr;
	}
	
	@OslcQueryCapability(title = "Change Request Query Capability", label = "Hansoft Change Request Catalog Query", resourceShape = OslcConstants.PATH_RESOURCE_SHAPES
			+ "/" + Constants.PATH_CHANGE_REQUEST, resourceTypes = { Constants.TYPE_CHANGE_REQUEST }, usages = { OslcConstants.OSLC_USAGE_DEFAULT })
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
	@Produces({ OslcMediaType.APPLICATION_RDF_XML,
			OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
	public List<HansoftChangeRequest> getChangeRequests(
			@PathParam("productId") final String productId,
			@QueryParam("oslc.where") final String where,
			@QueryParam("oslc.select") final String select,
			@QueryParam("oslc.prefix") final String prefix,
			@QueryParam("page") final String pageString,
			@QueryParam("oslc.orderBy") final String orderBy,
			@QueryParam("oslc.searchTerms") final String searchTerms,
			@QueryParam("oslc.paging") final String paging,
			@QueryParam("oslc.pageSize") final String pageSize)
			throws WebApplicationException {
			List<HansoftChangeRequest> hansoftChangeRequests = new ArrayList<HansoftChangeRequest>();
				hansoftChangeRequests = getChangeRequests(this.httpServletRequest, productId, where, select, prefix,
						pageString, orderBy, searchTerms, paging, pageSize);
			
				return hansoftChangeRequests;
	}
	
}
