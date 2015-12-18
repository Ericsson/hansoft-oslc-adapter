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
 *     Michael Fiedler     - initial API and implementation for Bugzilla adapter
 *     Nils Kronqvist	   - adapted for Hansoft adapter     
 *******************************************************************************/
package com.ericsson.eif.hansoft.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
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

import org.apache.log4j.Logger;
import org.eclipse.lyo.core.query.ParseException;
import org.eclipse.lyo.core.query.Properties;
import org.eclipse.lyo.core.query.QueryUtils;
import org.eclipse.lyo.oslc4j.core.OSLC4JConstants;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespaceDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcSchema;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Preview;

import se.hansoft.hpmsdk.EHPMError;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.exception.NoAccessException;
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

@OslcService(Constants.CHANGE_MANAGEMENT_DOMAIN)
@Path(HansoftManager.CHANGE_REQUEST_PATH_SEGMENT)
public class HansoftChangeRequestService {

    private static final Logger logger = Logger
            .getLogger(HansoftChangeRequestService.class.getName());

    @Context
    private HttpServletRequest httpServletRequest;
    @Context
    private HttpServletResponse httpServletResponse;
    @Context
    private UriInfo uriInfo;

    /**
     * RDF/XML, XML and JSON representation of a single change request
     * 
     * @param productId
     * @param changeRequestId
     * @param propertiesString
     * @param prefix
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("{changeRequestId}")
    @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
    public HansoftChangeRequest getChangeRequest(
    		@PathParam("changeRequestId") final String changeRequestId,
    		@QueryParam("oslc.properties") final String propertiesString,
            @QueryParam("oslc.prefix") final String prefix) throws WebApplicationException {

        HansoftChangeRequest changeRequest = getChangeRequestBasic(changeRequestId);

        Map<String, String> prefixMap;
        try {
            prefixMap = QueryUtils.parsePrefixes(prefix);
        } catch (ParseException e) {
            logger.error("Failed to parse prefix " + prefix, e);
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }

        addDefaultPrefixes(prefixMap);

        Properties properties;
        if (propertiesString == null) {
            properties = QueryUtils.WILDCARD_PROPERTY_LIST;
        } else {
	            try {
	                properties = QueryUtils.parseSelect(propertiesString, prefixMap);
	            } catch (ParseException e) {
	                logger.error("Failed to parse select statement: " + propertiesString, e);
	                throw new WebApplicationException(e, Status.BAD_REQUEST);
	            }
        }

        OSLCUtils.setETagHeader(OSLCUtils.getETagFromChangeRequest(changeRequest), httpServletResponse);

        httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_SELECTED_PROPERTIES, QueryUtils.invertSelectedProperties(properties));
        httpServletResponse.addHeader("OSLC-Core-Version", "2.0");
        // Lyo adds "Content-Type" automatically according to "Accept" in request
        // no need to add it manually here

		HansoftLogger hl = HansoftLogger.getInstance();
		hl.log("sending change request " + changeRequestId);		
		hl.log(changeRequest);
        
        return changeRequest;
    }

    /**
     * OSLC Compact representation of a single change request
     * 
     * Contains a reference to the smallPreview method in this class for the
     * preview document
     * 
     * @param productId
     * @param changeRequestId
     * @return
     * @throws WebApplicationException
     */
    @GET
    @Path("{changeRequestId}")
    @Produces({OslcMediaType.APPLICATION_X_OSLC_COMPACT_XML })
    public Compact getCompact(
    		@PathParam("productId") final String productId,
    		@PathParam("changeRequestId") final String changeRequestId)
            throws WebApplicationException {
    	
        Compact compact = null;
        String uri = null;
        
        try {
            HansoftChangeRequest hcr = getChangeRequestBasic(changeRequestId);

            compact = new Compact();
            compact.setAbout(HansoftChangeRequestFactory.getAbout(hcr.getIdentifier()));
            compact.setTitle(hcr.getTitle());
            compact.setShortTitle(hcr.getTitle());
           
            uri = "http://hansoft.com/wp-content/uploads/2013/01/favicon.ico"; 
            compact.setIcon(new URI(uri));

            // Create and set attributes for OSLC Preview Resource
            final Preview smallPreview = new Preview();
            smallPreview.setHintHeight("11em");
            smallPreview.setHintWidth("45em");
            uri = compact.getAbout().toString() + "/smallPreview";
            smallPreview.setDocument(new URI(uri));
            compact.setSmallPreview(smallPreview);

            // Use the HTML representation of a change request as the large
            // preview as well
            final Preview largePreview = new Preview();
            largePreview.setHintHeight("20em");
            largePreview.setHintWidth("45em");
            uri = compact.getAbout().toString()+ "/largePreview";
            largePreview.setDocument(new URI(uri));
            compact.setLargePreview(largePreview);
           
        } catch (URISyntaxException e) {
            logger.error("Failed to create URI " + uri, e);
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }
        
        httpServletResponse.addHeader("OSLC-Core-Version","2.0");
        httpServletResponse.addHeader("Content-Type", "application/x-oslc-compact+xml");
        return compact;
    }

