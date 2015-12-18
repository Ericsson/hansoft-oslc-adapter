/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation.
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
 *     Samuel Padgett       - remove final from class
 *******************************************************************************/
package com.ericsson.eif.hansoft.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcRange;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcRepresentation;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.annotation.OslcValueShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcValueType;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.Representation;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.eclipse.lyo.oslc4j.core.model.ValueType;

@OslcNamespace(OslcConstants.OSLC_CORE_NAMESPACE)
@OslcName("ServiceProviderCatalog")
@OslcResourceShape(title = "OSLC Service Provider Catalog Resource Shape", describes = OslcConstants.TYPE_SERVICE_PROVIDER_CATALOG)
public class ServiceProviderRefCatalog extends ServiceProviderCatalog {
    private final List<ServiceProviderRef> serviceProviders = new ArrayList<ServiceProviderRef>();

    /**
     * Default constructor
     */
    public ServiceProviderRefCatalog() {
        super();
    }

    /**
     * add service provider
     * @param serviceProvider
     */
    public void addServiceProvider(final ServiceProviderRef serviceProvider) {
        this.serviceProviders.add(serviceProvider);
    }

    /* (non-Javadoc)
     * @see org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog#getServiceProviders()
     * Override to remove annotations
     */
    public ServiceProvider[] getServiceProviders() {
        return new ServiceProvider[0];
    }

    @OslcDescription("Service providers")
    @OslcName("serviceProvider")
    @OslcPropertyDefinition(OslcConstants.OSLC_CORE_NAMESPACE
            + "serviceProvider")
    @OslcRange(OslcConstants.TYPE_SERVICE_PROVIDER)
    @OslcReadOnly
    @OslcRepresentation(Representation.Inline)
    @OslcTitle("Service Providers")
    @OslcValueShape(OslcConstants.PATH_RESOURCE_SHAPES + "/"
            + OslcConstants.PATH_SERVICE_PROVIDER)
    @OslcValueType(ValueType.LocalResource)
    public ServiceProviderRef[] getServiceProviderReferences() {
        return serviceProviders.toArray(new ServiceProviderRef[serviceProviders
                .size()]);
    }

    /**
     * removes service provider
     * @param serviceProvider
     */
    public void removeServiceProvider(final ServiceProviderRef serviceProvider) {
        serviceProviders.remove(serviceProvider);
    }

    /**
     * set serviceProviders 
     * @param serviceProviders
     */
    public void setServiceProviders(final ServiceProviderRef[] serviceProviders) {
        this.serviceProviders.clear();
        if (serviceProviders != null) {
            this.serviceProviders.addAll(Arrays.asList(serviceProviders));
        }
    }
}
