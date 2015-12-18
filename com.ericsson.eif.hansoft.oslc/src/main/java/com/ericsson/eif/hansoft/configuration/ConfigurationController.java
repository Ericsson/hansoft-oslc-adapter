package com.ericsson.eif.hansoft.configuration;

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

import com.ericsson.eif.hansoft.MappingPair;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.exception.ConfigItemNotFound;

/**
 * Created by matejlajcak on 03/06/15.
 */
public interface ConfigurationController {
    public String getManualSyncColumnName(String projectName) throws ConfigItemNotFound;
    public String getAutoSyncColumnName(String projectName) throws ConfigItemNotFound;
    public String getProjectCreationFactoryURL(String currentProjectName, String sync) throws ConfigItemNotFound;
    public String getBacklinkColumnName(String projectName) throws ConfigItemNotFound;
    public String getErrorColumnName(String projectName) throws ConfigItemNotFound;
    public String getParentTaskId(String currentProject, String syncLabel) throws ConfigItemNotFound;
    public String getFriendName(String currentProject, String syncLabel) throws ConfigItemNotFound;
    public String getFriendUserName(String friendName) throws ConfigItemNotFound;
    public String getFriendPassword(String friendName) throws ConfigItemNotFound;
    public boolean isIntegrationColumn(String currentProjectName, String columnName);
    public List<MappingPair> getMappingPairPost(String currentProject, String syncLabel) throws ConfigItemNotFound;
    public List<MappingPair> getMappingPairPut(String currentProject, String syncLabel) throws ConfigItemNotFound;
    public List<MappingPair> getMappingPair(String currentProject, String syncLabel) throws ConfigItemNotFound;
    public boolean isProjectInConfig(String projectName);
    public String getProjectLabel(String projectName) throws ConfigItemNotFound;
    public List<Sync> getSyncsForProject(String projectName);
    public String getProjectUserName(String projectName) throws ConfigItemNotFound;
    public List<Project> getProjects();
    public Project getProjectByName(String projectName) throws ConfigItemNotFound;
}
