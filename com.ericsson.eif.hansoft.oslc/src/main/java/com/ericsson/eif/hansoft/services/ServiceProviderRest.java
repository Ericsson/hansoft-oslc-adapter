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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.core.annotation.OslcDialog;
import org.eclipse.lyo.oslc4j.core.annotation.OslcDialogs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.mapping.AttributesMapper;
import com.ericsson.eif.hansoft.resources.ChangeRequest;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;

@OslcService(OslcConstants.OSLC_CORE_DOMAIN)
@Path(HansoftManager.REST_PATH_SEGMENT + "/" + HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT + "/{productId}")
public class ServiceProviderRest {

	private static final Logger logger = Logger
			.getLogger(ServiceProviderRest.class.getName());
	
	private ServiceProviderService serviceProviderService = new ServiceProviderService();

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
		} catch (java.text.ParseException e1) {
			logger.warn("Problem parsing date String (MM/dd/yyyy/HH:mm:ss):"
					+ fromDateString + " getCSVTextServiceProvider"
					+ this.getClass().getName(), e1);
			throw new WebApplicationException(e1, Status.BAD_REQUEST);
		}

		String[] columns = null;
		if (columnString != null) {
			columns = columnString.split(",");
		}
		
		boolean found = false;
		StringBuilder result = new StringBuilder();
		List<HansoftChangeRequest> changeRequests = serviceProviderService.getChangeRequests(this.httpServletRequest,
				productId, null, null, null, null, null, null, null, null);
		if (columns != null) {
			// first line with column names 
			for (String column : columns) {
				result.append(column + separator);
			}
			//encode for comparison
			for (int i=0; i < columns.length; i++ ) {
				columns[i] = AttributesMapper.getInstance().getPropertyNameFromColumnName(columns[i]);
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
	
	@OslcDialogs({ @OslcDialog(title = "Change Request Selection Dialog", label = "Hansoft Change Request Selection Dialog", uri = HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT
			+ "/{productId}/"
			+ HansoftManager.CHANGE_REQUEST_PATH_SEGMENT
			+ "/selector", hintWidth = "525px", hintHeight = "325px", resourceTypes = { Constants.TYPE_CHANGE_REQUEST }, usages = { OslcConstants.OSLC_USAGE_DEFAULT }) })
	@GET
	@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
	@Produces({OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
	public List<HansoftChangeRequest> getXmlJsonChangeRequests(
			@PathParam("productId") final String productId,
			@QueryParam("fromDate") final String fromDateString,
			@QueryParam("columns") final String columnString) {
		
		Date dateFrom = null;
		try {
			if (fromDateString != null) {
				dateFrom = new SimpleDateFormat("MM/dd/yyyy/HH:mm:ss",
						Locale.ENGLISH).parse(fromDateString);
			}
		} catch (java.text.ParseException e1) {
			logger.warn("Problem parsing date String (MM/dd/yyyy/HH:mm:ss):"
					+ fromDateString + " getCSVTextServiceProvider"
					+ this.getClass().getName(), e1);
			throw new WebApplicationException(e1, Status.BAD_REQUEST);
		}

		String[] columns = null;
		if (columnString != null) {
			columns = columnString.split(",");
		}
		
		
		List<HansoftChangeRequest> hansoftChangeRequests = new ArrayList<HansoftChangeRequest>();
		hansoftChangeRequests = serviceProviderService.getChangeRequests(this.httpServletRequest,
				productId, null, null, null, null, null, null, null, null);
	
		return hansoftChangeRequests;
	}
	
}
