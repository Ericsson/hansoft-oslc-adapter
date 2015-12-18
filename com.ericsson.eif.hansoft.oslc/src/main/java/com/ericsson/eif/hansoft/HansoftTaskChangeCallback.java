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

import se.hansoft.hpmsdk.HPMUniqueID;

public class HansoftTaskChangeCallback {
	
	private HPMUniqueID changedByResourceID;
	
	private HPMUniqueID changedByImpersonatedResourceID;
	
	private HPMUniqueID taskId;
	
	private int changedColumnHash;

	/**
	 * @return changedByResourceID
	 */
	public HPMUniqueID getChangedByResourceID() {
		return changedByResourceID;
	}

	/** 
	 * sets changedByResourceID
	 * @param changedByResourceID
	 */
	public void setChangedByResourceID(HPMUniqueID changedByResourceID) {
		this.changedByResourceID = changedByResourceID;
	}

	/**
	 * @return changedByImpersonatedResourceID
	 */
	public HPMUniqueID getChangedByImpersonatedResourceID() {
		return changedByImpersonatedResourceID;
	}

	/**
	 * sets ChangedByImpersonatedResourceID
	 * @param changedByImpersonatedResourceID
	 */
	public void setChangedByImpersonatedResourceID(
			HPMUniqueID changedByImpersonatedResourceID) {
		this.changedByImpersonatedResourceID = changedByImpersonatedResourceID;
	}

	/**
	 * @return taskId
	 */
	public HPMUniqueID getTaskId() {
		return taskId;
	}

	/**
	 * sets taskId
	 * @param taskId
	 */
	public void setTaskId(HPMUniqueID taskId) {
		this.taskId = taskId;
	}

	/**
	 * @return changedColumnHash
	 */
	public int getChangedColumnHash() {
		return changedColumnHash;
	}

	/**
	 * sets changedColumnHash
	 * @param changedColumnHash
	 */
	public void setChangedColumnHash(int changedColumnHash) {
		this.changedColumnHash = changedColumnHash;
	}
}
