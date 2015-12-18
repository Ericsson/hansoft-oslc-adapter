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
 *     Michael Fiedler     - initial API and implementation for Bugzilla adapter
 *     
 *******************************************************************************/
package com.ericsson.eif.hansoft;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.oauth.OAuth;
import net.oauth.OAuthException;
import net.oauth.OAuthProblemException;
import net.oauth.http.HttpMessage;
import net.oauth.server.OAuthServlet;

import org.apache.log4j.Logger;
import org.eclipse.lyo.server.oauth.consumerstore.FileSystemConsumerStore;
import org.eclipse.lyo.server.oauth.core.Application;
import org.eclipse.lyo.server.oauth.core.AuthenticationException;
import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.OAuthRequest;
import org.eclipse.lyo.server.oauth.core.token.LRUCache;
import org.eclipse.lyo.server.oauth.core.token.SimpleTokenStrategy;

import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;

import com.ericsson.eif.hansoft.exception.UnauthorizedException;
import com.ericsson.eif.hansoft.utils.HttpUtils;

public class CredentialsFilter implements Filter {

    private static final Logger logger = Logger
            .getLogger(CredentialsFilter.class.getName());

    public static final String CONNECTOR_ATTRIBUTE = "org.eclipse.lyo.oslc4j.bugzilla.HansoftConnector";
    public static final String CREDENTIALS_ATTRIBUTE = "org.eclipse.lyo.oslc4j.bugzilla.Credentials";
    private static final String ADMIN_SESSION_ATTRIBUTE = "org.eclipse.lyo.oslc4j.bugzilla.AdminSession";
    public static final String JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM = "invalid_expired_token";
    public static final String OAUTH_REALM = "Hansoft";

    private static LRUCache<String, HansoftConnector> keyToConnectorCache = new LRUCache<String, HansoftConnector>(
            200);

    @Override
    public void destroy() {
    }

    /**
     * Check for OAuth or BasicAuth credentials and challenge if not found.
     * 
     * Store the HansoftConnector in the HttpSession for retrieval in the REST
     * services.
     */
    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Don't protect requests to the admin parts of the oauth service.        
        String pathInfo = request.getPathInfo(); 
        if (pathInfo != null && pathInfo.startsWith("/oauth")) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        String token = OAuthServlet.getMessage(request, null).getToken();
        if (token != null) {
            // OAuth - check if authenticated
            if (!isOAuthAuthenticated(token, request, response)) {
                return;
            }
        } else {
            // Basic Authentication - check if authenticated
            if (!isBasicAuthenticated(request, response)) {
                return;
            }
        }

        // Authenticated - let call through
        chain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Check if the oauth token is valid, and if so
     * 
     * @param token
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    private boolean isOAuthAuthenticated(String token,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try {
            try {
                    OAuthRequest oAuthRequest = new OAuthRequest(request);
                    oAuthRequest.validate();

	                HansoftConnector connector = keyToConnectorCache.get(token);
	
	                // Check so session is still valid - if not, reset
	                if (connector != null && !HansoftManager.isConnected()) {
	                    HansoftManager.clearSession();
	                    connector = null;
	                    keyToConnectorCache.remove(token);
	                    logger.info("Hansoft session disconnected - will retry to connect.");
	                }

                    if (connector == null) {
                        throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
                    }

                    request.getSession().setAttribute(CONNECTOR_ATTRIBUTE, connector);

            } catch (OAuthProblemException e) {
                if (OAuth.Problems.TOKEN_REJECTED.equals(e.getProblem()))
                    throwInvalidExpiredException(e);
                else
                    throw e;
            }
        } catch (OAuthException e) {
            OAuthServlet.handleException(response, e, OAUTH_REALM);
            return false;
        }

        return true;
     }

