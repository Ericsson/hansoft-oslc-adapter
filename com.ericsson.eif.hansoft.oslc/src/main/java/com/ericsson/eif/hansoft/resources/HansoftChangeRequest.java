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
 *	   Sam Padgett	       - initial API and implementation
 *     Michael Fiedler     - adapted for OSLC4J
 *     
 *******************************************************************************/
package com.ericsson.eif.hansoft.resources;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.mapping.AttributesMapper;

//OSLC4J should give an rdf:type of oslc_cm:ChangeRequest
@OslcNamespace(Constants.CHANGE_MANAGEMENT_NAMESPACE)
@OslcName(Constants.CHANGE_REQUEST)
@OslcResourceShape(title = "Change Request Resource Shape", describes = Constants.TYPE_CHANGE_REQUEST)
public final class HansoftChangeRequest extends ChangeRequest {

	/**
	 * Default constructor
	 * @throws URISyntaxException
	 */
	public HansoftChangeRequest() throws URISyntaxException {
        super();
    }
    
    /**
     * Constructor
     * @param about
     * @throws URISyntaxException
     */
    public HansoftChangeRequest(URI about) throws URISyntaxException {
        super(about);
    }

    // Hansoft extended attributes beyond OSLC base ChangeRequest
    private Double workRemaining;
    private String hyperlink;
    private String parentTask;

    /**
     * @return remaining work
     */
    @OslcDescription("The remaining work in hours for this change request.")
    @OslcPropertyDefinition(Constants.HANSOFT_NAMESPACE + "workRemaining")
    @OslcTitle("Work Remaining")
    public Double getWorkRemaining() {
        return workRemaining;
    }

    @OslcDescription("A link to an associated item.")
    @OslcPropertyDefinition(Constants.HANSOFT_NAMESPACE + "hyperlink")
    @OslcTitle("Hyperlink")
    public String getHyperlink() {
        return hyperlink;
    }
    
    @OslcDescription("Id of the parent Task.")
    @OslcPropertyDefinition(Constants.HANSOFT_NAMESPACE + "parentTask")
    @OslcTitle("Parent Task")
    public String getParentTask() {
        return parentTask;
    }

    /**
     * sets work remaining
     * @param workRemaining
     */
    public void setWorkRemaining(Double workRemaining) {
        this.workRemaining = workRemaining;
    }

    /**
     * sets hyperlink
     * @param hyperlink
     */
    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }
    
    /**
     * sets parent task
     * @param parentTask
     */
    public void setParentTask(String parentTask) {
        this.parentTask = parentTask;
    }
    
    /**
     * See comment @AttributesMapper.
     * @return extended info
     */
    public String getExtendendInfo() {
    	return AttributesMapper.getInstance().getExtendedInfo(this);
    }
}
