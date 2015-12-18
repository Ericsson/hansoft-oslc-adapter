package com.ericsson.eif.hansoft.integration.logging;

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

/**
 * Class representing one item in change log
 * @author lukas svacina
 */
public class ChangeLogItem {
	
	public String fromProjectName;
	public String toProjectName;
    public String taskId;
    public String taskName;
    public String username;
    public String modifiedDate;
    public String modifiedTime;
    public String columnName;    
    public String oldValue;    
    public String newValue;
    public ConnectionType connectionType;
    public SynchronisationStatus synchronisationStatus;
    
    /**
     * Default constructor
     */
    public ChangeLogItem() {    	
    }
        
    /**
     * Constructor
     * 
     * @param projectName
     * @param taskName
     * @param taskId 
     * @param username
     * @param modifiedDate
     * @param modifiedTime
     * @param columnName
     * @param oldValue
     * @param newValue
     * @param connectionType
     */
    public ChangeLogItem(String fromProjectName, String toProjectName, String taskName, String taskId, 
    			String username, String modifiedDate, String modifiedTime, String columnName, 
    			String oldValue, String newValue, ConnectionType connectionType) {
    	
    	this.fromProjectName = fromProjectName;
    	this.toProjectName = toProjectName;
    	this.taskName = taskName;
    	this.taskId = taskId;
    	this.username = username;
    	this.modifiedDate = modifiedDate;
    	this.modifiedTime = modifiedTime;
    	this.columnName = columnName;
    	this.oldValue = oldValue;
    	this.newValue = newValue;
    	this.connectionType = connectionType;
    	
    	// synchronisation is by default OK
    	this.synchronisationStatus = SynchronisationStatus.Ok; 
    }
}