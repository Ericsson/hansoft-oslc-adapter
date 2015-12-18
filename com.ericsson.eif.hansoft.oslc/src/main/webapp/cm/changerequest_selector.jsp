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
    
    Nils Kronqvist	 - adapted for Hansoft
--%>
<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>

<%
	int productId = (Integer) request.getAttribute("productId");
	String selectionUri = (String) request.getAttribute("selectionUri");
	
	String bugzillaUri = "https://landfill.bugzilla.org/bugzilla-4.2-branch";
	String hansoftMinilogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_minilogo.png";
	String hansoftLogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo.png";	
	String projectName=(String) request.getAttribute("productName");
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8">
<title>Hansoft OSLC Adapter: Resource Selector</title>
<link rel="shortcut icon" href="http://hansoft.com/wp-content/uploads/2013/01/favicon.ico">
<script type="text/javascript" src="../../../../bugzilla.js"></script>
</head>

<body style="padding: 10px; background:#f5f5f5;">
	<%-- Padding --%>
	<img style="margin-top: 10px; widht: 126px; height: 30px;" src="http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo2.png">
	<p style="color: #47aede; font-size: 26px; font-family: Open Sans;" >Project: <%=projectName%></p>
	<div id="bugzilla-body">
	
		<p  style="color: #333333; font-size: 26px; font-family: Open Sans; border-bottom: 1px solid #e5e5e5; padding-bottom: 20px; padding-top: 10px;" id="searchMessage">Find a specific task by entering words that describe it.</p>
		<p id="loadingMessage" style="display: none;color: #f66901; font-size: 26px; font-family: Open Sans; border-bottom: 1px solid #e5e5e5; padding-bottom: 20px; padding-top: 10px;">Hansoft is pondering your search. Please stand by ...</p>

		<div>
			<input type="search" style="border: 1px solid #cccccc; font-size: 20px; height: 31px; width: 615px; margin-right: 10px;" id="searchTerms" placeholder="Please, Enter Hansoft search terms" autofocus>
			<button type="button" style="cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans;"
				onclick="search( '<%= selectionUri %>' )">Search</button>
		</div>

		<div style="margin-top: 20px;">
			<select id="results" size="10" style="border: 1px solid #cccccc; width: 698px; height: 258px;"></select>
		</div>

		<div style="width: 698px; margin-top: 5px;">
			<button style="float: right; cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans;" type="button"
				onclick="cancel()">Cancel</button>
			<button style="float: right; cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans; margin-right: 10px;" type="button"
				onclick="select()">Link Task</button>
		</div>
		
		<%-- So the buttons don't float outside the content area. --%>
		<div style="clear: both;"></div>

	</div>
</body>
</html>