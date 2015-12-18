/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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

import net.oauth.OAuthException;

/**
 * A special unauthorized exception indicating an OAuth problem.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 */
public class HansoftOAuthException extends UnauthorizedException {
    
    private static final long serialVersionUID = 6028256054048106565L;
    
    /**
     * Constructor
     * @param e
     */
    public HansoftOAuthException(OAuthException e) {
        super(e);
    }
}
