package com.ericsson.eif.hansoft;

/*
* Copyright (C) 2015 Ericsson AB. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer
*    in the documentation and/or other materials provided with the
*    distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import org.apache.commons.lang3.StringUtils;

import com.ericsson.eif.hansoft.resources.Priority;
import com.ericsson.eif.hansoft.resources.Severity;

import se.hansoft.hpmsdk.EHPMTaskAgilePriorityCategory;
import se.hansoft.hpmsdk.EHPMTaskSeverity;
import se.hansoft.hpmsdk.EHPMTaskStatus;

public class HansoftOSLCMapper {

    /**
     * See
     * http://open-services.net/wiki/change-management/Specification-3.0/#Resource_Priority
     * 
     * EHPMTaskAgilePriorityCategory_NewVersionOfSDKRequired = 0,
     * EHPMTaskAgilePriorityCategory_None = 1,
     * EHPMTaskAgilePriorityCategory_VeryLow = 2,
     * EHPMTaskAgilePriorityCategory_Low = 3,
     * EHPMTaskAgilePriorityCategory_Medium = 4,
     * EHPMTaskAgilePriorityCategory_High = 5,
     * EHPMTaskAgilePriorityCategory_VeryHigh = 6,
     *  
     * get Hansoft priority
     * @param priority
     */
    public static EHPMTaskAgilePriorityCategory getHsPriority(String priority) {
        EHPMTaskAgilePriorityCategory defValue = EHPMTaskAgilePriorityCategory.None;

        if (StringUtils.isEmpty(priority))
            return defValue;

        if (priority.equals(Priority.PriorityUnassigned.toString()))
            return EHPMTaskAgilePriorityCategory.None;
        if (priority.equals(Priority.High.toString()))
            return EHPMTaskAgilePriorityCategory.High;
        if (priority.equals(Priority.Medium.toString()))
            return EHPMTaskAgilePriorityCategory.Medium;
        if (priority.equals(Priority.Low.toString()))
            return EHPMTaskAgilePriorityCategory.Low;

        return defValue;
    }

    /**
     * gets OSLC priority
     * @param priority
     * @return OSLCPriority
     */
    public static String getOSLCPriority(EHPMTaskAgilePriorityCategory priority) {
        if (priority == null)
            return null;

        if (priority.equals(EHPMTaskAgilePriorityCategory.VeryHigh)
                || priority.equals(EHPMTaskAgilePriorityCategory.High))
            return Priority.High.toString();
        if (priority.equals(EHPMTaskAgilePriorityCategory.Medium))
            return Priority.Medium.toString();
        if (priority.equals(EHPMTaskAgilePriorityCategory.VeryLow)
                || priority.equals(EHPMTaskAgilePriorityCategory.Low))
            return Priority.Low.toString();
        if (priority.equals(EHPMTaskAgilePriorityCategory.None))
            return Priority.PriorityUnassigned.toString();

        return null;
    }

    /**
     * See
     * http://open-services.net/wiki/change-management/Specification-3.0/#Resource_Severity
     * 
     * EHPMTaskSeverity_NewVersionOfSDKRequired = 0,
     * EHPMTaskSeverity_None = 1,
     * EHPMTaskSeverity_A = 2,
     * EHPMTaskSeverity_B = 3,
     * EHPMTaskSeverity_C = 4,
     * EHPMTaskSeverity_D = 5,
	 *
     * @param severity
     * @return HS task severity
     */
    public static EHPMTaskSeverity getHsSeverity(String severity) {
        EHPMTaskSeverity defValue = EHPMTaskSeverity.None;

        if (StringUtils.isEmpty(severity))
            return defValue;

        if (severity.equals(Severity.Critical))
            return EHPMTaskSeverity.A;
        if (severity.equals(Severity.Major))
            return EHPMTaskSeverity.B;
        if (severity.equals(Severity.Normal))
            return EHPMTaskSeverity.C;
        if (severity.equals(Severity.Minor))
            return EHPMTaskSeverity.D;
        if (severity.equals(Severity.Unclassified))
            return EHPMTaskSeverity.None;

        return defValue;
    }

    /**
     * @param severity
     * @return OSLCSeverity
     */
    public static String getOSLCSeverity(EHPMTaskSeverity severity) {
        if (severity == null)
            return null;

        if (severity.equals(EHPMTaskSeverity.A))
            return Severity.Critical.toString();
        if (severity.equals(EHPMTaskSeverity.B))
            return Severity.Major.toString();
        if (severity.equals(EHPMTaskSeverity.C))
            return Severity.Normal.toString();
        if (severity.equals(EHPMTaskSeverity.D))
            return Severity.Minor.toString();
        if (severity.equals(EHPMTaskSeverity.None))
            return Severity.Unclassified.toString();

        return null;
    }

    // See
    // http://open-services.net/bin/view/Main/CmSpecificationV2?sortcol=table;up=#Resource_ChangeRequest
    //
    // Values for StatePredicates
    //
    // oslc_cm:closed Whether or not the Change Request is completely done, no
    // further fixes or fix
    // verification is needed.
    // oslc_cm:inprogress Whether or not the Change Request in a state
    // indicating that active work is
    // occurring. If oslc_cm:inprogress is true, then oslc_cm:fixed and
    // oslc_cm:closed
    // must also be false
    // oslc_cm:fixed Whether or not the Change Request has been fixed.
    // oslc_cm:approved Whether or not the Change Request has been approved.
    // oslc_cm:reviewed Whether or not the Change Request has been reviewed.
    // oslc_cm:verified Whether or not the resolution or fix of the Change
    // Request has been verified.

    // The list of possible state values should be specified by the
    // ResourceShape in OSLC CM v2, but
    // no common agreed model, hence not possible to generally translate. So for
    // this implementation assume
    // working as v3 of spec, see
    // http://open-services.net/wiki/change-management/Specification-3.0/#Resource_State
    // which by coincidence is same as table above.

    public static final String OSLC_CM_STATE_NAME_CLOSED = "Closed";
    public static final String OSLC_CM_STATE_NAME_INPROGRESS = "In-progress";
    public static final String OSLC_CM_STATE_NAME_FIXED = "Fixed";
    public static final String OSLC_CM_STATE_NAME_APPROVED = "Approved";
    public static final String OSLC_CM_STATE_NAME_REVIEWED = "Reviewed";
    public static final String OSLC_CM_STATE_NAME_VERIFIED = "Verified";

    public static final String HANSOFT_STATE_NAME_NOTDONE = "Not Done";
    public static final String HANSOFT_STATE_NAME_NOSTATUS = "No Status";
    public static final String HANSOFT_STATE_NAME_BLOCKED = "Blocked";
    public static final String HANSOFT_STATE_NAME_DELETED = "Deleted";

    
    /**
     * Return the status String corresponding to the EHPMTaskStatus or null
     * 
     * Valid states from the Hansoft SDK
     *
     * EHPMTaskStatus_NewVersionOfSDKRequired = 0,
     * EHPMTaskStatus_NoStatus = 1,
     * EHPMTaskStatus_NotDone = 2,
     * EHPMTaskStatus_InProgress = 3,
     * EHPMTaskStatus_Completed = 4,
     * EHPMTaskStatus_Blocked = 5,
     * EHPMTaskStatus_Deleted = 6,
	 *
     * @param status
     */
    public static String getOSLCStatus(EHPMTaskStatus status) {
        if (status == null)
            return null;

        if (status.equals(EHPMTaskStatus.InProgress))
            return OSLC_CM_STATE_NAME_INPROGRESS;
        if (status.equals(EHPMTaskStatus.Completed))
            return OSLC_CM_STATE_NAME_FIXED;
        if (status.equals(EHPMTaskStatus.NoStatus))
            return HANSOFT_STATE_NAME_NOSTATUS;
        if (status.equals(EHPMTaskStatus.NotDone))
            return HANSOFT_STATE_NAME_NOTDONE;
        if (status.equals(EHPMTaskStatus.Blocked))
            return HANSOFT_STATE_NAME_BLOCKED;
        if (status.equals(EHPMTaskStatus.Deleted))
            return HANSOFT_STATE_NAME_DELETED;

        return null;
    }

    /**
     * Return the status EHPMTaskStatus corresponding to the String or null
     * 
     * @param status
     * @return task status
     */
    public static EHPMTaskStatus getHsStatus(String status) {
        EHPMTaskStatus defValue = EHPMTaskStatus.NoStatus;

        if (StringUtils.isEmpty(status))
            return defValue;

        if (status.equals(OSLC_CM_STATE_NAME_INPROGRESS))
            return EHPMTaskStatus.InProgress;
        if (status.equals(OSLC_CM_STATE_NAME_FIXED))
            return EHPMTaskStatus.Completed;
        if (status.equals(HANSOFT_STATE_NAME_NOSTATUS))
            return EHPMTaskStatus.NoStatus;
        if (status.equals(HANSOFT_STATE_NAME_NOTDONE))
            return EHPMTaskStatus.NotDone;
        if (status.equals(HANSOFT_STATE_NAME_BLOCKED))
            return EHPMTaskStatus.Blocked;
        if (status.equals(HANSOFT_STATE_NAME_DELETED))
            return EHPMTaskStatus.Deleted;

        return defValue;
    }
}
