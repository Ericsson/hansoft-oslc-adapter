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
<script type="text/javascript" src="../bugzilla.js"></script>
</head>

<body style="padding: 10px; background:#f5f5f5;">
	<div>
			<input type="password" style="border: 1px solid #cccccc; font-size: 20px; height: 31px; width: 615px; margin-right: 10px;" id="plainPassword" placeholder="Please, Enter Hansoft password for encoding" autofocus>
			<button type="button" style="cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans;"
				onclick="encodePass( '<%= selectionUri %>' )">Encode</button>
	</div>
</body>
</html>