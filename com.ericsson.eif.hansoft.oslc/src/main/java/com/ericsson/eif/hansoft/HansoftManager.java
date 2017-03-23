/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation.
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
 *	   Sam Padgett	       - initial API and implementation
 *     Michael Fiedler     - adapted for OSLC4J
 *******************************************************************************/

package com.ericsson.eif.hansoft;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.lyo.oslc4j.client.ServiceProviderRegistryURIs;

import com.ericsson.eif.hansoft.configuration.util.H2HValidator;
import com.ericsson.eif.hansoft.scheduler.QSchedule;

import se.hansoft.hpmsdk.EHPMError;
import se.hansoft.hpmsdk.EHPMSdkDebugMode;
import se.hansoft.hpmsdk.HPMSdkCallbacks;
import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMSessionLock;

public class HansoftManager implements ServletContextListener,
        HttpSessionListener {

    private static final Logger logger = Logger.getLogger(HansoftManager.class
            .getName());

    public final static String REALM = "Hansoft";

    // Name of the custom column where the OSLC backLink is stored.
    public static String OSLC_BACKLINK_COL_NAME;

    private static String hansoftServer;
    private static String hansoftWorkingDir = "";
    private static String hansoftChangeLogDir = "";
    private static String hansoftLogDir = "";    
    private static String hansoftLib;
    private static int hansoftPort;
    private static String hansoftSDKVersion;
    private static String hansoftSDK;
    private static String hansoftSDKPassword;
    private static String hansoftDatabase;

    private static String hansoftAdapterServerScheme;
    private static String hansoftAdapterServerPort;
    private static String validate_h2h_config;
    private static String h2h_config_validation_output_to_file;
    private static String h2h_config_validation_output_on_screen;
    private static List<String> hansoftFPs;

    // In production make sure that the hostname used is the
    // public dns name for the server i.e.
    // hansoft-services.internal.ericsson.com    
    private static String hansoftAdapterServer;

    private static HPMSdkSession hsSession = null;

    private static String servletBase = null;
    private static String serviceBase = null;

    // The first part of the path segment. Set in the web.xml for jetty config
    // and by naming the war (i.e. name.war --> PROVIDER_CONTEXT_PATH = name)
    public static String PROVIDER_CONTEXT_PATH;

    // Path to the config area for a specific adapter instance 
    public static String adapterServletHome;

    public static final String PROVIDER_SERVICE_PATH = "/services";
    public static final String REST_PATH_SEGMENT = "rest";
    public static final String CATALOG_PATH_SEGMENT = "catalog";
    public static final String SERVICE_PROVIDER_PATH_SEGMENT = "projects";
    public static final String CHANGE_REQUEST_PATH_SEGMENT = "tasks";

    private static final String SYSTEM_PROPERTY_NAME_REGISTRY_URI = ServiceProviderRegistryURIs.class
            .getPackage().getName() + ".registryuri";

    /**
     * Hansoft adapter properties from adapter.properties
     * @param servletContext
     */
    private static void init(ServletContext servletContext) {

        Properties props = new Properties();
        try {
            PROVIDER_CONTEXT_PATH = servletContext.getContextPath();
            if (PROVIDER_CONTEXT_PATH.isEmpty()) {
                String message = "No servlet context name provided, will exit.";
                logger.error(message);
                throw new RuntimeException(message);
            }

            String adapterHome = System.getenv("ADAPTER_HOME");
            if (adapterHome == null) {
                // default to user home
                adapterHome = System.getProperty("user.home");
            }

            // The PROVIDER_CONTEXT_PATH has a beginning "/" - remove
            String contextPath = PROVIDER_CONTEXT_PATH.substring(1);
            adapterServletHome = adapterHome + File.separator + contextPath;

            // Need the properties file - if not found, exit
            try {
                File propsPath = new File(adapterServletHome + File.separator + "adapter.properties");
                if (propsPath.exists()) {
                    props.load(new FileInputStream(propsPath.toString()));
                } else {
                    String message = "The adapter.properties file not found, will exit.";
                    logger.error(message);
                    throw new RuntimeException(message);
                }
            } catch (Exception e) {
                String message = "Failed to read the adapter.properties file, will exit.";
                logger.error(message, e);
                throw new RuntimeException(message);
            }

            // It is ok not having a log4j configuration file, but recommended
            try {
                File log4jPropsPath = new File(adapterServletHome + File.separator + "log4j.properties");
                if (log4jPropsPath.exists()) {
                	// Allow using adapter home path in log4j file
        			System.setProperty("hansoft.adapter_servlet_home", adapterServletHome);
                    PropertyConfigurator.configure(log4jPropsPath.getPath());
                } else {
                    logger.warn("The log4j.properties file not found.");
                }
            } catch (Exception e) {
                logger.warn("Failed to read the log4j.properties file.", e);
            }
            
    		logger.info("Initialize of Hansoft adapter started ...");

            // We need to set the JNI path early, and failed when trying to
            // pass as input argument. Solution as below is working:
            //
            // From
            // http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
            //
            // At first the system property is updated with the new value.
            // This might be a relative path – or maybe you want to create that
            // path dynamically.
            //
            // The Classloader has a static field (sys_paths) that contains the
            // paths. If that field is set to null, it is initialized
            // automatically. Therefore forcing that field to null will result
            // into the reevaluation of the library path as soon as
            // loadLibrary() is called
            hansoftSDKVersion = props.getProperty("hansoft_sdk_version", "").trim();
            if (hansoftSDKVersion == "") {
                String message = "SDK version not set, will exit.";
                logger.error(message);
                throw new RuntimeException(message);
            }

            try {
                hansoftLib = adapterHome + File.separator + "hansoft_libs" + File.separator + hansoftSDKVersion;
                File libPath = new File(hansoftLib);
                if (libPath.exists()) {
                    System.setProperty("java.library.path", libPath.toString());
                    Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                    fieldSysPath.setAccessible(true);
                    fieldSysPath.set(null, null);
                } else {
                    String message = "Hansoft libs for SDK version: " + hansoftSDKVersion + " not found, will exit.";
                    logger.error(message);
                    throw new RuntimeException(message);
                }
            } catch (Exception e) {
                String message = "Failed configuring path for libs for SDK version: " + hansoftSDKVersion + ", will exit.";
                logger.error(message);
                throw new RuntimeException(message);
            }

            hansoftWorkingDir = props.getProperty("hansoft_working_dir", "").trim();
            hansoftChangeLogDir = props.getProperty("hansoft_change_log_dir", "").trim();
            hansoftLogDir = props.getProperty("hansoft_log_dir", "").trim();
            hansoftServer = props.getProperty("hansoft_server", "").trim();
            hansoftPort = Integer.parseInt(props.getProperty("hansoft_port", "-1"));
            hansoftDatabase = props.getProperty("hansoft_database", "").trim();
            hansoftSDK = props.getProperty("hansoft_sdk_user", "").trim();
            hansoftSDKPassword = props.getProperty("hansoft_sdk_password", "").trim();
            hansoftAdapterServer = props.getProperty("hansoft_adapter_server", "").trim();
            hansoftAdapterServerScheme = props.getProperty("hansoft_adapter_server_scheme", "http").trim();
            hansoftAdapterServerPort = props.getProperty("hansoft_adapter_server_port", "8080").trim();
            validate_h2h_config = props.getProperty("validate_h2h_config", "false").trim();
            h2h_config_validation_output_to_file = props.getProperty("h2h_config_validation_output_to_file", "true").trim();
            h2h_config_validation_output_on_screen = props.getProperty("h2h_config_validation_output_on_screen", "true").trim();
            
            OSLC_BACKLINK_COL_NAME = props.getProperty("hansoft_backlink", "OSLC Reference").trim();
            String FPs = props.getProperty("hansoft_fps", "").trim();
            setHansoftFPs(FPs);
            
            logger.log(Level.INFO, "Property hansoft_adapter_server = " + hansoftAdapterServer);
            logger.log(Level.INFO, "Property hansoft_adapter_server_scheme = " + hansoftAdapterServerScheme);
            logger.log(Level.INFO, "Property hansoft_adapter_server_port = " + hansoftAdapterServerPort);
            logger.log(Level.INFO, "Property hansoft_working_dir = " + hansoftWorkingDir);
            logger.log(Level.INFO, "Property hansoft_change_log_dir = " + hansoftChangeLogDir);
            logger.log(Level.INFO, "Property hansoft_log_dir = " + hansoftLogDir);
            logger.log(Level.INFO, "Property hansoft_server = " + hansoftServer);            
            logger.log(Level.INFO, "Property hansoft_port = " + hansoftPort);
            logger.log(Level.INFO, "Property hansoft_database = " + hansoftDatabase);
            logger.log(Level.INFO, "Property hansoft_sdk_user = " + hansoftSDK);
            logger.log(Level.INFO, "Property hansoft_sdk_version = " + hansoftSDKVersion);
            logger.log(Level.INFO, "Property validate_h2h_config = " + validate_h2h_config);
            logger.log(Level.INFO, "Property h2h_config_validation_output_to_file = " + h2h_config_validation_output_to_file);
            logger.log(Level.INFO, "Property h2h_config_validation_output_on_screen = " + h2h_config_validation_output_on_screen);
            
            getMainSession();
            loadH2HConfiguration();
            if (validate_h2h_config.equalsIgnoreCase("true")) {
            	validateH2Hconfiguration();
            }
            
            QSchedule.getInstance();

        } catch (SecurityException | IllegalArgumentException | HPMSdkException | HPMSdkJavaException e) {
            logger.error("Failed during static init of Hansoft Adapter", e);
        }
    }    	

	/**
	 * Read H2H configuration file
	 */
	 public static void loadH2HConfiguration() {
        String configPath = "";
        try {
        	configPath = adapterServletHome + File.separator + Constants.H2H_CONFIG_FILE;;
        	H2HConfig.getConfigFromFile(configPath);
        } catch (Throwable t) {
        	logger.error("Failed to parse H2H configuration file: " + configPath, t);
        }
	}

	/**
	 * Validates configuration in H2HConfig.xml file
	 * - if file is in correct format (XSD validation)
	 * - if provided values are valid 
	 *   e.g. projects are existing, if user has access to them,
	 *   mandatory integration columns exist and have correct type ...
	 *   
	 *   @see H2HValidator
	 */
	private static void validateH2Hconfiguration() {
		H2HValidator validator = new H2HValidator();
		if (h2h_config_validation_output_on_screen.equalsIgnoreCase("true")) {
			validator.setOutputToScreen(true);			
		} else {
			validator.setOutputToScreen(false);
		}
		
		if (h2h_config_validation_output_to_file.equalsIgnoreCase("true")) {
			validator.setOutputToFile(true);			
		} else {
			validator.setOutputToFile(false);
		}
				
		validator.validate();
	}

	/**
	 * @param fPs
	 */
	private static void setHansoftFPs(String fPs) {
    	hansoftFPs = new ArrayList<String>();
    	
    	if (StringUtils.isEmpty(fPs)) {
    		return;
    	}
		
    	String[] fPsParts = fPs.split(",");
    	for (String part : fPsParts) {
    		if (part != null && !"".equals(part.trim())) {
    			hansoftFPs.add(part);
    		}
    	}
	}

    /**
     * @return Hansoft SDK session
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public static HPMSdkSession getMainSession() throws HPMSdkException, HPMSdkJavaException {

        if (hsSession != null) {
            return hsSession;
        }

        // Change to EHPMSdkDebugMode.Debug to get memory leak info and
        // debug output. Note: this is expensive.
        EHPMSdkDebugMode debugMode = EHPMSdkDebugMode.Off;

        // The Semaphore's release() is called on each callback, so process
        // - and check for case where the session is invalid = exception
        // with EHPMError_ConnectionLost.
        @SuppressWarnings("serial")
        Semaphore hsSemaphore = new Semaphore(1, true) {
            @Override
            public void release() {
                if (hsSession != null) {
                    try {
                        hsSession.SessionProcess();
                    } catch (HPMSdkException e) {
                        logger.warn("Error from Hansoft Adapter: " + e.ErrorAsStr(), e);
                        if (e.GetError().equals(EHPMError.ConnectionLost)) {
                            clearSession();
                        }
                    } catch (HPMSdkJavaException e) {
                        logger.warn("Error from Hansoft Adapter: " + e.ErrorAsStr(), e);
                    }
                }
                super.release();
            }
        };

        // Callbacks will be received as result of calling SessionProcess().
        HPMSdkCallbacks hsCallbacks = new HansoftCallbacks();

        hsSession = HPMSdkSession.SessionOpen(hansoftServer, // Address to
                                                             // connect to
                hansoftPort, // Port of the server to connect to
                hansoftDatabase, // The database of the server to connect to
                hansoftSDK, // The name of the resource (SDK user) to log in
                            // as
                hansoftSDKPassword, // Password for SDK user
                hsCallbacks, // Callback if using Semaphore it will be
                             // called on SessionProcess()
                hsSemaphore, // Semaphore
                true, // Block on operations
                debugMode, // Debug mode
                0, // Number of Sessions. Each session will give a in memory
                   // copy of Hansoft DB, likely not needed.
                hansoftWorkingDir, // Working directory where db cache etc
                                   // is stored
                hansoftLib, // Path to the Hansoft library (HPMSdk.x64.dylib
                            // on mac os)
                null); // Certificate settings

        return hsSession;
    }

    /**
     * Clear the session - typically called if the session is broken
     */
    public static void clearSession() {
        HansoftConnector.releaseAll();
        hsSession = null;
    }

    /**
     * Per recommendation this was the way to check if the session is still
     * connected. If not connected we will get a EHPMError.ConnectionLost - for
     * now, all errors --> false.
     * 
     * @return true if conected, otherwise false
     */
    public static boolean isConnected() {
        HPMSessionLock lock = null;
        try {
            lock = getMainSession().SessionLock();
            getMainSession().ProjectEnum();
//            getMainSession().SessionProcess();
            lock.dispose();
            return true;
        } catch (HPMSdkException e) {
            if (e.GetError().equals(EHPMError.ConnectionLost)) {
                logger.warn("Connection to Hansoft server lost.");
            }
            return false;
        } catch (HPMSdkJavaException e) {
            return false;
        } finally {
            if (lock != null)
                lock.dispose();
        }
    }

    /**
     * @return virtual Hansoft SDK virtual session
     * @throws HPMSdkException
     * @throws HPMSdkJavaException
     */
    public static HPMSdkSession getVirtualSession() throws HPMSdkException, HPMSdkJavaException {
        return HPMSdkSession.SessionOpenVirtual(getMainSession());
    }

    // HttpSessionListener methods

    @Override
    public void sessionCreated(HttpSessionEvent arg0) {
        // Sessions get added to the HansoftConnector list when authenticated in
        // CredentialsFilter
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent arg0) {
        // Default timeout for a Jetty session is 30 min - after this we will
        // get timeout. Release the corresponding HansoftConnector - the
        // virtual session will be GC'd
        HansoftConnector.release(arg0.getSession());
    }

    // ServletContextListener methods

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // No need to de-register - catalog will go away with the web app
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final ServletContext servletContext = servletContextEvent
                .getServletContext();

        init(servletContext);

        // We are getting the adapter server name, port and scheme from the
        // adapter.properties file (8080 and 8080 are defaults if empty).
        // Alternative is to lazily get from the http request in first call,
        // but not given that those should be used. This way more explicit, but
        // less convenient - as need to be configured.
        
        String host = "";
        if (hansoftAdapterServer == null || hansoftAdapterServer.isEmpty()) {
            try {
                host = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (final UnknownHostException exception) {
                host = "localhost";
            }
        } else {
            host = hansoftAdapterServer;
        }
        logger.info("Hansoft Adapter Server: " + host);

        String path = hansoftAdapterServerScheme + "://" + host;
        path += (hansoftAdapterServerPort.isEmpty() ? "" : ":"
                + hansoftAdapterServerPort);
        path += PROVIDER_CONTEXT_PATH;

        servletBase = path;
        serviceBase = path + PROVIDER_SERVICE_PATH;
        System.setProperty(SYSTEM_PROPERTY_NAME_REGISTRY_URI, serviceBase + "/"
                + CATALOG_PATH_SEGMENT);
        
        logger.info("Hansoft Adapter Servlet base: " + servletBase);
        logger.info("Hansoft Adapter Service base: " + serviceBase);
        logger.info("Initialize of Hansoft adapter done.");
    }
    
    /**
     * @param httpServletRequest
     * @return prefix
     */
    public static String getPrefix(HttpServletRequest httpServletRequest) {
    	if (httpServletRequest == null) {
    		return "";
    	}
    	// check if IP is in list of known FocalPoints
    	String ip = httpServletRequest.getRemoteAddr();
    	if (ip == null) {
    		ip = "";
    	}
    	
    	if (ip != null && !ip.trim().equals("")) {
    		if (hansoftFPs.contains(ip)) {
    			return Constants.FP_FIX_PREFIX;
    		}
    	}

    	// check if hostname is in list of known FocalPoints
    	try {
    		String hostname = InetAddress.getByName(ip).getHostName();
    		if (hostname != null && !hostname.trim().equals("")) {
        		if (hansoftFPs.contains(hostname)) {
        			return Constants.FP_FIX_PREFIX;
        		}    			
    		}
		} catch (UnknownHostException e) {
			logger.error("Unknown host name: " + e.getMessage());
		}
    	return "";
    }
    
    /**
     * @return adapterServletHome
     */
    public static String getAdapterServletHome() {
        return adapterServletHome;
    }

	/**
	 * @return hansoftChangeLogDir
	 */
	public static String getChangeLogDir() {
        return hansoftChangeLogDir;
    }
	
	/**
	 * @return hansoftLogDir
	 */
	public static String getHansoftLogDir() {
        return hansoftLogDir;
    }
	
    /**
     * @return servletBase
     */
    public static String getServletBase() {
        return servletBase;
    }

    /**
     * @return serviceBase
     */
    public static String getServiceBase() {
        return serviceBase;
    }
    
    /**
     * @return port where hansoft adapter is running
     */
    public static String getHansoftAdapterServerPort() {
    	return hansoftAdapterServerPort;    	
    }
}