    /**
     * Check if the basic credentials are valid, if so - create and 
     * store a Hansoft connection in the session.
     * 
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    private boolean isBasicAuthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        HansoftConnector connector = (HansoftConnector) session.getAttribute(CredentialsFilter.CONNECTOR_ATTRIBUTE);
        if (connector != null) {
            if (!HansoftManager.isConnected()) {
                // Session is not valid - reset
            	HansoftManager.clearSession();
            	connector = null;
            	session.setAttribute(CONNECTOR_ATTRIBUTE, null);
            	logger.info("Hansoft session disconnected - will retry to connect.");
            } else {
                // We have an authenticated connector in session - return
                return true;
            }
        }

        try {
        	// Try getting credentials from request and create a connection
            Credentials credentials = (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE);
            if (credentials == null) {
                credentials = HttpUtils.getCredentials(request);
                if (credentials == null) {
                    throw new UnauthorizedException();
                }
            }
            
            if (credentials != null) {
                connector = HansoftConnector.createAuthorized(credentials);
                if (connector == null) {
                    // Failed creating a connector for some other reason then access. Try restart the SDK connection.
                    HansoftManager.clearSession();
                    logger.info("Hansoft session not connected - will retry to connect.");
                    return false;
                }

                // We have an authenticated connector in session
                session.setAttribute(CONNECTOR_ATTRIBUTE, connector);
                session.setAttribute(CREDENTIALS_ATTRIBUTE, credentials);
            }

        } catch (UnauthorizedException e) {
        	// This will trigger the browser to open a basic login dialog
            logger.info("Sending unauthorized-response to get browser login dialog.");
            HttpUtils.sendUnauthorizedResponse(response, e);
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        OAuthConfiguration config = OAuthConfiguration.getInstance();

        // Validates a user's ID and password.
        config.setApplication(new Application() {
            @Override
            public void login(HttpServletRequest request, String id,
                    String password) throws AuthenticationException {
                try {
                    Credentials creds = new Credentials();
                    creds.setUsername(id);
                    creds.setPassword(password);

                    HansoftConnector hc = HansoftConnector
                            .createAuthorized(creds);

                    request.setAttribute(CONNECTOR_ATTRIBUTE, hc);
                    request.getSession().setAttribute(CREDENTIALS_ATTRIBUTE,
                            creds);
                    request.getSession().setAttribute(ADMIN_SESSION_ATTRIBUTE,
                            hc.isAdmin());

                } catch (HPMSdkException e) {
                    throw new AuthenticationException(
                            e.getCause().getMessage(), e);
                } catch (HPMSdkJavaException e) {
                    throw new AuthenticationException(
                            e.getCause().getMessage(), e);
                } catch (UnauthorizedException e) {
                    throw new AuthenticationException(e.getMessage(), e);
                }
            }

            @Override
            public String getName() {
                // Display name for this application.
                return "Hansoft";
            }

            @Override
            public boolean isAdminSession(HttpServletRequest request) {
                return Boolean.TRUE.equals(request.getSession().getAttribute(
                        ADMIN_SESSION_ATTRIBUTE));
            }

            @Override
            public String getRealm(HttpServletRequest request) {
                return HansoftManager.REALM;
            }

            @Override
            public boolean isAuthenticated(HttpServletRequest request) {
                HansoftConnector hc = (HansoftConnector) request.getSession()
                        .getAttribute(CONNECTOR_ATTRIBUTE);
                if (hc == null) {
                    return false;
                }

                request.setAttribute(CONNECTOR_ATTRIBUTE, hc);
                return true;
            }
        });

        /*
         * Override some SimpleTokenStrategy methods so that we can keep the
         * HansoftConnection associated with the OAuth tokens.
         */
        config.setTokenStrategy(new SimpleTokenStrategy() {
            @Override
            public void markRequestTokenAuthorized(
                    HttpServletRequest httpRequest, String requestToken)
                    throws OAuthProblemException {
                HansoftConnector hc = (HansoftConnector) httpRequest
                        .getAttribute(CONNECTOR_ATTRIBUTE);
                keyToConnectorCache.put(requestToken, hc);
                super.markRequestTokenAuthorized(httpRequest, requestToken);
            }

            @Override
            public void generateAccessToken(OAuthRequest oAuthRequest)
                    throws OAuthProblemException, IOException {
                String requestToken = oAuthRequest.getMessage().getToken();
                HansoftConnector hc = keyToConnectorCache.remove(requestToken);
                super.generateAccessToken(oAuthRequest);
                keyToConnectorCache.put(oAuthRequest.getAccessor().accessToken,
                        hc);
            }
        });

        try {
            // For now, keep the consumer info in a file. This is only the
            // credentials for allowing server access - to be able to read/write
            // you also need to log in  with a valid Hansoft user. So assume this
            // info is not that sensitive.
            String root = HansoftManager.getAdapterServletHome();
            String oauthStore = root + File.separator + "adapterOAuthStore.xml"; 
            config.setConsumerStore(new FileSystemConsumerStore(oauthStore));
            makeFileOwnerPrivate(oauthStore);
        } catch (Throwable t) {
            logger.error("Error initializing the OAuth consumer store.", t);
        }
    }
    
    /**
     * Makes file read/writable only to the owner     
     * @param absolutePath
     */
    private void makeFileOwnerPrivate(String absolutePath) {
        // create empty file if it does not already exist
        File file = new File(absolutePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error("Error creating file: " + absolutePath, e);
            }
        }

        // on a POSIX system - protect the file from "others":
        if (FileSystems.getDefault().supportedFileAttributeViews()
                .contains("posix")) {
            Path path = Paths.get(absolutePath);
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            try {
                Files.setPosixFilePermissions(path, permissions);
            } catch (IOException e) {
                logger.error("Error protecting the file: " + absolutePath, e);
            }
        }
    }

    /**
     * Jazz requires a exception with the magic string "invalid_expired_token"
     * to restart OAuth authentication
     * 
     * @param e
     * @return
     * @throws OAuthProblemException
     */
    private void throwInvalidExpiredException(OAuthProblemException e)
            throws OAuthProblemException {
        OAuthProblemException ope = new OAuthProblemException(
                JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM);
        ope.setParameter(HttpMessage.STATUS_CODE, new Integer(
                HttpServletResponse.SC_UNAUTHORIZED));
        throw ope;
    }
}
