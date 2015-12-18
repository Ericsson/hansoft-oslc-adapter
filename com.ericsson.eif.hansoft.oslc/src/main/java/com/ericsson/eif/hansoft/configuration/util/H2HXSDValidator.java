package com.ericsson.eif.hansoft.configuration.util;

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
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class H2HXSDValidator {

	public static void main(String[] args) throws IOException, SAXException {

		File schemaFile = new File(System.getProperty("user.dir") + File.separator + "H2HConfigXMLSchema.xsd");
		if (!schemaFile.exists()) {
			System.out.println("XSD schema file not found on path: " + schemaFile.getAbsolutePath());
			return;
		}
		
		File xmlFile = new File(System.getProperty("user.dir") + File.separator + "H2HConfig.xml");
		if (!xmlFile.exists()) {
			System.out.println("XML config file not found on path: " + xmlFile.getAbsolutePath());
			return;
		}

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(schemaFile);

		Source xmlSource = new StreamSource(xmlFile);
		Validator validator = schema.newValidator();

		try {
			validator.validate(xmlSource);
			System.out.println(xmlSource.getSystemId() + " is valid");
		} catch (SAXException e) {
			System.out.println(xmlSource.getSystemId() + " is NOT valid");
			System.out.println("Reason: " + e.getLocalizedMessage());
		}
	}
}
