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
package com.ericsson.eif.hansoft.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Corresponds to an HTTP 401 response.
 * 
 * @author Nils Kronqvist
 */
public class NoAccessException extends RestException {

    private static final long serialVersionUID = -1866025281460310607L;

    /**
     * Default constructor
     */
    public NoAccessException() {
        super(HttpServletResponse.SC_UNAUTHORIZED, "User has no access to this resource.");
    }

    /**
     * Constructor
     * @param message
     */
    public NoAccessException(String message) {
        super(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /**
     * Constructor
     * @param t
     */
    public NoAccessException(Throwable t) {
        super(t, HttpServletResponse.SC_UNAUTHORIZED);
    }
}
