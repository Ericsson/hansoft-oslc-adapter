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
package com.ericsson.eif.hansoft.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.server.OAuthServlet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.Credentials;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.exception.HansoftOAuthException;
import com.ericsson.eif.hansoft.exception.UnauthorizedException;

/**
 * Utilities for working with HTTP requests and responses.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 */
public class HttpUtils {

	private static final Logger logger = Logger.getLogger(HttpUtils.class.getName());

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String BASIC_AUTHORIZATION_PREFIX = "Basic ";
    private static final String BASIC_AUTHENTICATION_CHALLENGE = BASIC_AUTHORIZATION_PREFIX
            + "realm=\"" + HansoftManager.REALM + "\"";
    private static final String OAUTH_AUTHORIZATION_PREFIX = "OAuth ";
    private static final String OAUTH_AUTHENTICATION_CHALLENGE = OAUTH_AUTHORIZATION_PREFIX
            + "realm=\"" + HansoftManager.REALM + "\"";

    /**
     * Gets the credentials from an HTTP request.
     * 
     * @param request
     *            the request
     * @return the Bugzilla credentials or <code>null</code> if the request did
     *         not contain an <code>Authorization</code> header
     * @throws UnauthorizedException
     *             on problems reading the credentials from the
     *             <code>Authorization</code> request header
     */
    public static Credentials getCredentials(HttpServletRequest request)
            throws UnauthorizedException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || "".equals(authorizationHeader)) {
            return null;
        }

        Credentials credentials = new Credentials();
        if (!authorizationHeader
                .startsWith(HttpUtils.BASIC_AUTHORIZATION_PREFIX)) {
            throw new UnauthorizedException(
                    "Only basic access authentication is supported.");
        }

        String encodedString = authorizationHeader
                .substring(HttpUtils.BASIC_AUTHORIZATION_PREFIX.length());
        try {
            String unencodedString = new String(Base64.decode(encodedString),
                    "UTF-8");
            int seperator = unencodedString.indexOf(':');
            if (seperator == -1) {
                throw new UnauthorizedException(
                        "Invalid Authorization header value.");
            }

            credentials.setUsername(unencodedString.substring(0, seperator));
            credentials.setPassword(unencodedString.substring(seperator + 1));
        } catch (DecodingException e) {
            throw new UnauthorizedException(
                    "Username and password not Base64 encoded.");
        } catch (UnsupportedEncodingException e) {
            throw new UnauthorizedException(
                    "Invalid Authorization header value.");
        }

        return credentials;
    }

    /** gets user name from httpServletRequest
    * @param session
    * @return logged in user name or Unknown in case of error
    */
    public static String getUsername(HttpServletRequest httpServletRequest) {		
		
		String username = "Unknown";
		
		if (httpServletRequest == null) {
			logger.error("Error while getting username from httpServletRequest.");
			logger.debug("parameter httpServletRequest is null.");
	    	logger.debug("Using default username " + username);
			return username;
		}
		
		Credentials credentials = null;
	   
		try {
	     	credentials = HttpUtils.getCredentials(httpServletRequest);
			} catch (UnauthorizedException e) {
				logger.error("Error while getting credentials from httpServletRequest.", e);								
		    	logger.debug("Using default username " + username);
				e.printStackTrace();
				return username;
			}
	    
	    if (credentials == null) {
	 	   logger.error("Error while getting username from credentials.");
	 	   logger.debug("Credentials are null");
	 	   logger.debug("Using default username " + username);
	 	   return username;
	    }
	  
	     username = credentials.getUsername();
	     return username;
	}
    
    /**
     * @param response
     * @param e - unauthorized exception
     * @throws IOException
     * @throws ServletException
     */
    public static void sendUnauthorizedResponse(HttpServletResponse response,
            UnauthorizedException e) throws IOException, ServletException {
        if (e instanceof HansoftOAuthException) {
            OAuthServlet.handleException(response, e, HansoftManager.REALM);
        } else {
            // Accept basic access or OAuth authentication.
            response.addHeader(WWW_AUTHENTICATE_HEADER,
                    OAUTH_AUTHENTICATION_CHALLENGE);
            response.addHeader(WWW_AUTHENTICATE_HEADER,
                    BASIC_AUTHENTICATION_CHALLENGE);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
    
    /**
     * @param response
     * @return content of http response
     * @throws IOException
     */
    public static String getContent(HttpResponse response) throws IOException {        
    	if (response == null)
    		return "";
    	
    	HttpEntity entity = response.getEntity();
        if (entity == null)
        	return "";
        
        InputStream content = entity.getContent();
        if (content == null)
        	return "";
        
        StringBuilder contents = new StringBuilder(50000);
        BufferedReader br = null;
        
        try {         
          br = new BufferedReader(new InputStreamReader(content, "UTF-8"));
          String line = "";
          while ((line = br.readLine()) != null) {
            contents.append(line);
            contents.append(Constants.LS);
          }          
        } catch (IOException e) {
        	logger.error("error reading content from HttpResponse", e);
        } 
        finally 
        {
          if (br != null)
        	  br.close();
        }
        
        return contents.toString();
      }
    
    /**
     * @param response
     * @return location from http response 
     * 
     * Location: http://<Hansoft server address>:8443/test/services/tasks/2571     * 
     */
    public static String getLocation(HttpResponse response) {
    	String location = response.getFirstHeader(Constants.LOCATION_HEADER_FIELD).toString();
    	if (!StringUtils.isEmpty(location) && location.startsWith(Constants.LOCATION_PREFIX)) {
    		location = location.substring(Constants.LOCATION_PREFIX.length());
    		return location.trim();
    	}
    	return "";
	}
}

