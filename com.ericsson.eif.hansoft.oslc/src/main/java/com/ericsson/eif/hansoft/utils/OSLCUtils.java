package com.ericsson.eif.hansoft.utils;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.wink.common.internal.utils.UnmodifiableMultivaluedMap;
import org.eclipse.lyo.oslc4j.provider.jena.OslcRdfXmlProvider;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.integration.HansoftOslcRdfXmlProvider;
import com.ericsson.eif.hansoft.resources.ChangeRequest;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;

public class OSLCUtils {

	/**
	 * @param currentLink
	 * @param incomingChangeRequest
	 * @return backLink from change request
	 */
	public static String getRelatedChangeRequestBackLink (final String currentLink, final ChangeRequest incomingChangeRequest) {
		String backLink = currentLink;
		
		if (incomingChangeRequest.getRelatedChangeRequests() != null && incomingChangeRequest.getRelatedChangeRequests().length > 0 ){
			String newLink = incomingChangeRequest.getRelatedChangeRequests()[0].getValue().toString();
			backLink = (backLink == null)? newLink : (backLink + ", " + newLink);
		}
		return backLink;
	}	
	
	/**
	 * @param changeRequest
	 * @return eTag from change request 
	 */
	public static String getETagFromChangeRequest(final ChangeRequest changeRequest) {		
        Long eTag = null;

        if (changeRequest.getModified() != null) {
            eTag = changeRequest.getModified().getTime();
        } else if (changeRequest.getCreated() != null) {
            eTag = changeRequest.getCreated().getTime();
        } else {
            eTag = new Long(0);
        }

        return eTag.toString();
    }

    /**
     * @param eTagFromChangeRequest
     * @param httpServletResponse
     */
    public static void setETagHeader(final String eTagFromChangeRequest,
            final HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader(Constants.ETAG, eTagFromChangeRequest);
    }
   
    /**
     * @param response
     * @return eTag header or null
     */
    public static String getETagHeader(final HttpResponse response) {
    	if (response.containsHeader(Constants.ETAG)) {
    		return response.getFirstHeader(Constants.ETAG).getValue();
    	}
    	return null;
    }

	/**
	 * @param hansoftChangeRequest
	 * @return RDF data
	 * @throws WebApplicationException
	 * @throws IOException
	 */
	public static String createRDFData(HansoftChangeRequest hansoftChangeRequest) throws WebApplicationException, IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	OslcRdfXmlProvider provider = new OslcRdfXmlProvider();
		provider.writeTo(hansoftChangeRequest, HansoftChangeRequest.class, HansoftChangeRequest.class, null, null, null, outputStream);
		return outputStream.toString("UTF-8");
	}
	
	/**
	 * @param rdfData
	 * @return tasks created from RDF data
	 * @throws WebApplicationException
	 * @throws IOException
	 */
	public static Object[] createTaskFromRDFData(String rdfData) throws WebApplicationException, IOException {
		InputStream inputStream = new ByteArrayInputStream(rdfData.getBytes(StandardCharsets.UTF_8));
		HansoftOslcRdfXmlProvider provider = new HansoftOslcRdfXmlProvider();

    	MediaType mediaType = new MediaType("application", "rdf+xml", new HashMap<String, String>());
    	UnmodifiableMultivaluedMap map = new UnmodifiableMultivaluedMap<>(null);
    	Object[] tasks = provider.readFrom(HansoftChangeRequest.class, mediaType, map, inputStream);
    	
		return tasks;
	}
}