    /**
     * OSLC small preview for a single change request
     * 
     * Forwards to changerequest_preview_small.jsp to build the html
     * 
     * @param productId
     * @param changeRequestId
     * @throws WebApplicationException
     */
    @GET
    @Path("{changeRequestId}/smallPreview")
    @Produces({ MediaType.TEXT_HTML })
    public void getSmallPreview(
    		@PathParam("productId")       final String productId, 
    		@PathParam("changeRequestId") final String changeRequestId)
            throws WebApplicationException {

        HansoftChangeRequest hcr = getChangeRequestBasic(changeRequestId);
         
        hcr.setAbout( HansoftChangeRequestFactory.getAbout( changeRequestId ) );

        final String hansoftUri = HansoftManager.getServiceBase().toString();
        httpServletRequest.setAttribute("changeRequest", hcr);
        httpServletRequest.setAttribute("bugzillaUri", hansoftUri);

        RequestDispatcher rd = httpServletRequest
                .getRequestDispatcher("/cm/changerequest_preview_small.jsp");
        try {
            rd.forward(httpServletRequest, httpServletResponse);
        } catch (ServletException | IOException e) {
            logger.error("Failed to forward to jsp processing.", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * OSLC large preview for a single change request
     * 
     * Forwards to changerequest_preview_large.jsp to build the html
     * 
     * @param productId
     * @param changeRequestId
     * @throws WebApplicationException
     */
    @GET
    @Path("{changeRequestId}/largePreview")
    @Produces({ MediaType.TEXT_HTML })
    public void getLargePreview(
    		@PathParam("productId")       final String productId,
    		@PathParam("changeRequestId") final String changeRequestId)
            throws WebApplicationException {

        HansoftChangeRequest hcr = getChangeRequestBasic(changeRequestId);
        
        hcr.setAbout( HansoftChangeRequestFactory.getAbout( changeRequestId ) );

        final String hansoftUri = HansoftManager.getServiceBase().toString();
        httpServletRequest.setAttribute("changeRequest", hcr);
        httpServletRequest.setAttribute("bugzillaUri", hansoftUri);

        RequestDispatcher rd = httpServletRequest
                .getRequestDispatcher("/cm/changerequest_preview_large.jsp");
        try {
            rd.forward(httpServletRequest, httpServletResponse);
        } catch (ServletException | IOException e) {
            logger.error("Failed to forward to jsp processing.", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * HTML representation for a single change request
     * 
     * @param productId
     * @param changeRequestId
     * @throws WebApplicationException
     */
    @GET
    @Path("{changeRequestId}")
    @Produces({ MediaType.TEXT_HTML })
    public Response getHtmlChangeRequest(
            @PathParam("changeRequestId") final String changeRequestId)
            throws WebApplicationException {

        HansoftChangeRequest hcr = getChangeRequestBasic(changeRequestId);

        httpServletRequest.setAttribute("changeRequest", hcr);
        RequestDispatcher rd = httpServletRequest
                .getRequestDispatcher("/cm/changerequest_html.jsp");
        try {
            rd.forward(httpServletRequest, httpServletResponse);
        } catch (ServletException | IOException e) {
            logger.error("Failed to forward to jsp processing.", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }

    /**
     * Updates a single change request via RDF/XML, XML or JSON PUT
     * 
     * @param eTagHeader
     * @param changeRequestId
     * @param changeRequest
     * @return
     * @throws WebApplicationException
     */
    @PUT
    @Consumes({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
    @Path("{taskId}")
    public Response updateChangeRequest(
            @HeaderParam("If-Match") final String eTagHeader,
            @PathParam("taskId") final String taskId,
            final HansoftChangeRequest updatedChangeRequest)
            throws WebApplicationException {
    	
        // Get the existing Task into a ChangeRequest
        HansoftChangeRequest hcr = getChangeRequestBasic(taskId);
                
        String syncFrom = httpServletRequest.getHeader(Constants.SYNC_FROM_LABEL);
        String fromProject = HansoftUtils.getProjectNameFromSync(syncFrom);
        
        // log only requests from H2H
		String integrationType = httpServletRequest.getHeader(Constants.INTEGRATION_TYPE);
		if (Constants.H2H.equalsIgnoreCase(integrationType)) {        
			HansoftLogger hl = HansoftLogger.getInstance();
			hl.log("updateChangeRequest service received updated change request " + taskId + " from " + fromProject);		
			hl.log(updatedChangeRequest);          		
		}
		
		HPMUniqueID changedTaskId = HansoftUtils.getTaskId(taskId);
		HPMUniqueID changedTaskProjectId = null;
		String changedTaskProjectName = "";		
		try {
			changedTaskProjectId = HansoftUtils.getProjectIdOfTask(changedTaskId);
    		HPMUniqueID changedTaskRealProjectId = HansoftUtils.getRealProjectIdOfProjectId(changedTaskProjectId);
			changedTaskProjectName = HansoftUtils.getProjectNameByProjectID(HansoftManager.getMainSession(), changedTaskRealProjectId);			
		} catch (Exception e) {
			logger.error("Error getting project name from task id", e);
		}
		
		// setup change logger but log only changes in H2H
		ChangeLogger incomingChangeLogger = new ChangeLogger();
		if (Constants.H2H.equalsIgnoreCase(integrationType)) {			
			String username = HttpUtils.getUsername(httpServletRequest);
			incomingChangeLogger.setup(fromProject, changedTaskProjectName, changedTaskId, username, ConnectionType.In);
		} 	
				
        try {
        	//This is to check if tasks has changed since previous GET method
            final String originalETag = OSLCUtils.getETagFromChangeRequest(hcr);
            updatedChangeRequest.setIdentifier(hcr.getIdentifier());
            
            //This is useful when using dialogs for relatedChangeRequest
            //If customer try to link to a task, we need to extract backLink
            if (updatedChangeRequest.getHyperlink() == null) {
            	updatedChangeRequest.setHyperlink(OSLCUtils.getRelatedChangeRequestBackLink(hcr.getHyperlink(), updatedChangeRequest));
            }

            if (eTagHeader == null || originalETag.equals(eTagHeader)) {
                
            	HansoftTaskFactory.updateTask(httpServletRequest, updatedChangeRequest, incomingChangeLogger);
            
            	// temporary workaround to fix problem with wrong eTags
    			try {
    				Thread.sleep(2000);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    			
            	incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Ok);
                OSLCUtils.setETagHeader(OSLCUtils.getETagFromChangeRequest(updatedChangeRequest), httpServletResponse);
            } else {
            	logger.error("Failed to update change request " + taskId + " -preconditions not met.");
            	logger.debug("originalETag and eTagHeader are not same");
            	logger.debug("eTagHeader:" + eTagHeader);
            	logger.debug("originalETag:" + originalETag);
            	
            	// do not throw exception here because it will be caught in catch(Exception) block bellow
            	// and we will return status INTERNAL_SERVER_ERROR instead of PRECONDITION_FAILED
            	return Response.status(Status.PRECONDITION_FAILED).build();                
            }
        } catch (NoAccessException e) {
        	incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
        	
        	String username = HttpUtils.getUsername(httpServletRequest);        	
        	String message = "Failed to update task " + taskId + ", user " + username + " does not have access to project " + changedTaskProjectName;
        	logger.error(message);        	
        	e.setMessage(message);
            throw new WebApplicationException(e, Status.FORBIDDEN);            
        } catch (HPMSdkException e) {        	
        	incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
        	
            if (e.GetError().equals(EHPMError.InvalidID)) {            	            	
                logger.error("Invalid task id: " + taskId, e);
                throw new WebApplicationException(e, Status.NOT_FOUND);
            }
            else {            	
            	logger.error("Error while retrieving the change request: " + taskId, e);
                throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
        	incomingChangeLogger.setSynchronisationStatus(SynchronisationStatus.Error);
            logger.error("Failed to update change request " + taskId, e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
        finally {
        	if (Constants.H2H.equalsIgnoreCase(integrationType)) {
        		incomingChangeLogger.writeToCsv();        		
        	}        	
        }
        
        return Response.ok().build();
    }

    /**
     * @param changeRequestId
     * @return Hansoft change request object or null
     * @throws WebApplicationException
     */
    private HansoftChangeRequest getChangeRequestBasic(final String changeRequestId) 
    		throws WebApplicationException {

        HansoftChangeRequest changeRequest = null;
        
        try {
            changeRequest = HansoftChangeRequestFactory.getChangeRequestFromTask(httpServletRequest, changeRequestId, false);
        } catch (URISyntaxException e) {
        	logger.error("Error while retrieving the change request: " + changeRequestId + " - uri syntax is wrong", e);
        	throw new WebApplicationException(e, Status.BAD_REQUEST);
        } catch (NoAccessException e) {
        	String username = HttpUtils.getUsername(httpServletRequest);        	
        	String message = "Error while retrieving the change request " + changeRequestId + ", user " + username + " does not have access to it";
        	logger.error(message);        	
        	e.setMessage(message);
            throw new WebApplicationException(e, Status.FORBIDDEN);            
        } catch (HPMSdkException e) {
            if (e.GetError().equals(EHPMError.InvalidID)) {
                logger.error("Invalid task id: " + changeRequestId, e);
                throw new WebApplicationException(e, Status.NOT_FOUND);
            } else {
                logger.error("Error while retrieving the change request: " + changeRequestId, e);
                throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
        	logger.error("Error while retrieving the change request: " + changeRequestId, e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        } 
        
        if (changeRequest == null) {
            logger.info("Change request not found: " + changeRequestId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        
        return changeRequest;
    }

    /**
     * Adds default prefixes to change request
     * @param prefixMap
     */
    private static void addDefaultPrefixes(final Map<String, String> prefixMap) {
        recursivelyCollectNamespaceMappings(prefixMap, HansoftChangeRequest.class);
    }

    /**
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
   
}
