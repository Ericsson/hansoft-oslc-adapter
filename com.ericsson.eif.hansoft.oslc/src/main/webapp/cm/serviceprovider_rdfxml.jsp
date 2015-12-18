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
<%@ page import="java.net.URI" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.Service" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.ServiceProvider" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.Dialog" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.CreationFactory" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.ResourceShape" %>
<%@ page import="org.eclipse.lyo.oslc4j.core.model.QueryCapability" %>

<%
	Service service = (Service) request.getAttribute("service");
    response.addHeader("OSLC-Core-Version", "2.0");
	ServiceProvider serviceProvider = (ServiceProvider)request.getAttribute("serviceProvider");
	String details = serviceProvider.getDetails()[0].toString();
	String bugzillaUri = "https://landfill.bugzilla.org/bugzilla-4.2-branch";
	String hansoftMinilogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_minilogo.png";
	String hansoftLogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo.png";	
	//OSLC Dialogs
	Dialog [] selectionDialogs = service.getSelectionDialogs();
	String selectionDialog = selectionDialogs[0].getDialog().toString();
	Dialog [] creationDialogs = service.getCreationDialogs();
	String creationDialog = creationDialogs[0].getDialog().toString();
	//OSLC CreationFactory and shape
	CreationFactory [] creationFactories = service.getCreationFactories();
	String creationFactory = creationFactories[0].getCreation().toString();
	URI[] creationShapes = creationFactories[0].getResourceShapes();
	String creationShape = creationShapes[0].toString();
	//OSLC QueryCapability and shape
	QueryCapability [] queryCapabilities= service.getQueryCapabilities();
	String queryCapability = queryCapabilities[0].getQueryBase().toString();
	String queryShape = queryCapabilities[0].getResourceShape().toString();
%>
<rdf:RDF
	xmlns:oslc="http://open-services.net/ns/core#"
    xmlns:oslc_cm="http://open-services.net/xmlns/cm/1.0/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:jfs="http://jazz.net/xmlns/prod/jazz/jfs/1.0/" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	<oslc:ServiceProvider rdf:about="<%=serviceProvider.getAbout() %>">
		<dcterms:title rdf:parseType="Literal"><%=serviceProvider.getTitle() %></dcterms:title>
		<oslc:details rdf:resource="<%=details %>" />
		<dcterms:description rdf:parseType="Literal"><%=serviceProvider.getDescription() %></dcterms:description>
		<dcterms:publisher rdf:resource="<%=serviceProvider.getPublisher().getAbout() %>" />
		<oslc:service>
			<oslc:Service>
				<oslc:domain rdf:resource="http://open-services.net/ns/cm#" />
				<oslc:selectionDialog>
					<oslc:Dialog>
						<dcterms:title rdf:parseType="Literal">Hansoft Change Request Selection</dcterms:title>
						<oslc:label>Change Request</oslc:label>
						<oslc:dialog rdf:resource="<%=selectionDialog %>" />
						<oslc:hintWidth>810px</oslc:hintWidth>
						<oslc:hintHeight>594px</oslc:hintHeight>
						<oslc:resourceType rdf:resource="http://open-services.net/ns/cm#ChangeRequest" />
					</oslc:Dialog>
				</oslc:selectionDialog>
				<oslc:creationDialog>
					<oslc:Dialog>
						<dcterms:title rdf:parseType="Literal">Hansoft Change Request Creation</dcterms:title>
						<oslc:label>Change Request</oslc:label>
						<oslc:dialog rdf:resource="<%=creationDialog %>" />
						<oslc:hintWidth>688px</oslc:hintWidth>
						<oslc:hintHeight>486px</oslc:hintHeight>
						<oslc:resourceType rdf:resource="http://open-services.net/ns/cm#ChangeRequest" />
					</oslc:Dialog>
				</oslc:creationDialog>
				<oslc:creationFactory>
					<oslc:CreationFactory>
						<dcterms:title rdf:parseType="Literal">Collection Creation Factory</dcterms:title>
						<oslc:creation rdf:resource="<%=creationFactory %>" />
						<oslc:resourceType rdf:resource="http://open-services.net/ns/cm#ChangeRequest" />
						<oslc:resourceShape rdf:resource="<%=creationShape %>" />
					</oslc:CreationFactory>
				</oslc:creationFactory>	
				<oslc:queryCapability>
					<oslc:QueryCapability>
						<dcterms:title rdf:parseType="Literal">Query Capability</dcterms:title>
						<oslc:creation rdf:resource="<%=queryCapability %>" />
						<oslc:resourceType rdf:resource="http://open-services.net/ns/cm#ChangeRequest" />
						<oslc:resourceShape rdf:resource="<%=queryShape %>" />
					</oslc:QueryCapability>
				</oslc:queryCapability>										
			</oslc:Service>
		</oslc:service>
  </oslc:ServiceProvider>  			
</rdf:RDF>