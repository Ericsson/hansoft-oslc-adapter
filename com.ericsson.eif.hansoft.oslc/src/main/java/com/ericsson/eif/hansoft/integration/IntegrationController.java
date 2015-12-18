package com.ericsson.eif.hansoft.integration;

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

import java.util.List;

import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.HPMSdkException;
import se.hansoft.hpmsdk.HPMSdkJavaException;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Sync;

public class IntegrationController {
	
	private static final Logger logger = Logger.getLogger(IntegrationController.class.getName());
	
	private static boolean isSynchronizing = false; 
	
	/**
	 * Set synchronizing flag 
	 * - used in reloading of H2HConfig service
	 * @param value
	 */
	public static synchronized void setSynchronizing(boolean value) {
		isSynchronizing = value;
	}
	
	/**
	 * Get synchronizing flag
	 * @return
	 */
	public static synchronized boolean isSynchronizing() {
		return isSynchronizing;
	}
	
	/**
	 * Finds out correct type of integration and executes it.
	 * @param taskId
	 * @param projectId
	 * @param columnName
	 * @param projectName 
	 * @param hc
	 * @param selectedSyncs
	 * @throws Exception
	 */
	public void syncTask(HPMUniqueID taskId, HPMUniqueID projectId, Project project, String columnName, 
				HansoftConnector hc, List<Sync> selectedSyncs) {
		
		for (Sync selectedSync : selectedSyncs) {			
			try {				
				HansoftIntegration hi = new HansoftIntegration();
				
				// set synchronizing flag to true
				// if someone will try to access reloadH2HConfiguration service				
				// synchronization will be still running 
				// and after finish, the reload of H2H configuration can be done
				IntegrationController.setSynchronizing(true);
				
				hi.syncTask(taskId, projectId, project, hc, selectedSync);
			} catch (Exception e) {
				String message = "Synchronization of column " + columnName + " to " + selectedSync.getLabel() + " failed. " + e.getMessage();
				logger.error(message, e);
				e.printStackTrace();
				
				// add error message to error column
				try {
					HansoftIntegration.addErrorMessageToTask(projectId, project.getProjectName(), taskId, message, hc);
				} catch (HPMSdkException | HPMSdkJavaException e1) {
					logger.error("Error adding error message to task " + taskId + " in project named " + project.getProjectName() + " projectId " + projectId, e1);
					e1.printStackTrace();
				}
		   } finally {
			    // set synchronizing flag to false
			    // and get access to reloadH2HConfiguration service
			    IntegrationController.setSynchronizing(false);
		   }
		}
	}
}