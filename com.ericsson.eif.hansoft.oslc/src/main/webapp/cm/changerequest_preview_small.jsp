<!DOCTYPE html>
<!--
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
 -->
<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>
<%@ page import="java.net.*,java.util.*,java.text.SimpleDateFormat" %>
<%@ page import="com.ericsson.eif.hansoft.resources.HansoftChangeRequest" %>
<%@ page import="com.ericsson.eif.hansoft.resources.Person" %>


<%
	HansoftChangeRequest changeRequest = (HansoftChangeRequest)request.getAttribute("changeRequest");
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
<title>Change Request: <%= heading %></title>
<link href="<%= bugzillaUri %>/skins/standard/global.css" rel="stylesheet" type="text/css">
<link href="<%= bugzillaUri %>/skins/standard/index.css" rel="stylesheet" type="text/css">
<link href="<%= bugzillaUri %>/skins/standard/global.css" rel="alternate stylesheet" title="Classic" type="text/css">
<link href="<%= bugzillaUri %>/skins/standard/index.css" rel="alternate stylesheet" title="Classic" type="text/css">
<link href="<%= bugzillaUri %>/skins/contrib/Dusk/global.css" rel="stylesheet" title="Dusk" type="text/css">
<link href="<%= bugzillaUri %>/skins/contrib/Dusk/index.css" rel="stylesheet" title="Dusk" type="text/css">
<link href="<%= bugzillaUri %>/skins/custom/global.css" rel="stylesheet" type="text/css">
<link href="<%= bugzillaUri %>/skins/custom/index.css" rel="stylesheet" type="text/css">
<link rel="shortcut icon" href="http://hansoft.com/wp-content/uploads/2013/01/favicon.ico">
<style type="text/css">
body {
	background: #FFFFFF;
	padding: 0;
}

td {
	padding-right: 5px;
	min-width: 175px;
}

th {
	padding-right: 5px;
	text-align: right;
}
</style>
</head>
<body>
	<div id="bugzilla-body">
<table class="edit_form">
	<tr>
		<th>Status:</th>
		<td><%= changeRequest.getStatus() %></td>
		<th>Priority:</th>
		<td><%= changeRequest.getPriority() %></td>
	</tr>
	
	<tr>
		<th>Modified:</th>
		<td><%= modified %></td>
    	<th><b>Assigned to</b>:</th>
    	<td>
            <% for (Person person : contributors) { %>                
                <%= person.getName() + " (mail: " + person.getMbox() + ")"%><br/>
			<% } %>		            	
    	<td>
	</tr>

</table>
</div>
</body>
</html>