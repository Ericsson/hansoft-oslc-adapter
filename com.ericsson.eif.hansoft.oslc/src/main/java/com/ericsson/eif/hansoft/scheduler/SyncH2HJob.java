package com.ericsson.eif.hansoft.scheduler;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import se.hansoft.hpmsdk.HPMSdkSession;
import se.hansoft.hpmsdk.HPMTaskEnum;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.configuration.ConfigurationControllerImpl;
import com.ericsson.eif.hansoft.integration.IntegrationController;
import com.ericsson.eif.hansoft.integration.RulesController;
import com.ericsson.eif.hansoft.utils.HansoftUtils;

public class SyncH2HJob implements org.quartz.Job {

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context) 
			throws JobExecutionException {
		
		JobKey jobKey = context.getJobDetail().getKey();
		System.out.println("SyncH2HJob says: " + jobKey + " executing at " + new Date());
		
		JobDataMap map = context.getJobDetail().getJobDataMap();
		String syncFromProjectName = map.getString("SyncFromProjectName");
		String syncFromProjectId = map.getString("SyncFromProjectId");
		String syncToProjectName = map.getString("SyncToProjectName");
		String syncToId = map.getString("SyncToId");
		
		// find Project in config matching our projectId
		Project syncFromProject = null;
		for (Project project : ConfigurationControllerImpl.getInstance().getProjects()) {
			if (project.getId().equalsIgnoreCase(syncFromProjectId)) {
				syncFromProject = project;
				break;
			}
		}
		
		// in syncCandidates we will keep sync objects where we want to synchronize
		// they will be filter by sync rules
		List<Sync> syncCandidates = new ArrayList<Sync>();
		
		// find Sync object for our SyncId.
		for (Project project : ConfigurationControllerImpl.getInstance().getProjects()) {
			if (project.getId().equalsIgnoreCase(syncFromProjectId)) {
				for (Sync sync : project.getSyncList()) {
					if (sync.getId().equalsIgnoreCase(syncToId)) {
						syncCandidates.add(sync);
						break;
					}
				}
			}
		}
		
		try {
			// get all tasks in project from which we want to sync
			HPMUniqueID projectId = HansoftUtils.getProjectIdFromProjectName(syncFromProjectName);
			HPMSdkSession session = HansoftManager.getMainSession();
			HPMUniqueID backlogId = session.ProjectUtilGetBacklog(projectId);
			HPMTaskEnum backlogTasks = session.TaskEnum(backlogId);

			// prepare needed information to be able to sync task			
			String projectUserName = ConfigurationControllerImpl.getInstance().getProjectUserName(syncFromProjectName);
			HansoftConnector hc = HansoftConnector.getHSConnectorForUser(projectUserName);
			HPMUniqueID realProjectId = HansoftUtils.getRealProjectIdOfProjectId(projectId);
			
			// Apply sync rules and synchronize tasks from one project to another			
			RulesController rulesController = new RulesController();
			IntegrationController ic = new IntegrationController();
			for (HPMUniqueID taskId : backlogTasks.m_Tasks) {				
				List<Sync> selectedSyncs = rulesController.filterSyncs(taskId, realProjectId, syncFromProjectName, hc, syncCandidates, true);
				if (!selectedSyncs.isEmpty()) {
					System.out.println("JOB is synchronizing task: " + taskId + " from: " + syncFromProjectName + " to: " + syncToProjectName);				
					ic.syncTask(taskId, projectId, syncFromProject, "SyncH2HJob was not able to identify changed column name", hc, selectedSyncs);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
