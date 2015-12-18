<?xml version="1.0" encoding="UTF-8"?>
<%--
 Copyright (c) 2011, 2012 IBM Corporation.

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 
 The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 and the Eclipse Distribution License is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 
 Contributors:
 
    Sam Padgett		 - initial API and implementation
    Michael Fiedler	 - adapted for OSLC4J
--%>
<%@ page contentType="application/rdf+xml" language="java" pageEncoding="UTF-8" %>
<%@ page import="com.ericsson.eif.hansoft.resources.ServiceProviderRefCatalog" %>
<%@ page import="com.ericsson.eif.hansoft.resources.ServiceProviderRef" %>
<%
	String baseUri = (String) request.getAttribute("baseUri");
	String catalogUri = (String) request.getAttribute("catalogUri");
	String oauthDomain = (String) request.getAttribute("oauthDomain");
	
	ServiceProviderRefCatalog catalog = (ServiceProviderRefCatalog) request.getAttribute("catalog");
%>
<rdf:RDF
	xmlns:oslc="http://open-services.net/ns/core#"
	xmlns:oslc_cm="http://open-services.net/xmlns/cm/1.0/"
	xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:jfs="http://jazz.net/xmlns/prod/jazz/jfs/1.0/" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<oslc:Publisher>
		<dcterms:identifier>com.ericsson.eif.hansoft</dcterms:identifier>
		<dcterms:title>Ericsson EIF</dcterms:title>
	</oslc:Publisher>
  
	<oslc:ServiceProviderCatalog rdf:about="<%= catalog.getAbout().toString() %>">
		<oslc:oauthConfiguration>
			<oslc:OAuthConfiguration>
				<oslc:oauthAccessTokenURI rdf:resource="<%= baseUri + "/oauth/accessToken" %>"/>
				<oslc:authorizationURI rdf:resource="<%= baseUri + "/oauth/authorize" %>"/>
				<oslc:oauthRequestTokenURI rdf:resource="<%= baseUri + "/oauth/requestToken" %>"/>
			</oslc:OAuthConfiguration>
		</oslc:oauthConfiguration>
    
		<% for (ServiceProviderRef sp : catalog.getServiceProviderReferences()) { %>
		<oslc:serviceProvider>
			<oslc:ServiceProvider rdf:about="<%= sp.getAbout().toString() %>">
				<dcterms:title><%= sp.getTitle() %></dcterms:title>
				<oslc:details rdf:resource="<%=sp.getDetails()[0].toString() %>" />
			</oslc:ServiceProvider>
		</oslc:serviceProvider>
		<% } %>	
    
		<dcterms:publisher rdf:resource="http://open-services.net/ns/about"/>
		<oslc:domain rdf:resource="http://open-services.net/ns/cm#"/>
		<dcterms:description>Provides information about services offered by Hansoft OSLC adapter</dcterms:description>
		<dcterms:title>Hansoft OSLC Service Provider Catalog</dcterms:title>
	</oslc:ServiceProviderCatalog>  			
</rdf:RDF>