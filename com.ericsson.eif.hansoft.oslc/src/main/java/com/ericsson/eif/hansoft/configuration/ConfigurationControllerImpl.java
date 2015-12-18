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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ericsson.eif.hansoft.Friend;
import com.ericsson.eif.hansoft.H2HConfig;
import com.ericsson.eif.hansoft.MappingPair;
import com.ericsson.eif.hansoft.Project;
import com.ericsson.eif.hansoft.Sync;
import com.ericsson.eif.hansoft.exception.ConfigItemNotFound;
import com.ericsson.eif.hansoft.utils.StringUtils;

/**
 * Created by matejlajcak on 03/06/15.
 */
public class ConfigurationControllerImpl implements ConfigurationController {
	
	private static final ConfigurationControllerImpl INSTANCE = new ConfigurationControllerImpl();

    private static final Logger logger = Logger.getLogger(ConfigurationControllerImpl.class.getName());
    
    /**
     * private constructor
     */
    private ConfigurationControllerImpl() {
	}
	
	/**
	 * @return single instance of configuration controller
	 */
	public static ConfigurationControllerImpl getInstance() {
		return INSTANCE;
	}
	
    @Override
    public String getManualSyncColumnName(String projectName) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(projectName)) {
            	String manualTriggerColumnName = project.getManualTriggerColumnName();
            	if (StringUtils.isEmpty(manualTriggerColumnName)) {
            		break;
            	} 
            	else {
            		return manualTriggerColumnName;
            	}
            }
        }
        
        String message = "ManualSyncColumnName not found in H2H config file.";
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getAutoSyncColumnName(String projectName) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(projectName)) {
            	String autoSyncColumnName = project.getAutoSyncColumnName();
            	if (StringUtils.isEmpty(autoSyncColumnName)) {
            		break;
            	} 
            	else {
            		return autoSyncColumnName;
            	}            	
            }
        }
        
        String message = "AutoSyncColumnName not found in H2H config file.";
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getProjectCreationFactoryURL(String currentProjectName, String syncLabel) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProjectName)) {
                for (Sync sync : project.getSyncList()) {
                    if (sync.getLabel().contentEquals(syncLabel)) {                        
                    	String creationFactoryURL = sync.getCreationFactoryURL();
                    	if (StringUtils.isEmpty(creationFactoryURL)) {
                    		break;
                    	} 
                    	else {
                    		return creationFactoryURL;
                    	}                    	
                    }
                }
            }
        }
        
        String message = "ProjectCreationFactoryURL not found in H2H config file for project: "+currentProjectName + " sync " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getBacklinkColumnName(String projectName) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(projectName)) {            	
            	String backlinkColumnName = project.getBacklinkColumnName();
            	if (StringUtils.isEmpty(backlinkColumnName)) {
            		break;
            	}
            	else {
            		return backlinkColumnName;
            	}
            }
        }
        
        String message = "BackLinkColumnName not found in H2H config file for project: " + projectName;
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getErrorColumnName(String projectName) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
             if (project.getProjectName().contentEquals(projectName)) {
            	String errorColumnName = project.getErrorColumnName();
             	if (StringUtils.isEmpty(errorColumnName)) {
             		break;
             	}
             	else {
             		return errorColumnName;
             	}            	
             }
        }
        
        String message = "ErrorColumnName not found in H2H config file for project: " + projectName;
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }
    
    @Override
    public String getFriendName(String currentProject, String syncLabel) throws ConfigItemNotFound {
    	for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProject)) {
                for (Sync sync : project.getSyncList()) {
                	if (sync.getLabel().contentEquals(syncLabel)) {
                		String friendName = sync.getFriendName();
                     	if (StringUtils.isEmpty(friendName)) {
                     		break;
                     	}
                     	else {
                     		return friendName;
                     	}
                    }
                }
            }
        }
    	
        String message = "FriendName not found in H2H config file " + " in project " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getFriendUserName(String friendName) throws ConfigItemNotFound {
        for (Friend friend : H2HConfig.getInstance().getFriendsList()) {
            if (friend.getFriendName().contentEquals(friendName)) {            	
            	String friendUserName = friend.getUsername();
             	if (StringUtils.isEmpty(friendUserName)) {
             		break;
             	}
             	else {
             		return friendUserName;
             	}
            }
        }
        
        String message = "Friend username (local Hansoft username) not found in H2H config file for friend name";
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

    @Override
    public String getFriendPassword(String friendName) throws ConfigItemNotFound {
        for (Friend friend : H2HConfig.getInstance().getFriendsList()) {
            if (friend.getFriendName().contentEquals(friendName)) {
            	String friendPassword = friend.getPassword();
             	if (StringUtils.isEmpty(friendPassword)) {
             		break;
             	}
             	else {
             		return friendPassword;
             	}
            }
        }
        
        String message = "Friend password (local Hansoft password) not found in H2H config file for friend name";
        logger.error(message);
        throw new ConfigItemNotFound(message);
    }

	@Override
	public boolean isIntegrationColumn(String currentProjectName, String columnName) {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProjectName)) {
            	if (project.getIntegrationColumnNames().contains(columnName)) {
            		return true;
            	}
            }
        }
		
		return false;
	}

	@Override
	public String getParentTaskId(String currentProjectName,String syncLabel) throws ConfigItemNotFound {
		
		for(Project project : H2HConfig.getInstance().getProjectList()) {
            if(project.getProjectName().contentEquals(currentProjectName)) {
                for(Sync sync : project.getSyncList()) {
                    if (sync.getLabel().contentEquals(syncLabel)) {                    	
                        return sync.getParentTaskid();
                    }
                }
            }
        }
		
		String message = "parent task not found in H2H config file in " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}

	@Override
	public List<MappingPair> getMappingPairPost(String currentProject,
			String syncLabel) throws ConfigItemNotFound {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProject)) {
                for (Sync sync : project.getSyncList()) {
                	if (sync.getLabel().contentEquals(syncLabel)) {
                		return sync.getMappingPairListPost();
                    }
                }
            }
        }
		
        String message = "MappingPair for POST not found in H2H config file " +
                " in project " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}
	
	@Override
	public List<MappingPair> getMappingPairPut(String currentProject,
			String syncLabel) throws ConfigItemNotFound {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProject)) {
                for (Sync sync : project.getSyncList()) {
                	if (sync.getLabel().contentEquals(syncLabel)) {
                		return sync.getMappingPairListPut();
                    }
                }
            }
        }
		
        String message = "MappingPair for PUT not found in H2H config file " +
                " in project " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}
	
	@Override
	public List<MappingPair> getMappingPair(String currentProject,
			String syncLabel) throws ConfigItemNotFound {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(currentProject)) {
                for (Sync sync : project.getSyncList()) {
                	if (sync.getLabel().contentEquals(syncLabel)) {
                		return sync.getMappingPairList();
                    }
                }
            }
        }
		
        String message = "MappingPair not found in H2H config file " +
                " in project " + syncLabel;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}

	@Override
	public boolean isProjectInConfig(String projectName) {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(projectName)) {
            	return true;
            }
        }
		return false;
	}

	@Override
	public String getProjectLabel(String projectName) throws ConfigItemNotFound {
        for (Project project : H2HConfig.getInstance().getProjectList()) {
            if (project.getProjectName().contentEquals(projectName)) {
            	String label = project.getLabel();
             	if (StringUtils.isEmpty(label)) {
             		break;
             	}
             	else {
             		return label;
             	}
            }
        }
        
        String message = "Failed to get project label for project " + projectName;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}
		
	public List<Sync> getSyncsForProject(String projectName) {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
			if (project.getProjectName().equals(projectName)) {
				return project.getSyncList();
			}
		}
		return new ArrayList<Sync>();
	}

	@Override
	public String getProjectUserName(String projectName) throws ConfigItemNotFound {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
			if (project.getProjectName().equals(projectName)) {
				String projectUserName = project.getProjectUserName();
             	if (StringUtils.isEmpty(projectUserName)) {
             		break;
             	}
             	else {
             		return projectUserName;
             	}
			}
		}
		
		String message = "Failed to get project user name for project " + projectName;
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}

	@Override
	public List<Project> getProjects() {
		return H2HConfig.getInstance().getProjectList();
	}
	
	@Override
	public Project getProjectByName(String projectName) throws ConfigItemNotFound {
		for (Project project : H2HConfig.getInstance().getProjectList()) {
			if (project.getProjectName().equalsIgnoreCase(projectName)) {
            	return project;
			}
		}
		
		String message = "Failed to get project named " + projectName + " in configuration";
        logger.error(message);
        throw new ConfigItemNotFound(message);
	}
}
