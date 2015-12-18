package com.ericsson.eif.hansoft.mapping;

/*
* Copyright (C) 2015 Ericsson AB. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer
*    in the documentation and/or other materials provided with the
*    distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.HansoftManager;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;

/**
 * Copied from TFS Attributes Mapper. Main current purpose is a simple solution
 * to define what attributes to show in the web UI for a Hansoft item. TBD if it
 * makes sense reusing the more advanced mapping as defined in TFS solution and
 * extend with this capability - and likely formatting capability. Or if this
 * should be a separate mapping file.
 * 
 * @author qnilkro
 * 
 */
public class AttributesMapper {

	private static final Logger logger = Logger
			.getLogger(AttributesMapper.class.getName());

	private static AttributesMapper instance;
	
	// Keep entries in linked hash map to preserve order from file
	private LinkedHashMap<String, String> webUiVisibleAttributes;

	/**
	 * @return single instance of attribute mapper
	 */
	public static AttributesMapper getInstance() {
		if (instance == null) {
			instance = new AttributesMapper();
			instance.addMappingRules();
		}
		return instance;
	}

	/**
	 * Private constructor
	 */
	private AttributesMapper() {
		webUiVisibleAttributes = new LinkedHashMap<String, String>();
	}

	/**
	 * gets property name
	 * @param name
	 * @return property name or empty string
	 * 
	 * Encode so all non alphanumeric chars are replaced with hex code
	 * and surrounded by "_" characters. This mimic the way Hansoft xml
	 * export is dealing with the column names.
	 */
	public String getPropertyNameFromColumnName(String name) {
		if (StringUtils.isEmpty(name))
			return "";
		
		final int len = name.length();
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < len; i++) {
			Character ch = name.charAt(i);
			if (!Character.isDigit(ch) && !Character.isLetter(ch)) {
				// Replace with _hex value of ascii_
				sb.append('_');
				sb.append(Integer.toHexString((int) ch));
				sb.append('_');
			} else {
				sb.append(ch);
			}
		}
		
