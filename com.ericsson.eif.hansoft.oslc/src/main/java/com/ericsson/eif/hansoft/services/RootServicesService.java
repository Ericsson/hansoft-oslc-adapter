/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
package com.ericsson.eif.hansoft.services;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.factories.ServiceProviderRefCatalogFactory;

/**
 * Jazz Root Services Service, see:
 * https://jazz.net/wiki/bin/view/Main/RootServicesSpec
 * https://jazz.net/wiki/bin/view/Main/RootServicesSpecAddendum2
 */
public class RootServicesService extends HttpServlet {

    private static final long serialVersionUID = -8125286361811879744L;
    private static final Logger logger = Logger
            .getLogger(RootServicesService.class.getName());

    /**
     * Return a Rational Jazz compliant root services document
     * 
     * See https://jazz.net/wiki/bin/view/Main/RootServicesSpec
     * 
     * @throws WebApplicationException
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws WebApplicationException {

        request.setAttribute("baseUri", HansoftManager.getServiceBase());
        request.setAttribute("catalogUri", ServiceProviderRefCatalogFactory
                .getUri().toString());
        request.setAttribute("oauthDomain", HansoftManager.getServletBase());
        response.addHeader("OSLC-Core-Version", "2.0");
        response.addHeader("Content-Type", "application/rdf+xml");
        final RequestDispatcher rd = request
                .getRequestDispatcher("/cm/rootservices_rdfxml.jsp");

        try {
            rd.forward(request, response);
            response.flushBuffer();
        } catch (Exception e) {
            logger.error(
                    "Error while forwarding the rootservice request to jsp.", e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
}
