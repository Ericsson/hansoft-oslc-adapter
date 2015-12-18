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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.eclipse.lyo.oslc4j.client.ServiceProviderRegistryURIs;
import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.PrefixDefinition;
import org.eclipse.lyo.oslc4j.core.model.Publisher;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderFactory;

import se.hansoft.hpmsdk.HPMProjectProperties;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.exception.NoAccessException;
import com.ericsson.eif.hansoft.resources.ServiceProviderRef;
import com.ericsson.eif.hansoft.services.ServiceProviderService;

public class HansoftServiceProviderFactory {
    // The class(es) with e.g. Dialog, creator and query annotations.
    private static Class<?>[] RESOURCE_CLASSES = { ServiceProviderService.class };
    
    /**
     * Default constructor 
     */
    private HansoftServiceProviderFactory() {
        super();
    }

    /**
     * @param serviceProviderId
     * @return About uri
     * @throws URISyntaxException
     */
    public static URI getAbout(final String serviceProviderId)
            throws URISyntaxException {
        return new URI(getAboutString(serviceProviderId));
    }

    /**
     * @param serviceProviderId
     * @return about uri as string
     */
    public static String getAboutString(final String serviceProviderId) {
        String basePath = HansoftManager.getServiceBase();
        return basePath + "/" + HansoftManager.SERVICE_PROVIDER_PATH_SEGMENT
                + "/" + serviceProviderId;
    }

    /**
     * Create a new Hansoft OSLC change management service provider.
     * 
     * @param httpServletRequest
     * @param product
     * @param serviceProviderId
     * @return
     * @throws URISyntaxException
     * @throws HPMSdkJavaException
     * @throws HPMSdkException
     * @throws OslcCoreApplicationException
     * @throws WebApplicationException
     */
    public static ServiceProvider createServiceProvider(
            HttpServletRequest httpServletRequest,
            final String serviceProviderId) throws NumberFormatException,
            NoAccessException, URISyntaxException, HPMSdkException,
            HPMSdkJavaException, OslcCoreApplicationException {

        ServiceProvider hsp = null;
        HPMUniqueID projectId = new HPMUniqueID();

        // Will throw NumberFormatException if not correct number id
        projectId.m_ID = Integer.decode(serviceProviderId);

        // Get the project
        HansoftConnector hc = HansoftConnector
                .getAuthorized(httpServletRequest);
        HPMSdkSession session = hc.getHansoftSession();
        
        // Check if user is member of the project - if not, skip as it should
        // not be visible
        if (!hc.isMemberOfProject(projectId)) {
            throw new NoAccessException();
        }
        
        HPMProjectProperties projectProp = session.ProjectGetProperties(projectId);
        String product = projectProp.m_NiceName;

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("productId", serviceProviderId);

        String basePath = HansoftManager.getServiceBase();

        hsp = ServiceProviderFactory.createServiceProvider(basePath,
                ServiceProviderRegistryURIs.getUIURI(), product,
                "Service provider for Hansoft product: " + product,
                new Publisher("Eclipse Lyo", "urn:oslc:ServiceProvider"),
                RESOURCE_CLASSES, parameterMap);

        // Seems as this is needs to be set, but not accessed(?). If omitted the
        // RTC
        // dialog for finding SPs to configure to a project will not work.
        URI detailsURIs[] = { new URI(basePath + "/details") };
        hsp.setDetails(detailsURIs);

        final PrefixDefinition[] prefixDefinitions = {
                new PrefixDefinition(OslcConstants.DCTERMS_NAMESPACE_PREFIX,
                        new URI(OslcConstants.DCTERMS_NAMESPACE)),
                new PrefixDefinition(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX,
                        new URI(OslcConstants.OSLC_CORE_NAMESPACE)),
                new PrefixDefinition(OslcConstants.OSLC_DATA_NAMESPACE_PREFIX,
                        new URI(OslcConstants.OSLC_DATA_NAMESPACE)),
                new PrefixDefinition(OslcConstants.RDF_NAMESPACE_PREFIX,
                        new URI(OslcConstants.RDF_NAMESPACE)),
                new PrefixDefinition(OslcConstants.RDFS_NAMESPACE_PREFIX,
                        new URI(OslcConstants.RDFS_NAMESPACE)),
                new PrefixDefinition(
                        Constants.CHANGE_MANAGEMENT_NAMESPACE_PREFIX, new URI(
                                Constants.CHANGE_MANAGEMENT_NAMESPACE)),
                new PrefixDefinition(Constants.HANSOFT_NAMESPACE_PREFIX,
                        new URI(Constants.HANSOFT_NAMESPACE)),
                new PrefixDefinition(Constants.HANSOFT_NAMESPACE_PREFIX_EXT,
                        new URI(Constants.HANSOFT_NAMESPACE_EXT)),
                new PrefixDefinition(Constants.FOAF_NAMESPACE_PREFIX, new URI(
                        Constants.FOAF_NAMESPACE)), };

        hsp.setPrefixDefinitions(prefixDefinitions);

        hsp.setAbout(getAbout(serviceProviderId));
        hsp.setIdentifier(serviceProviderId);
        hsp.setCreated(new Date());

        return hsp;
    }

    /**
     * Create a new Hansoft OSLC change management reference service provider.
     * "Reference" meaning a minimal implementation, including what's needed for
     * the most common UC, i.e. show a list.
     * 
     * @param product
     * @return
     * @throws URISyntaxException
     * @throws HPMSdkJavaException
     * @throws HPMSdkException
     */
    public static ServiceProviderRef createServiceProviderRef(
            HansoftConnector hc, HPMUniqueID projectId)
            throws URISyntaxException, HPMSdkException, HPMSdkJavaException {

        // Check if user is member of the project - if not, skip as it should
        // not be visible
        if (!hc.isMemberOfProject(projectId))
            return null;

        final ServiceProviderRef hsp = new ServiceProviderRef();

        HPMSdkSession session = hc.getHansoftSession();
        HPMProjectProperties projectProp = session
                .ProjectGetProperties(projectId);
        hsp.setTitle(projectProp.m_NiceName);

        hsp.setAbout(getAbout(projectId.toString()));

        // Seems as this is needs to be set, but not accessed(?). If omitted the
        // RTC
        // dialog for finding SPs to configure to a project will not work.
        URI detailsURIs[] = { new URI(HansoftManager.getServiceBase()
                + "/details") };
        hsp.setDetails(detailsURIs);

        return hsp;
    }
}
