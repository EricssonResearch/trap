package com.ericsson.research.trap.utils;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */

/**
 * Port of Java's Future interface, for compatibility with pre-1.5
 * 
 * @author Vladimir Katardjiev
 */
public interface Future
{
	/**
	 * Cancel the future task.
	 * 
	 * @return false if the task could not be cancelled, typically because it
	 *         has already completed normally; true otherwise
	 */
	boolean cancel();
	
	/**
	 * Cancel the future task.
	 * 
	 * @param mayInterruptIfRunning
	 *            true if the thread executing this task should be interrupted;
	 *            otherwise, in-progress tasks are allowed to complete
	 * @return false if the task could not be cancelled, typically because it
	 *         has already completed normally; true otherwise
	 */
	boolean cancel(boolean mayInterruptIfRunning);

	/**
	 * Checks if the task has been cancelled.
	 * 
	 * @return <i>true</i> if cancel was called before the task executed,
	 *         <i>false</i> otherwise.
	 */
	boolean isCancelled();

	/**
	 * Checks if the task has executed.
	 * 
	 * @return <i>true</i> if the task has executed, <i>false</i> otherwise.
	 */
	boolean isDone();
}
