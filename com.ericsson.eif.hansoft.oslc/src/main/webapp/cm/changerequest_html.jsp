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
 
    Sam Padgett		 - initial API and implementation
    Michael Fiedler	 - adapted for OSLC4J
--%>
<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>
<%@ page import="com.ericsson.eif.hansoft.resources.HansoftChangeRequest" %>
<%@ page import="com.ericsson.eif.hansoft.resources.Person" %>
<%@ page import="java.net.*,java.util.*,java.text.SimpleDateFormat" %>

<%
	HansoftChangeRequest changeRequest = (HansoftChangeRequest) request.getAttribute("changeRequest");
	String heading = "(" + changeRequest.getIdentifier() + ") " + changeRequest.getTitle();
	
	List<Person> contributors = changeRequest.getContributors();
	
	SimpleDateFormat formatter = new SimpleDateFormat();
	Date modifiedDate = (Date) changeRequest.getModified();
	String modified = formatter.format(modifiedDate);
	
	String bugzillaUri = "https://landfill.bugzilla.org/bugzilla-4.2-branch";
	String hansoftMinilogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_minilogo.png";
	String hansoftLogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo.png";

%>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html;charset=utf-8">
		<title>Hansoft OSLC Adapter: Change Request for <%= heading %></title>
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
							Hansoft OSLC Adapter: Change Request
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
	
				<h1><%= heading%></h1>
				
				<p>Provides a summary of the Change Request</p>

	            <table>
		            <tr>
			            <td><b>This document</b>:</td>
			            <td><a href="<%= changeRequest.getAbout() %>">
			            <%= changeRequest.getAbout() %></a></td>
		            </tr>
		            <tr>
			            <td><b>Project</b>:</td>
			            <td><a href="<%= changeRequest.getServiceProvider() %>">
			            <%= changeRequest.getServiceProvider() %></a></td>
		            </tr>	            
		            <tr>
			            <td><b>Title</b>:</td>
			            <td><%= changeRequest.getTitle()%></td>
		            </tr>		            
					<% String assignedTo = "";
						for (Person person : contributors) {                
				    		assignedTo += person.getName() + " (mail: " + person.getMbox() + ")<br/>";
						} 		            	
					if (assignedTo != null && !("").equals(assignedTo)) { %>
		            <tr>
		            	<td><b>Assigned to</b>:</td>
			            <td><%= assignedTo%></td>
		            </tr>
		            <% } %>
		            <% if (changeRequest.getWorkRemaining() != null) { %>
		           	<tr>
			            <td><b>Work Remaining</b>:</td>
			            <td><%= changeRequest.getWorkRemaining()%></td>
		            </tr>
		            <% } %>
		            <% if (changeRequest.getParentTask() != null && !("").equals(changeRequest.getParentTask())) { %>
		           	<tr>
			            <td><b>Parent Task ID</b>:</td>
			            <td><%= changeRequest.getParentTask()%></td>
		            </tr>
		            <% } %>
		            <% if (changeRequest.getDescription() != null && !("").equals(changeRequest.getDescription())) { %>
		           	<tr>
			            <td><b>Description</b>:</td>
			            <td><%= changeRequest.getDescription()%></td>
		            </tr>
		            <% } %>	
					<% if (changeRequest.getModified() != null) { %>
		           	<tr>
			            <td><b>Modified</b>:</td>
			            <td><%= changeRequest.getModified()%></td>
		            </tr>
		            <% } %>
					<% if (changeRequest.getStatus() != null && !("").equals(changeRequest.getStatus())) { %>
		           	<tr>
			            <td><b>Status</b>:</td>
			            <td><%= changeRequest.getStatus()%></td>
		            </tr>
		            <% } %>
					<% if (changeRequest.getPriority() != null && !("").equals(changeRequest.getPriority())) { %>
		           	<tr>
			            <td><b>Priority</b>:</td>
			            <td><%= changeRequest.getPriority()%></td>
		            </tr>
		            <% } %>
		            <%= changeRequest.getExtendendInfo() %>		            
	            </table>
			</div>
		</div>
		<br/>
		<div id="footer">
			<div class="intro"></div>
			<div class="outro">
				<div style="margin: 0 1em 1em 1em; line-height: 1.6em; text-align: left">
					<b>OSLC Tools Adapter Server 0.1</b> brought to you by <a href="http://eclipse.org/lyo">Eclipse Lyo</a><br>
				</div>
			</div>
		</div>
	</body>
</html>