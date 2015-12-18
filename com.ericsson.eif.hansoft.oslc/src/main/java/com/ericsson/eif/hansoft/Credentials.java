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
package com.ericsson.eif.hansoft;

/**
 * Encapsulates a Hansoft username and password.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 */
public class Credentials {
    private String username;
    private String password;
    
    /**
     *  Default constructor
     */
    public Credentials() {
	}
    
    /**
     * Constructor
     * @param userName
     * @param psswd
     */
    public Credentials(String userName, String psswd) {
    	this.username = userName;
    	this.password = psswd;
	}

    /**
     * @return user name
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}