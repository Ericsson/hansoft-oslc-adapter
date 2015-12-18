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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.ericsson.eif.leansync.mapping.SyncConfigLoader;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;

public class LeanSyncConfig {
	
	private static Map<Object, SyncConfiguration> configurationMap; 

	/**
	 * Reads configuration from file
	 * @param configPath
	 * @throws Exception
	 */
	public static void getConfigFromFile(String configPath) throws Exception {
		
        String xmlFromFile = new String();
        BufferedReader reader = Files.newBufferedReader(Paths.get(configPath), Charset.defaultCharset());
        String line = null;
        while ((line = reader.readLine()) != null) {
            xmlFromFile = xmlFromFile + line;
        }
		
		SyncConfigLoader syncConfigLoader = new SyncConfigLoader();
		configurationMap = syncConfigLoader.loadConfiguration(xmlFromFile);
	}

	/**
	 * @return configuration map
	 */
	public static Map<Object, SyncConfiguration> getConfigurationMap() {
		return configurationMap;
	}

	/**
	 * @param configurationMap
	 */
	public static void setConfigurationMap(Map<Object, SyncConfiguration> configurationMap) {
		LeanSyncConfig.configurationMap = configurationMap;
	}
}
