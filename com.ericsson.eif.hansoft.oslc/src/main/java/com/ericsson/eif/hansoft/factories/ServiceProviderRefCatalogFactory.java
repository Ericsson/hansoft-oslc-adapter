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
 *     Michael Fiedler      - adapted for Bugzilla service provider
 *******************************************************************************/
package com.ericsson.eif.hansoft.factories;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.core.model.OAuthConfiguration;
import org.eclipse.lyo.oslc4j.core.model.Publisher;

import se.hansoft.hpmsdk.HPMProjectEnum;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.resources.ServiceProviderRef;
import com.ericsson.eif.hansoft.resources.ServiceProviderRefCatalog;

/**
 * This is the OSLC service provider catalog factory for the Hansoft adapter. It
 * does not save any state, so re-generates for each call. The entries are
 * minimal representations of ServiceProviders to make the catalog small in size
 * even in case of 100+ entries.
 */
public class ServiceProviderRefCatalogFactory {
    private static final Logger logger = Logger
            .getLogger(ServiceProviderRefCatalogFactory.class.getName());

    private static URI about;
    private static final String title;
    private static final String description;
    private static final Publisher publisher;

    static {
        try {
            title = "OSLC Service Provider Catalog";
            description = "OSLC Service Provider Catalog";
            publisher = new Publisher("Ericsson EIF",
                    "com.ericsson.eif.hansoft");
            publisher.setIcon(new URI(
                    "http://open-services.net/css/images/logo-forflip.png"));
        } catch (final URISyntaxException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     *  private constructor  - no need to instantiate this
     */
    private ServiceProviderRefCatalogFactory() {
        super();
    }

    /**
     * @return about URI
     */
    public static URI getUri() {
    	if (about == null) {
    		try {
				about = new URI(HansoftManager.getServiceBase() + "/catalog");
			} catch (URISyntaxException e) {
				logger.error("Failed to create URI for the catalog", e);
			}
    	}
        return about;
    }

    /**
     * Return the minimal ServiceProviderCatalog
     * 
     * @param httpServletRequest
     * @return ServiceProviderCatalog
     */
    public static ServiceProviderRefCatalog getServiceProviderCatalog(
            HttpServletRequest httpServletRequest) {

        ServiceProviderRefCatalog serviceProviderCatalog = new ServiceProviderRefCatalog();

        serviceProviderCatalog.setAbout(getUri());
        serviceProviderCatalog.setTitle(title);
        serviceProviderCatalog.setDescription(description);
        serviceProviderCatalog.setPublisher(publisher);

        OAuthConfiguration oauthConf = new OAuthConfiguration();
        String baseUri = HansoftManager.getServiceBase();
        try {
            oauthConf.setAuthorizationURI(new URI(baseUri + "/oauth/authorize"));
            oauthConf.setOauthAccessTokenURI(new URI(baseUri + "/oauth/accessToken"));
            oauthConf.setOauthRequestTokenURI(new URI(baseUri+ "/oauth/requestToken"));
        } catch (URISyntaxException e1) {
			logger.error("Failed to create URI for the catalog", e1);
        }
        
        serviceProviderCatalog.setOauthConfiguration(oauthConf);

        // The generic OSLC Bugzilla example code added based on domains in SPs.
        // As we know domain
        // we can add explicitly, but might be more elegant reverting to
        // original code.
        try {
            serviceProviderCatalog.addDomain(new URI(Constants.CHANGE_MANAGEMENT_DOMAIN));
        } catch (URISyntaxException e) {
        	logger.error("Failed add doamin URI " + Constants.CHANGE_MANAGEMENT_DOMAIN , e);
        }

        try {
            HansoftConnector hc = HansoftConnector.getAuthorized(httpServletRequest);
            HPMSdkSession session = hc.getHansoftSession();
            HPMProjectEnum Projects = session.ProjectEnum();
            for (HPMUniqueID projectId : Projects.m_Projects) {
                ServiceProviderRef hsp = HansoftServiceProviderFactory
                        .createServiceProviderRef(hc, projectId);
            
                if (hsp != null)
                    serviceProviderCatalog.addServiceProvider(hsp);
            }

        } catch (Exception e) {
            logger.error("Error when creating a SP from Hansoft project.", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }

        return serviceProviderCatalog;
    }
}
