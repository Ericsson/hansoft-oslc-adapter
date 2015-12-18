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
<%@ page contentType="application/json" language="java" pageEncoding="UTF-8" %>
<%@ page import="com.ericsson.eif.hansoft.resources.HansoftChangeRequest"%>
<%@ page import="java.net.*,java.util.*,java.net.URLEncoder" %> 

{
<% 
List<HansoftChangeRequest> results = (List<HansoftChangeRequest>) request.getAttribute("results");
//for ()
if ( results.isEmpty() ) {
%>
results: [{"title" : "No matching results in Hansoft Backlog", "resource": ""}]	
<% } else { %>
results: [
<% int i = 0; for (HansoftChangeRequest b : results) { String title = URLEncoder.encode( b.getTitle(), "UTF-8" ); %>
    
    <% if (i > 0) { %>,<% } %>
    
   {  "title" : "<%= b.getIdentifier() %>:<%= title %>",
      "resource" : "<%= b.getAbout() %>"
   }
<% i++; } %>
]
<%} %>
}