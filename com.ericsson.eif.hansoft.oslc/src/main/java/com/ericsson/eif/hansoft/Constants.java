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
 *     Michael Fiedler      - Bugzilla adpater implementations
 *******************************************************************************/
package com.ericsson.eif.hansoft;

import org.eclipse.lyo.oslc4j.core.model.OslcConstants;

public interface Constants {
    public static String CHANGE_MANAGEMENT_DOMAIN = "http://open-services.net/ns/cm#";
    public static String CHANGE_MANAGEMENT_NAMESPACE = "http://open-services.net/ns/cm#";
    public static String CHANGE_MANAGEMENT_NAMESPACE_PREFIX = "oslc_cm";
    public static String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static String FOAF_NAMESPACE_PREFIX = "foaf";
    public static String QUALITY_MANAGEMENT_NAMESPACE = "http://open-services.net/ns/qm#";
    public static String QUALITY_MANAGEMENT_PREFIX = "oslc_qm";
    public static String REQUIREMENTS_MANAGEMENT_NAMESPACE = "http://open-services.net/ns/rm#";
    public static String REQUIREMENTS_MANAGEMENT_PREFIX = "oslc_rm";
    public static String SOFTWARE_CONFIGURATION_MANAGEMENT_NAMESPACE = "http://open-services.net/ns/scm#";
    public static String SOFTWARE_CONFIGURATION_MANAGEMENT_PREFIX = "oslc_scm";
    
    public static final String DCTERMS_NAMESPACE = "http://purl.org/dc/terms/";

    public static String HANSOFT_DOMAIN = "http://www.hansoft.com/rdf#";
    public static String HANSOFT_NAMESPACE = "http://www.hansoft.com/rdf#";
    public static String HANSOFT_NAMESPACE_PREFIX = "hs";

    // For oslc_cm, they have defined oslc_cmx prefix mapped to
    // "http://open-services.net/ns/cm-x#"
    // For rtc, they have defined rtc_ext mapped to
    // "http://jazz.net/xmlns/prod/jazz/rtc/ext/1.0/" where
    // rtc_cm is mapped to http://jazz.net/xmlns/prod/jazz/rtc/cm/1.0/. So
    // following this pattern kind of:

    public static String HANSOFT_NAMESPACE_EXT = "http://www.hansoft.com/ns/ext#";
    public static String HANSOFT_NAMESPACE_PREFIX_EXT = "hs_ext";

    public static String CHANGE_REQUEST = "ChangeRequest";
    public static String TYPE_CHANGE_REQUEST = CHANGE_MANAGEMENT_NAMESPACE
            + "ChangeRequest";
    public static String TYPE_CHANGE_SET = SOFTWARE_CONFIGURATION_MANAGEMENT_NAMESPACE
            + "ChangeSet";
    public static String TYPE_DISCUSSION = OslcConstants.OSLC_CORE_NAMESPACE
            + "Discussion";
    public static String TYPE_PERSON = FOAF_NAMESPACE + "Person";
    public static String TYPE_REQUIREMENT = REQUIREMENTS_MANAGEMENT_NAMESPACE
            + "Requirement";
    public static String TYPE_TEST_CASE = QUALITY_MANAGEMENT_NAMESPACE
            + "TestCase";
    public static String TYPE_TEST_EXECUTION_RECORD = QUALITY_MANAGEMENT_NAMESPACE
            + "TestExecutionRecord";
    public static String TYPE_TEST_PLAN = QUALITY_MANAGEMENT_NAMESPACE
            + "TestPlan";
    public static String TYPE_TEST_RESULT = QUALITY_MANAGEMENT_NAMESPACE
            + "TestResult";
    public static String TYPE_TEST_SCRIPT = QUALITY_MANAGEMENT_NAMESPACE
            + "TestScript";

    public static String PATH_CHANGE_REQUEST = "changeRequest";

    public static String USAGE_LIST = CHANGE_MANAGEMENT_NAMESPACE + "list";

    public static final String HDR_OSLC_VERSION = "OSLC-Core-Version";
    public static final String OSLC_VERSION_V2 = "2.0";
    
    public static final String NEXT_PAGE = "com.ericsson.eif.hansoft.NextPage";
    
    public static final String FP_FIX_PREFIX = "__FP_FIX__";
    
    public static final String LOCATION_HEADER_FIELD = "Location";
    
    public static final String LOCATION_PREFIX = "Location:";
    
    public static final String LS = "\n";

    public static String SYNC_TO_SEPARATOR_START = ".(";
    public static String SYNC_TO_SEPARATOR_END = ").";
    public static String SYNC_TO_SEPARATOR_SEPARATOR = ", ";

    public static final String OK = "OK";
    public static final String FAILED = "FAILED";
    // text FAILED in red color which is displayed in backLink column in case of some synchronization problem
 	public static String BACKLINK_SYNCHRONIZATION_STATUS_FAILED = "<COLOR=255,7,0>FAILED</COLOR>";
    public static String BACKLINK_URL_TAG_START = "<URL=";
    public static String BACKLINK_URL_END = ">";
    public static final String BACKLINK_URL_TAG_END = "</URL>";
    public static final String SELECTED_SYNC_URL_PARAMETER = "connectionId=";
 	
    public static String INTEGRATION_TYPE = "Integration-type";
    public static String H2H = "H2H";
    public static String H2H_USER = "H2H";
    public static String SYNC_FROM_LABEL = "SyncFromLabel";
    public static String SYNC_FROM_PROJECT_ID = "SyncFromProjectID";
    public static String LEAN_SYNC_HEADER_FIELD = "LeanSync";
    public static String ETAG = "ETag";
    public static final String COLUMN_ITEM_NAME = DCTERMS_NAMESPACE + "Item name";
    public static final String DCTERMS_TITLE   = DCTERMS_NAMESPACE + "title";
    public static final String COLUMN_DESCRIPTION = DCTERMS_NAMESPACE + "Description";
    public static final String DCTERMS_IDENTIFIER = DCTERMS_NAMESPACE + "identifier";
    public static final String DCTERMS_DUEDATE = DCTERMS_NAMESPACE + "dueDate";
    public static final String COLUMN_PRIORITY = CHANGE_MANAGEMENT_NAMESPACE + "Priority";
    public static final String COLUMN_SEVERITY = CHANGE_MANAGEMENT_NAMESPACE + "Severity";
    public static final String COLUMN_STATUS = CHANGE_MANAGEMENT_NAMESPACE + "Status";
    public static final String COLUMN_WORK_REMAINING = HANSOFT_NAMESPACE + "Work Remaining";
    
    public static String CREATE = "create";
    public static String UPDATE = "update";
    
    public static String LEAN_SYNC_CONFIG_FILE = "LeanSyncConfig.xml";
	public static String H2H_CONFIG_FILE = "H2HConfig.xml";
	
	
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String TIME_FORMAT = "HH:mm:ss";
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public static final String LAST_UPDATED_TIME = "LastUpdatedTime";
	
	public static final String SYNC_RULE_OPERATOR_EQUALS = "equals";
	public static final String SYNC_RULE_OPERATOR_NOT_EQUALS = "not equals";
	public static final String SYNC_RULE_OPERATOR_CONTAINS = "contains";
	public static final String SYNC_RULE_OPERATOR_NOT_CONTAINS = "not contains";
	public static final String SYNC_RULE_OPERATOR_STARTSWITH = "starts with";
	
	public static final String TRIGGER_MANUAL = "manual";
	public static final String TRIGGER_AUTOMATIC = "automatic";
}

