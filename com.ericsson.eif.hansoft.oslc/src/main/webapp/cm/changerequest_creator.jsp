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
 
    Sam Padgett 	 - initial API and implementation
    Michael Fiedler	 - adapted for OSLC4J
    
    Nils Kronqvist	 - adapted for Hansoft
--%>
<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.net.URLDecoder" %>
<% 
    String productName = (String) request.getAttribute("productName");
	String creatorUri = "";//(String) request.getAttribute("creatorUri");
    String name = (String) request.getAttribute("externalName");
    String description = "";
    //Replacing problematic chracters
    name = name.replace("\"", "'");
    name = URLDecoder.decode(name, "UTF-8");
    //Identifying key characters in URL and queryString
    int posDescURL = creatorUri.indexOf("dc:description");
    int posName = name.indexOf("dc:name");
    int posDesc = name.indexOf("dc:description");
     //Setting the value to display
    creatorUri = (posDescURL > 0 ? creatorUri.substring(0, posDescURL-1 ): creatorUri );
    description = (posDesc > 0 ? name.substring(posDesc+15, name.length() ): "" );
    name = ( posDesc > 0 ? name.substring(posName+8, posDesc - 1): name.substring(posName + 8) );	
    
	String bugzillaUri = "https://landfill.bugzilla.org/bugzilla-4.2-branch";
	String hansoftMinilogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_minilogo.png";
	String hansoftLogo = "http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo.png";		
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8">
<title>Hansoft OSLC Adapter: Resource Creator</title>
<link href="<%=bugzillaUri%>/skins/standard/global.css" rel="stylesheet" type="text/css">
<link href="<%=bugzillaUri%>/skins/standard/index.css" rel="stylesheet" type="text/css">
<link href="<%=bugzillaUri%>/skins/standard/global.css" rel="alternate stylesheet" title="Classic" type="text/css">
<link href="<%=bugzillaUri%>/skins/standard/index.css" rel="alternate stylesheet" title="Classic" type="text/css">
<link href="<%=bugzillaUri%>/skins/contrib/Dusk/global.css" rel="stylesheet" title="Dusk" type="text/css">
<link href="<%=bugzillaUri%>/skins/contrib/Dusk/index.css" rel="stylesheet" title="Dusk" type="text/css">
<link rel="icon" href="http://hansoft.com/wp-content/uploads/2013/01/favicon.ico" type="image/x-icon">
<script type="text/javascript" src="../../../../bugzilla.js"></script>
</head>

<body style="padding: 10px; background:#f5f5f5;">
	<img style="margin-top: 10px; widht: 126px; height: 30px;" src="http://www.hansoft.com/wp-content/themes/quare/img/hansoft_logo2.png">
	<p style="color: #47aede; font-size: 26px; font-family: Open Sans; margin: 5px 0px;">Project: <%=productName%></p>
	<p  style="margin: 5px 0px; color: #333333; font-size: 22px; font-family: Open Sans; padding-top: 10px;" id="searchMessage">Please fill the form and submit.</p>
	
	<div id="bugzilla-body" style="background:#f5f5f5;">
    <form id="Create" method="POST" class="enter_bug_form" style="background:#f5f5f5;">
	<input name="product" type="hidden" value="<%=productName%>" />
			<table style="clear: both;">
			<tr>
				
					<td style="width: 600px;">
						<div style="float: left; color: #333333; font-size: 16px; font-family: Open Sans;" class="field_label required">Item Name:</div>
						<input name="summary" class="required text_input"
							type="text" style="background: #ffffff; width: 600px; border: 1px solid #cccccc; font-size: 16px; height: 31px;" id="summary" value="<%=name%>" required autofocus>
					</td>				
				</tr>
				<tr>
					
					<td style="width: 600px;">
						<div style="float: left; margin-top: 10px; color: #333333; font-size: 16px; font-family: Open Sans;"  class="field_label">Description:</div>
						<textarea style="width: 600px; height: 150px; border: 1px solid #cccccc;"
							id="description" name="description"><%=description%></textarea>
					</td>
				</tr>
				
				<tr>
					<td style="width: 600px;">
						<input type="button" style="float: right; cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans;" value="Cancel" onclick="javascript:cancel()">
						<input type="button"
							style="float: right; margin-right: 10px; cursor: pointer; color: #888888; font-size: 20px; font-family: Open Sans;"
							value="Create Task"
							onclick="create( '<%= creatorUri %>' )">
					</td>
				</tr>
			</table>
			<div style="width: 500px;">				
			</div>			
		</form>
		<div style="clear: both;"></div>
	</div>
</body>
</html>