		return sb.toString();
	}

	/**
	 * gets column name from name of property
	 * Decode so all encoded chars (see above) are reverted bacj from encode.
	 * This mimic the way Hansoft xml export is dealing with the column names. 
	 * 
	 * @param name
	 * @return column name or empty string
	 */
	public String getColumnNameFromPropertyName(String name) {
		if (StringUtils.isEmpty(name))
			return "";		
		
		final int len = name.length();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < len; i++) {
			Character ch = name.charAt(i);
			if (ch == '_') {
				// Restore character from _hex value of ascii_
				int end = name.indexOf('_', i + 1);
				String asciiChar = name.substring(i + 1, end);			
				Character decodedCh = (char) Integer.decode("0x" + asciiChar).intValue();
				sb.append(decodedCh);
				i = end;
			} else {
				sb.append(ch);
			}
		}
		
		return sb.toString();
	}

	/**
	 * Create a html table key: value entry for each attribute defined in the
	 * mapping file to be shown in UI.
	 * 
	 * @param hcr
	 * @return extended info
	 */
	public String getExtendedInfo(HansoftChangeRequest hcr) {
//		if (this.webUiVisibleAttributes.isEmpty() || hcr == null)
//			return "";		
		
		String htmlInfo = "";

		Map<QName, Object> extProperties = hcr.getExtendedProperties();
		for (QName name : extProperties.keySet()) {
			String label = getColumnNameFromPropertyName(name.getLocalPart());
			String value = extProperties.get(name).toString();
			try {
				new URL(value); // Throws exception if not url
				value = "<a href=\"" + value + "\">" + value + "</a>";
			} catch (MalformedURLException e) {
				// Not a url
			}
			if (value != null) {
				htmlInfo +=  "<tr><td><b>" + label + "</b>:</td><td>" + value + "</td></tr>" + "\n";
			}
		}

		return htmlInfo;
	}


	/**
	 * Get an entry for the specified key
	 * 
	 * @param key
	 * @param hcr
	 * @param extProperties
	 * @param formatter
	 * @return Get an entry for the specified key. If missing value, still show the 
	 * entry in the web UI, and with value = "".
	 */
	private String getExtendedInfoFor(String key, HansoftChangeRequest hcr,
			Map<QName, Object> extProperties, SimpleDateFormat formatter) {

		String value = "";
		if (key.startsWith(Constants.HANSOFT_NAMESPACE_PREFIX_EXT)) {
			String localName = key
					.substring(Constants.HANSOFT_NAMESPACE_PREFIX_EXT.length() + 1);
			QName qkey = new QName(Constants.HANSOFT_NAMESPACE_EXT, localName);
			if (extProperties.containsKey(qkey)) {
				value = extProperties.get(qkey).toString();

				// Check if this is a valid url - if so, format as such
				try {
					new URL(value); // Throws exception if not url
					value = "<a href=\"" + value + "\">" + value + "</a>";
				} catch (MalformedURLException e) {
					// Not a url
				}
			} 
		} else {
			Date date = null;

			switch (key) {
			case "dcterms:identifier":
				value = hcr.getIdentifier();
				break;
			case "dcterms:modified":
				date = hcr.getModified();
				if (date != null) {
					value = formatter.format(date);
				}
				break;
			case "dcterms:created":
				date = hcr.getCreated();
				if (date != null) {
					value = formatter.format(date);
				}
				break;
			case "dcterms:description":
				value = hcr.getDescription();
				break;
			case "oslc_cm:closeDate":
				date = hcr.getCloseDate();
				if (date != null) {
					value = formatter.format(date);
				}
				break;
			case "oslc_cm:status":
				value = hcr.getStatus();
				break;
			case "oslc_cm:severity":
				value = hcr.getSeverity();
				break;
			case "oslc_cm:priority":
				value = hcr.getPriority();
				break;
			}
		}

		if (value != null) {
			String label = webUiVisibleAttributes.get(key);
			return "<tr><td><b>" + label + "</b>:</td><td>" + value
					+ "</td></tr>" + "\n";
		} else {
			return "";
		}
	}

	/**
	 * add mapping rules
	 * it is ok not to have this defined, so only log as info if not present.
	 */
	private void addMappingRules() {
		String attributesMappingFile = HansoftManager.getAdapterServletHome()
				+ "/attribute_mapping.xml";
		try {
			File inputFile = new File(attributesMappingFile);
			if (!inputFile.exists()) {
				logger.info("Attribute mapping file not defined: "
						+ attributesMappingFile);
				return;
			}
			load(new FileInputStream(inputFile));
		} catch (XPathExpressionException | ParserConfigurationException
				| SAXException | IOException e) {
			logger.error("Failed to read attribute mapping file: "
					+ attributesMappingFile, e);
		}
	}

	/**
	 * Configure mapping rules by parsing the input file
	 * 
	 * @param input
	 *            file from which to read the mapping rules
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	private void load(InputStream input) throws ParserConfigurationException,
			SAXException, IOException, XPathExpressionException {

		// initialize dom and xpath factory
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse(input);
		XPath xpath = XPathFactory.newInstance().newXPath();

		// locate all property nodes:
		NodeList propertyNodes = (NodeList) xpath.evaluate(
				"//mapping//property", doc, XPathConstants.NODESET);

		for (int i = 0; i < propertyNodes.getLength(); i++) {
			Node node = propertyNodes.item(i);
			NamedNodeMap attrs = node.getAttributes();
			Node show = attrs.getNamedItem("showInWebUI");
			if (show != null
					&& show.getNodeValue().compareToIgnoreCase("true") == 0) {
				String key = attrs.getNamedItem("key").getNodeValue();
				String value = attrs.getNamedItem("value").getNodeValue();
				webUiVisibleAttributes.put(key, value);
			}
		}
	}
}
