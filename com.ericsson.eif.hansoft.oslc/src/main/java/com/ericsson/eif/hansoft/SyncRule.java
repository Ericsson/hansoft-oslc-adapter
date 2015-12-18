package com.ericsson.eif.hansoft;

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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("syncRule")
public class SyncRule {

	@XStreamAsAttribute
	private String attribute;
	
	@XStreamAsAttribute
	private String attributeNameSpace;
	
	@XStreamAsAttribute
	private String value;
	
	@XStreamAsAttribute
	private boolean ignoredByScheduler;
	
	@XStreamAsAttribute
	private String operator;
	
	/**
	 * Default constructor
	 */
	public SyncRule() {
	}
	
	/**
	 * Constructor
	 * @param attribute
	 * @param value
	 */
	public SyncRule(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
		this.ignoredByScheduler = false;
	}
	
	/**
	 * @return attribute
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * sets attribute
	 * @param attribute
	 */
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	/**
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * sets value
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the ignoredByScheduler
	 */
	public boolean isIgnoredByScheduler() {
		return ignoredByScheduler;
	}

	/**
	 * @param ignoredByScheduler the ignoredByScheduler to set
	 */
	public void setIgnoredByScheduler(boolean ignoredByScheduler) {
		this.ignoredByScheduler = ignoredByScheduler;
	}

	/**
	 * @return attributeNameSpace
	 */
	public String getAttributeNameSpace() {
		return attributeNameSpace;
	}

	/**
	 * sets attributeNameSpace
	 * @param attributeNameSpace
	 */
	public void setAttributeNameSpace(String attributeNameSpace) {
		this.attributeNameSpace = attributeNameSpace;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}
}
