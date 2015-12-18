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
 *******************************************************************************/
package com.ericsson.eif.hansoft.resources;

import java.net.URI;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.annotation.OslcValueType;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.ValueType;

@OslcNamespace(OslcConstants.OSLC_CORE_NAMESPACE)
@OslcName("ServiceProvider")
@OslcResourceShape(title = "OSLC Service Provider Resource Shape", describes = OslcConstants.TYPE_SERVICE_PROVIDER)
public class ServiceProviderRef extends AbstractResource {
    private final SortedSet<URI> details = new TreeSet<URI>();
    private String title;

    /**
     * Default constructor
     */
    public ServiceProviderRef() {
        super();
    }

    /**
     * @return Title of the service provider
     */
    @OslcDescription("Title of the service provider")
    @OslcPropertyDefinition(OslcConstants.DCTERMS_NAMESPACE + "title")
    @OslcReadOnly
    @OslcTitle("Title")
    @OslcValueType(ValueType.XMLLiteral)
    public String getTitle() {
        return title;
    }

    /**
     * @return URLs that may be used to retrieve web pages to determine additional details about the service provider
     */
    @OslcDescription("URLs that may be used to retrieve web pages to determine additional details about the service provider")
    @OslcPropertyDefinition(OslcConstants.OSLC_CORE_NAMESPACE + "details")
    @OslcReadOnly
    @OslcTitle("Details")
    public URI[] getDetails() {
        return details.toArray(new URI[details.size()]);
    }

    /**
     * sets title
     * @param title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * sets URI with details
     * @param details
     */
    public void setDetails(final URI[] details) {
        this.details.clear();
        if (details != null) {
            this.details.addAll(Arrays.asList(details));
        }
    }
}
