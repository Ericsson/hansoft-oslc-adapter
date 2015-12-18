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
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;

import com.ericsson.eif.hansoft.H2HConfig;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.configuration.util.CryptoUtil;
import com.ericsson.eif.hansoft.factories.ServiceProviderRefCatalogFactory;
import com.ericsson.eif.hansoft.integration.IntegrationController;
import com.ericsson.eif.hansoft.resources.ServiceProviderRef;
import com.ericsson.eif.hansoft.resources.ServiceProviderRefCatalog;
import com.ericsson.eif.hansoft.scheduler.QSchedule;

@OslcService(OslcConstants.OSLC_CORE_DOMAIN)
@Path("")
public class ServiceProviderCatalogService {
    private static final Logger logger = Logger
            .getLogger(ServiceProviderCatalogService.class.getName());

    @Context
    private HttpServletRequest httpServletRequest;
    @Context
    private HttpServletResponse httpServletResponse;
    @Context
    private UriInfo uriInfo;

    /**
     * Return the OSLC service provider catalog as RDF/XML, XML or JSON
     *  
     */
    @GET
    @Path("/details")
    @Produces({ OslcMediaType.APPLICATION_RDF_XML,
            OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
    public void getDetails() {
    	System.out.println("Details");
        return;
    }
    
    /**
     * Return encoded password
     *
     * @throws WebApplicationException
     */
    @GET
    @Path("/passEncode")
    @Produces(MediaType.TEXT_PLAIN)
    public String getEncodePasswordForH2HConfigFriend(@QueryParam("plainPassword") final String plainPassword)
    	throws WebApplicationException {
    	String result = "";
    	
    	httpServletRequest.setAttribute("selectionUri", uriInfo.getAbsolutePath().toString());
    	
    	if (plainPassword != null) {
    		try {
        		result = CryptoUtil.enCrypt(plainPassword);
    			System.out.println(result);
    		} catch (Exception e) {
    			logger.error("Error during encoding password via service.", e);
    		}
		} else {
			RequestDispatcher rd = httpServletRequest
					.getRequestDispatcher("/cm/serviceProviderPassword.jsp");
			try {
				rd.forward(httpServletRequest, httpServletResponse);
			} catch (Exception e) {
				logger.error("Error while forwarding the changeRequestSelector to jsp.", e);
				throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
			}
		}
    	
        return result;
    }

    /**
     * Return the OSLC service provider catalog as RDF/XML, XML or JSON
     * 
     * @throws WebApplicationException
     */
    @GET
    @Path(HansoftManager.CATALOG_PATH_SEGMENT)
    @Produces({ OslcMediaType.APPLICATION_RDF_XML,
            OslcMediaType.APPLICATION_XML })
    public void getServiceProviderCatalog() throws WebApplicationException {        
    	ServiceProviderRefCatalog catalog = getServiceProviderRefCatalog(httpServletRequest);
        if (catalog == null) {
            logger.info("Failed to get the Service Provider Catalog");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        
        httpServletRequest.setAttribute("catalog", catalog);
        httpServletRequest.setAttribute("baseUri", HansoftManager.getServiceBase());
        httpServletRequest.setAttribute("catalogUri",
                ServiceProviderRefCatalogFactory.getUri().toString());
        httpServletRequest.setAttribute("oauthDomain", HansoftManager.getServletBase());
        
        httpServletResponse.addHeader("OSLC-Core-Version", "2.0");
        httpServletResponse.addHeader("Content-Type", "application/rdf+xml");
        
        RequestDispatcher rd = httpServletRequest
                .getRequestDispatcher("/cm/serviceprovidercatalog_rdfxml.jsp");
        try {
            rd.forward(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            logger.error("Error while rendering the Service Provider Catalog", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Return the catalog as HTML. Forwards to serviceprovidercatalog_html.jsp
     * to build the html
     * 
     * @param serviceProviderId
     * @throws WebApplicationException
     */
    @GET
    @Path(HansoftManager.CATALOG_PATH_SEGMENT)
    @Produces(MediaType.TEXT_HTML)
    public void getHtmlServiceProviderCatalog() throws WebApplicationException {
    	ServiceProviderRefCatalog catalog = getServiceProviderRefCatalog(httpServletRequest);        
        if (catalog == null) {
            logger.info("Failed to get the Service Provider Catalog");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        
        httpServletRequest.setAttribute("catalog", catalog);
        RequestDispatcher rd = httpServletRequest
                .getRequestDispatcher("/cm/serviceprovidercatalog_html.jsp");
        try {
            rd.forward(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            logger.error("Error while rendering the Service Provider Catalog", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * RDF/XML, XML and JSON representations of an OSLC Service Provider
     * collection
     * 
     * @throws WebApplicationException
     */
    @GET
    @Path(HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT)
    @Produces({ OslcMediaType.APPLICATION_RDF_XML,
            OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
    public ServiceProviderRef[] getServiceProviders() throws WebApplicationException {

        ServiceProviderRefCatalog catalog = getServiceProviderRefCatalog(httpServletRequest);        
        if (catalog == null) {
            logger.info("Failed to get the Service Provider Catalog");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        httpServletResponse.addHeader("OSLC-Core-Version","2.0");
        httpServletResponse.addHeader("Content-Type", "application/rdf+xml");
        return catalog.getServiceProviderReferences();
    }
    
    /**
     * Return the catalog as HTML. Forwards to serviceprovidercatalog_html.jsp
     * to build the html
     * 
     * @throws WebApplicationException
     */
    @GET
    @Path(HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT)
    @Produces(MediaType.TEXT_HTML)
    public void getHtmlServiceProviders() throws WebApplicationException {
        String forwardUri = uriInfo.getBaseUri() + HansoftManager.CATALOG_PATH_SEGMENT;
        URI uri = null;
        
        try {
        	uri = new URI(forwardUri);
        }
        catch (URISyntaxException e) {
        	logger.error("Error creating uri: " + forwardUri, e);
        	throw new WebApplicationException(e, Status.BAD_REQUEST);
        }
        
        try {
            httpServletResponse.sendRedirect(forwardUri);
            Response.seeOther(uri).build();                        
        } catch (IOException e) {
            logger.error("Error sendRedirect redirect: " + forwardUri, e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
	
	/**
	 * Reloads configuration from H2HConfig.xml
	 * without need of Tomcat restart.
	 * If scheduler is running, it is stopped and started again.
	 */
	@GET
    @Path("/reloadH2Hconfiguration")	
	public void reloadH2HConfiguration() {
		logger.info("Reload of H2H configuration started");
		
		// if we are doing sync now, wait until synchronization is finished
		while (IntegrationController.isSynchronizing()) {
            try { 
            	logger.info("Synchronization is running, reload of H2HConfig.xml will wait for 2 seconds until synchronization is done");
                wait(2000);
            } catch (Exception e) {
            	logger.error ("Exception caught during waiting until synchronization is done", e);            	
            }            
        }
		
		// reload H2HConfig
		synchronized (H2HConfig.class) {
			logger.debug("Clear H2HConfig object started");
			H2HConfig.clearConfig();
			logger.debug("Clear H2HConfig object done");
			
			logger.debug("Loading of new H2HConfig started");
			HansoftManager.loadH2HConfiguration();
			logger.debug("Loading of new H2HConfig done");
		}
		
		// update scheduler, it can happen that changed H2HConfig 
		// contains new triggers or conditions for scheduler
		logger.debug("Updating scheduler started");				
		QSchedule.getInstance().update();
		logger.debug("Updating scheduler done");
		
		logger.info("Reload of H2H configuration done");		
		System.out.println("Reload of H2H configuration done");
	}
	
    /**
     * @param httpServletRequest
     * @return ServiceProviderRefCatalog
     * @throws WebApplicationException
     */
    private ServiceProviderRefCatalog getServiceProviderRefCatalog(
    		HttpServletRequest httpServletRequest) throws WebApplicationException {
    	
    	ServiceProviderRefCatalog catalog = null;
        
    	try {
            catalog = ServiceProviderRefCatalogFactory 
                    .getServiceProviderCatalog(httpServletRequest);
        } catch (Exception e) {
            logger.error("Error while getting the Service Provider Catalog", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
        
        return catalog;
    }
}