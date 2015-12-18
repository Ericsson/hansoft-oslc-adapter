/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ericsson.eif.hansoft.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import com.ericsson.eif.hansoft.Constants;

public class StringUtils {

    /**
     * Converts input stream to string
     * @param is
     * @throws IOException
     */
    public static String streamToString(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append(Constants.LS);
        }
        return sb.toString();
    }

    /**
     * Converts end of line character (\n) to html tag <br />
     * @param expr 
     */
    public static String forHtml(String expr) {
        // convert each line to a paragraph
        StringTokenizer st = new StringTokenizer(expr, "\n"); //$NON-NLS-1$
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            sb.append(XmlUtils.encode(st.nextToken()) + "<br/>"); //$NON-NLS-1$ 
        }
        return sb.toString();
    }

    /**
     * @param date
     * @return string representation of date in rfc2822 format
     */
    public static String rfc2822(Date date) {
        String pattern = "EEE, dd MMM yyyy HH:mm:ss Z"; //$NON-NLS-1$
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    /**
     * @param dateStr
     * @return Date in rfc2822 format
     * @throws ParseException
     */
    public static Date rfc2822(String dateStr) throws ParseException {
        String pattern = "EEE, dd MMM yyyy HH:mm:ss Z"; //$NON-NLS-1$
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.parse(dateStr);
    }

    /**
     * @param title
     * @return escaped string
     */
    @SuppressWarnings("nls")
    public static String stringEscape(String title) {
        return title.replaceAll("\\\\", "\\\\\\\\");
    }

    /**
     * @param s
     * @return true if string is empty otherwise false
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
    
    /**
     * @param s
     * @return true if string is not empty otherwise false
     */
    public static boolean isNotEmpty(String s) 
    {
        return s != null && s.trim().length() != 0;
    }

    /**
     * @param s
     * @return true if given string is number, otherwise false
     */
    public static boolean isNumeric(String s) {    	
    	if (s == null)
    		return false;
    	
    	for (char c : s.toCharArray())
        {
            if (!Character.isDigit(c)) 
            	return false;
        }
    	
        return true;
    }
}
