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

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.Credentials;
import com.ericsson.eif.hansoft.utils.StringUtils;

public class Poster {
	
	/**
	 * create resource
	 * @param url
	 * @param rdfData
	 * @param syncTo 
	 * @param linkToMother 
	 * @return 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws Exception 
	 */
	public HttpResponse postTask(String url, String rdfData, Credentials credentials, String syncLabel, String projectId) throws Exception {
		
		if (StringUtils.isEmpty(url)) {
			throw new Exception("Cannot POST task, url is empty");
		}

		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);

		httpPost.addHeader("OSLC-Core-Version", "2.0");
		httpPost.addHeader("Content-type", "application/rdf+xml");
		httpPost.addHeader("Accept", "application/rdf+xml");
		httpPost.addHeader(Constants.INTEGRATION_TYPE, Constants.H2H);
		httpPost.addHeader(Constants.SYNC_FROM_LABEL, syncLabel);
		httpPost.addHeader(Constants.SYNC_FROM_PROJECT_ID, projectId);
		httpPost.addHeader(getAuthorizationHeader(credentials));
		httpPost.setEntity(new StringEntity(rdfData, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		return response;
	}
	
	/**
	 * update resource
	 * @param url
	 * @param rdfData
	 * @param eTag 
	 * @return 
	 */
	public HttpResponse putTask(String url, String rdfData, String eTag, Credentials credentials, String syncLabel, String projectId) throws Exception {

		if (StringUtils.isEmpty(url)) {
			throw new Exception("Cannot PUT task, url is empty");
		}

		HttpClient client = new DefaultHttpClient();
		HttpPut httpPut = new HttpPut(url);

		httpPut.addHeader("OSLC-Core-Version", "2.0");
		httpPut.addHeader("Content-type", "application/rdf+xml");
		httpPut.addHeader("Accept", "application/rdf+xml");
		httpPut.addHeader(Constants.INTEGRATION_TYPE, Constants.H2H);
		httpPut.addHeader(Constants.SYNC_FROM_LABEL, syncLabel);
		httpPut.addHeader(Constants.SYNC_FROM_PROJECT_ID, projectId);
		httpPut.addHeader(getAuthorizationHeader(credentials));
		httpPut.setEntity(new StringEntity(rdfData, "UTF-8"));
		if (!StringUtils.isEmpty(eTag)) {
			httpPut.addHeader("If-Match", eTag);
		}

		HttpResponse response = client.execute(httpPut);
		return response;
	}
		
	/**
	 * get resource
	 * @param url
	 * @param credentials
	 * @return
	 * @throws Exception
	 */
	public HttpResponse getTask(String url, Credentials credentials) throws Exception {

		if (StringUtils.isEmpty(url)) {
			throw new Exception("Cannot GET task, url is empty");
		}

		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);

		httpGet.addHeader("OSLC-Core-Version", "2.0");
		httpGet.addHeader("Content-type", "application/rdf+xml");
		httpGet.addHeader("Accept", "application/rdf+xml");
		httpGet.addHeader(getAuthorizationHeader(credentials));

		HttpResponse response = client.execute(httpGet);
		return response;
	}		
	
	/**
	 * gets header with authorization
	 * @param credentials
	 * @return
	 */
	private Header getAuthorizationHeader(Credentials credentials) {
		return BasicScheme.authenticate(new UsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword()), "UTF-8", false);
	}
}