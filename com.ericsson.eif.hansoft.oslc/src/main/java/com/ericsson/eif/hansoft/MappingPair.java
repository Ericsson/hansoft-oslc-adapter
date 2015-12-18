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

@XStreamAlias("mapping")
public class MappingPair {

	@XStreamAsAttribute
	private String use;
	@XStreamAsAttribute
	private String prefix;
	@XStreamAsAttribute
	private String nameSpace;
	@XStreamAsAttribute
	private String localPart;

	@XStreamAsAttribute
	private String destinationPrefix;
	@XStreamAsAttribute
	private String destinationNameSpace;
	@XStreamAsAttribute
	private String destinationLocalPart;
	
	/**
	 * Default constructor
	 */
	public MappingPair() {
	}

	/**
	 * Constructor
	 * @param localPart
	 * @param destinationLocalPart
	 */
	public MappingPair(String localPart, String destinationLocalPart) {
		this.localPart = localPart;
		this.destinationLocalPart = destinationLocalPart;		
	}

	/**
	 * Constructor
	 * @param localPart
	 * @param destinationLocalPart
	 * @param nameSpace
	 * @param destinationNameSpace
	 */
	public MappingPair(String localPart, String destinationLocalPart,
			String nameSpace, String destinationNameSpace) {
		this(localPart, destinationLocalPart);
		this.nameSpace = nameSpace;
		this.destinationNameSpace = destinationNameSpace;
	}

	/**
	 * Constructor
	 * @param localPart
	 * @param destinationLocalPart
	 * @param prefix
	 * @param destinationPrefix
	 * @param nameSpace
	 * @param destinationNameSpace
	 */
	public MappingPair(String localPart, String destinationLocalPart,
			String prefix, String destinationPrefix, String nameSpace,
			String destinationNameSpace) {
		this(localPart, destinationLocalPart, nameSpace, destinationNameSpace);
		this.prefix = prefix;
		this.destinationPrefix = destinationPrefix;
	}

	/**
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Sets prefix
	 * @param prefix
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return namespace
	 */
	public String getNameSpace() {
		return nameSpace;
	}

	/**
	 * Sets namespace
	 * @param nameSpace
	 */
	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	/**
	 * @return localPart
	 */
	public String getLocalPart() {
		return localPart;
	}

	/**
	 * sets localPart
	 * @param localPart
	 */
	public void setLocalPart(String localPart) {
		this.localPart = localPart;
	}

	/**
	 * @return destinationPrefix
	 */
	public String getDestinationPrefix() {
		return destinationPrefix;
	}

	/**
	 * sets destinationPrefix
	 * @param destinationPrefix
	 */
	public void setDestinationPrefix(String destinationPrefix) {
		this.destinationPrefix = destinationPrefix;
	}

	/**
	 * @return destinationNameSpace
	 */
	public String getDestinationNameSpace() {
		return destinationNameSpace;
	}

	/**
	 * sets destinationNameSpace
	 * @param destinationNameSpace
	 */
	public void setDestinationNameSpace(String destinationNameSpace) {
		this.destinationNameSpace = destinationNameSpace;
	}

	/**
	 * @return destinationLocalPart
	 */
	public String getDestinationLocalPart() {
		return destinationLocalPart;
	}

	/**
	 * sets destinationLocalPart
	 * @param destinationLocalPart
	 */
	public void setDestinationLocalPart(String destinationLocalPart) {
		this.destinationLocalPart = destinationLocalPart;
	}

	/** 
	 * @return use
	 */
	public String getUse() {
		return use;
	}

	/**
	 * sets use
	 * @param use
	 */
	public void setUse(String use) {
		this.use = use;
	}
}
