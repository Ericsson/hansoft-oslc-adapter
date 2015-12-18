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

import java.util.ArrayList;
import java.util.List;

import com.ericsson.eif.hansoft.utils.StringUtils;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Created by matejlajcak on 26/05/15.
 */
@XStreamAlias("sync")
public class Sync {

	private String id;
    private String friendName;
    private HansoftActorType hansoftActorType;
    private String server;
    private String database;
    private String projectName;
    private String parentTaskid;
    private String creationFactoryURL;
    private Boolean keepInSync;
    @XStreamAlias("scheduling")
    private Scheduler scheduler;
    private String trigger;

    @XStreamAlias("mappings")
    private List<MappingPair> mappingPairList = new ArrayList<MappingPair>();
    
    @XStreamAlias("syncRules")
    private List<SyncRule> syncRules = new ArrayList<SyncRule>();

    /**
     * Default constructor
     */
    public Sync() {    	
    }

    /**
     * @return identificator of Sync
     */
    public String getId() {
        return this.id;
    }

    /**
     * sets identificator of Sync
     * @param id
     */
    public void setId(String id) {
        if (StringUtils.isNotEmpty(id))
        	id = id.trim();
        
    	this.id = id;
    }
    
    /**
     * @return friend name
     */
    public String getFriendName() {
        return this.friendName;
    }

    /**
     * sets friendName
     * @param friendName
     */
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }
    
    /**
     * @return hansoftActorType
     */
    public HansoftActorType getHansoftActorType() {
        return hansoftActorType;
    }

    /**
     * sets hansoftActorType
     * @param hansoftActorType
     */
    public void setHansoftActorType(HansoftActorType hansoftActorType) {
        this.hansoftActorType = hansoftActorType;
    }
    
    /**
     * @return server
     */
    public String getServer() {
        return server;
    }

    /**
     * sets server
     * @param server
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * sets database
     * @param database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * sets projectName
     * @param projectName
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return creationFactoryURL
     */
    public String getCreationFactoryURL() {
        return creationFactoryURL;
    }

    /**
     * sets creationFactoryURL
     * @param creationFactoryURL
     */
    public void setCreationFactoryURL(String creationFactoryURL) {
        this.creationFactoryURL = creationFactoryURL;
    }
    
    /**
     * @return synchronization label
     * label is in format .(daatabase, projectName, server). 
     * e.g .(OSLC_Adapter_Test, xpromarProject, <Hansoft server address>). 
     */
    public String getLabel() {
    	return Constants.SYNC_TO_SEPARATOR_START + database + Constants.SYNC_TO_SEPARATOR_SEPARATOR + projectName + Constants.SYNC_TO_SEPARATOR_SEPARATOR + server + Constants.SYNC_TO_SEPARATOR_END;
    }

	/**
	 * @return parentTaskid
	 */
	public String getParentTaskid() {
		return parentTaskid;
	}

	/**
	 * @param parentTaskid
	 */
	public void setParentTaskid(String parentTaskid) {
		this.parentTaskid = parentTaskid;
	}
	
	/**
	 * @return mappingPairs for POST requests
	 */
	public List<MappingPair> getMappingPairListPost() {
		List<MappingPair> mappingPairPost = new ArrayList<MappingPair>();
		for (MappingPair mappingPair : mappingPairList) {
			if (StringUtils.isEmpty(mappingPair.getUse()) || mappingPair.getUse().equalsIgnoreCase(Constants.CREATE)) {
				mappingPairPost.add(mappingPair);
			}
		}
		return mappingPairPost;
	}
	
	/**
	 * @return mappingPairs for PUT requests
	 */
	public List<MappingPair> getMappingPairListPut() {
		List<MappingPair> mappingPairPut = new ArrayList<MappingPair>();
		for (MappingPair mappingPair : mappingPairList) {
			if (StringUtils.isEmpty(mappingPair.getUse()) || mappingPair.getUse().equalsIgnoreCase(Constants.UPDATE)) {
				mappingPairPut.add(mappingPair);
			}
		}
		return mappingPairPut;
	}

	/**
	 * @return mappingPairList
	 */
	public List<MappingPair> getMappingPairList() {
		return mappingPairList;
	}

	/**
	 * @param mappingPairList
	 */
	public void setMappingPairList(List<MappingPair> mappingPairList) {
		this.mappingPairList = mappingPairList;
	}

	/**
	 * @return syncRules
	 */
	public List<SyncRule> getSyncRules() {
		return syncRules;
	}

	/**
	 * @param syncRules
	 */
	public void setSyncRules(List<SyncRule> syncRules) {
		this.syncRules = syncRules;
	}
	
	/**
	 * @return true if keepInSync is set to true, otherwise false
	 * value true means that all synchronization rules are skipped
	 */
	public Boolean isKeptInSync() {
		if (this.keepInSync == null)
			return false;
		
		return this.keepInSync;
	}

	/**
	 * sets keepInSync value
	 * @param value
	 */
	public void setKeepInSync(Boolean value) {
		this.keepInSync = value;
	}

	/**
	 * @return scheduler
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * @param scheduler
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}	
	 
	/**
	 * @return true if sync has active scheduler and at least one trigger
	 * otherwise returns false
	 */
	public boolean hasActiveScheduler() {
		if (this.getScheduler() != null && this.getScheduler().isActive()) {
			for (SchedulerTrigger trigger : this.getScheduler().getTriggers()) {
				if (trigger.isActive() == true) 
					return true;
			}
		}
		return false;
	}
}

