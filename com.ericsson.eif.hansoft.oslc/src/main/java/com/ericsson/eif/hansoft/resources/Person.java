/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ericsson.eif.hansoft.resources;

import java.net.URI;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;

import com.ericsson.eif.hansoft.Constants;

/**
 * A FOAF Person.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 * @see <a href="http://xmlns.com/foaf/spec/">FOAF Vocabulary Specification</a>
 */
@OslcNamespace(Constants.FOAF_NAMESPACE)
@OslcResourceShape(title = "FOAF Person Resource Shape", describes = Constants.TYPE_PERSON)
public class Person extends AbstractResource {
    private URI uri = null;
    private String name = null;
    private String mbox = null;

    /**
     * @return uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * sets uri
     * @param uri
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * @return person name
     */
    @OslcDescription("A FOAF name ")
    @OslcPropertyDefinition(Constants.FOAF_NAMESPACE + "name")
    @OslcReadOnly
    @OslcTitle("Name")
    public String getName() {
        return name;
    }

    /**
     * sets person name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return email address
     */
    @OslcDescription("A FOAF Email address ")
    @OslcPropertyDefinition(Constants.FOAF_NAMESPACE + "mbox")
    @OslcReadOnly
    @OslcTitle("Email Address")
    public String getMbox() {
        return mbox;
    }

    /**
     * sets email address
     * @param mbox
     */
    public void setMbox(String mbox) {
        this.mbox = mbox;
    }
}
