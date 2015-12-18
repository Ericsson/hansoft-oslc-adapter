<!DOCTYPE html>
<%--
 Copyright (c) 2011, 2012 IBM Corporation.

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 
 The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 and the Eclipse Distribution License is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 
 Contributors:
 
    Sam Padgett	  	- initial API and implementation
    Michael Fiedler	- adapted for OSLC4J
--%>

<%@page import="com.ericsson.eif.hansoft.resources.HansoftChangeRequest"%>
<%@page import="org.eclipse.lyo.oslc4j.core.model.ServiceProvider"%>
<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%
    List<HansoftChangeRequest> changeRequests = (List<HansoftChangeRequest>) request.getAttribute("results");
	ServiceProvider serviceProvider = (ServiceProvider) request.getAttribute("serviceProvider");
	
	String heading = "(" + serviceProvider.getIdentifier() + ") " + serviceProvider.getTitle();
	
	String queryUri = (String)request.getAttribute("queryUri");
	String nextPageUri = (String)request.getAttribute("nextPageUri");
	String previousPageUri = (String)request.getAttribute("previousPageUri");
	String pageNumber = (String)request.getAttribute("pageNumber");
	
	String bugzillaUri = "https://landfill.bugzilla.org/bugzilla-4.2-branch";
	String hansoftMinilogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_minilogo.png";
	String hansoftLogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo.png";	
	
%>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html;charset=utf-8">
		<title>Hansoft OSLC Adapter: Service Provider for <%= heading %></title>
		<link href="<%= bugzillaUri %>/skins/standard/global.css" rel="stylesheet" type="text/css">
		<link href="<%= bugzillaUri %>/skins/standard/index.css" rel="stylesheet" type="text/css">
		<link href="<%= bugzillaUri %>/skins/standard/global.css" rel="alternate stylesheet" title="Classic" type="text/css">
		<link href="<%= bugzillaUri %>/skins/standard/index.css" rel="alternate stylesheet" title="Classic" type="text/css">
		<link href="<%= bugzillaUri %>/skins/contrib/Dusk/global.css" rel="stylesheet" title="Dusk" type="text/css">
		<link href="<%= bugzillaUri %>/skins/contrib/Dusk/index.css" rel="stylesheet" title="Dusk" type="text/css">
		<link href="<%= bugzillaUri %>/skins/custom/global.css" rel="stylesheet" type="text/css">
		<link href="<%= bugzillaUri %>/skins/custom/index.css" rel="stylesheet" type="text/css">
		<link rel="shortcut icon" href="<%= bugzillaUri %>/images/favicon.ico">
	</head>
	<body onload="">
	
		<div id="header">
			<div id="banner"></div>
			<table border="0" cellspacing="0" cellpadding="0" id="titles">
				<tr>
					<td id="title">
						<p>
							Hansoft OSLC Adapter: Service Provider
						</p>
					</td>
					<td id="information">
						<p class="header_addl_info">
							version 0.1
						</p>
					</td>
				</tr>
			</table>
		</div>
		
		<div id="bugzilla-body">  
			<div id="page-index">
			
				<img src=<%= hansoftLogo%> alt="icon" />
					<table>
						<tr>
						<td style="min-width:70px">
						<% if (previousPageUri != null) { %><a href="<%= previousPageUri %>">Previous Page</a><% } %>
						<% if (previousPageUri == null) { %><a style="pointer-events: none; cursor: default;">Previous Page</a><% } %>
						</td>
						<td style="min-width:10px">
						<% if (pageNumber != null) { %><h4><%= pageNumber %> </h4><% } %>
						</td>
						<td style="min-width:50px">
						<% if (nextPageUri != null) { %><a href="<%= nextPageUri %>">Next Page</a><% } %>
						<% if (nextPageUri == null) { %><a style="pointer-events: none; cursor: default;">Next Page</a><% } %>
						</td>
						</tr>
					</table>
				<h1>Query Results</h1>
				<p>Found items (limited to 100): <%= changeRequests.size() %></p>

                <% for (HansoftChangeRequest changeRequest : changeRequests) { %>                
                <p>Summary: <%= changeRequest.getTitle() %><br /><a href="<%= changeRequest.getAbout() %>">
                	<%= changeRequest.getAbout() %></a></p>
			    <% } %>
            	

			</div>
		</div>
		
		<div id="footer">
			<div class="intro"></div>
			<div class="outro">
				<div style="margin: 0 1em 1em 1em; line-height: 1.6em; text-align: left">
					<b>OSLC Tools Adapter Server 0.1</b> brought to you by <a href="http://eclipse.org/lyo">Eclipse Lyo</a><br />
				</div>
			</div>
		</div>
	</body>
</html>

