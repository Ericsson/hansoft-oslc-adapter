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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import se.hansoft.hpmsdk.EHPMProjectCustomColumnsColumnType;
import se.hansoft.hpmsdk.HPMProjectCustomColumnsColumn;
import se.hansoft.hpmsdk.HPMUniqueID;

import com.ericsson.eif.hansoft.HansoftConnector;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.SyncRule;
import com.ericsson.eif.hansoft.factories.HansoftChangeRequestFactory;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.utils.HansoftUtils;

public class RulesController {
	
	private static final Logger logger = Logger.getLogger(RulesController.class.getName());
	
	/**
	 * @param taskId
	 * @param projectId
	 * @param projectName
	 * @param hc
	 * @param syncCandidates
	 * @param calledFromScheduler
	 * @return list of Sync matching synchronization rules. Only to these Sync we can synchronize tasks
	 * @throws Exception
	 */
	public List<Sync> filterSyncs(HPMUniqueID taskId, HPMUniqueID projectId, String projectName, HansoftConnector hc, List<Sync> syncCandidates, boolean calledFromScheduler) throws Exception {

		List<Sync> selectedSyncs = new ArrayList<Sync>();
    	HansoftChangeRequest hansoftChangeRequest = HansoftChangeRequestFactory.getChangeRequestFromTaskBasic2(null, projectId.toString(), projectId, taskId, false, hc);
    	
		for (Sync candidate : syncCandidates) {
			boolean rulesMatched = false;
			try {
				rulesMatched = evaluateRules(candidate, hansoftChangeRequest, projectId, projectName, calledFromScheduler) || candidate.isKeptInSync();
			} catch (Exception e) {
				logger.error("Failed to evaluate rules of sync candidate " + candidate.getLabel());
				rulesMatched = false;
			}
			
			if (rulesMatched) {
				selectedSyncs.add(candidate);
			}
		}   	
    	
		return selectedSyncs;
	}

	/**
	 * @param sync
	 * @param hansoftChangeRequest
	 * @param projectId
	 * @param projectName
	 * @param calledFromScheduler
	 * @throws Exception
	 */
	private boolean evaluateRules(Sync sync, HansoftChangeRequest hansoftChangeRequest, HPMUniqueID projectId, String projectName, boolean calledFromScheduler) throws Exception {
		List<SyncRule> rules = sync.getSyncRules();
		if (rules == null || rules.isEmpty()) {
			return true;
		}	
					
		for (SyncRule rule : rules) {

			// if synchronization is called from scheduler,
			// do not evaluate rules which are ignored by scheduler
			if (calledFromScheduler) {
				if (rule.isIgnoredByScheduler()) {
					continue;
				}
			}
			
			// evaluate rules
			boolean match = false;
			String attributeName = rule.getAttribute();
			String attributeNS = rule.getAttributeNameSpace();
			String attributeValue = rule.getValue();
			Object realValue = HansoftUtils.getValueOfAttribute(hansoftChangeRequest, attributeName, attributeNS);			
									
			if (realValue == null || attributeValue == null) {
				return false;
			}
			
			HPMProjectCustomColumnsColumn customColumn = HansoftUtils.getHansoftCustomColumnByName(projectId, projectName, attributeName);
			
			// special handling for multiSelectDropDown menu
			if (customColumn != null && customColumn.m_Type == EHPMProjectCustomColumnsColumnType.MultiSelectionDropList) {
	    		String[] columnValues = ((String) realValue).split(";");
	    		for (String columnValue : columnValues) {
    				if (columnValue.equals(attributeValue)) {
    					match = true;
    					break;
	    			}				
				}
			} else {
				if (realValue.equals(attributeValue)) {
					match = true;
				}
			}
			
			if (!match) {
				return false;
			}
		}
		return true;
	}
}
