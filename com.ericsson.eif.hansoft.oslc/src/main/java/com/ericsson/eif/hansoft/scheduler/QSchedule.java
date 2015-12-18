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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.SchedulerTrigger;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.configuration.ConfigurationControllerImpl;


public class QSchedule {
	
	private static final QSchedule instance = new QSchedule();
	private static final Logger logger = Logger.getLogger(QSchedule.class.getName());
	private static Scheduler scheduler;

	/**
	 * @return instance of QSchedule
	 */
	public static QSchedule getInstance() {
		return instance;
	}
	
	/**
	 * @return instance of Scheduler
	 */
	public static Scheduler getInstanceScheduler() {
		return scheduler;
	}
	
	/**
	 * Constructor
	 */
	public QSchedule() {		
		try {			
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			// to prevent having lines QuartzSchedulerThread:276 - batch acquisition of 0 triggers in hansoft_oslc.log
			// start scheduler only if we have at least one scheduled job
			int numberOfScheduledJobs = fillSchedulerFromConfiguration(scheduler);			
			if (numberOfScheduledJobs > 0) {
				scheduler.start();
			}
		} catch (SchedulerException e) {
			logger.error("Scheduler error." + e.getMessage(), e);
		}
	}
	
	/**
	 * shutdown of scheduler
	 * @param scheduler
	 */
	public static void shutdown(Scheduler scheduler) {
		try {
			scheduler.shutdown();
		} catch (SchedulerException e) {
			logger.error("Scheduler error during shutdown." + e.getMessage(), e);
		}
	}
	
	/**
	 * update of scheduler - reread configuration from H2HConfig.xml
	 * @param scheduler
	 */
	public void update() {		
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.standby();
			scheduler.clear();  // get rid of all scheduled jobs, triggers...
			
			// to prevent having lines QuartzSchedulerThread:276 - batch acquisition of 0 triggers in hansoft_oslc.log
			// start scheduler only if we have at least one scheduled job
			int numberOfScheduledJobs = fillSchedulerFromConfiguration(scheduler);
			if (numberOfScheduledJobs > 0) {
				scheduler.start();				
			}
		} catch (SchedulerException e) {
			logger.error("Scheduler error during clear." + e.getMessage(), e);
		}
	}
	
	/**
	 * fill configuration for scheduler from H2HConfig.xml
	 * @param scheduler
	 */
	private int fillSchedulerFromConfiguration(Scheduler scheduler) {
		int numberOfScheduledJobs = 0;
		
		// fill up triggers from configuration
		for (Project project : ConfigurationControllerImpl.getInstance().getProjects()) {
			for (Sync sync : project.getSyncList()) {
				
				// if scheduler is not activated then skip
				if (sync.getScheduler() == null || !sync.getScheduler().isActive()) 
					continue;
				
				for (SchedulerTrigger schedulerTrigger : sync.getScheduler().getTriggers()) {	
				
					// if trigger is not activated then skip
					if (!schedulerTrigger.isActive()) 
						continue;
											
					// define the job and tie it to our SyncH2HJob class
					JobDetail job = newJob(SyncH2HJob.class)
					    .withIdentity(sync.getProjectName() + " " + schedulerTrigger.getCronScheduleString(), project.getProjectName())
					    .usingJobData("SyncFromProjectName", project.getProjectName())
					    .usingJobData("SyncFromProjectId", project.getId())
					    .usingJobData("SyncToId", sync.getId())
					    .usingJobData("SyncToProjectName", sync.getProjectName())
					    .build();
					
					CronTrigger trigger = newTrigger()
						    .withIdentity("Cron_trigger_" + sync.getId() + "_" + schedulerTrigger.getCronScheduleString(), project.getProjectName())
						    .withSchedule(cronSchedule(schedulerTrigger.getCronScheduleString()))							    
						    .build();
					
					// tell quartz to schedule the job using our trigger
					try {
						scheduler.scheduleJob(job, trigger);					
						numberOfScheduledJobs++;
					} catch (SchedulerException e) {
						logger.error("Scheduler error during scheduling job using trigger." + e.getMessage(), e);
					}
				}
			}
		}	
		
		return numberOfScheduledJobs;
	}	
}
