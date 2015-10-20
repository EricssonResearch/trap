package com.ericsson.research.trap.impl;

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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.auth.TrapAuthenticationException;
import com.ericsson.research.trap.spi.TrapMessage;

public class NullAuthentication implements TrapAuthentication
{
	
	public static NullAuthentication	instance	= new NullAuthentication();
	
	public Collection<String> getContextKeys(Collection<String> availableKeys) throws TrapException
	{
		return new LinkedList<String>();
	}

	public boolean verifyAuthentication(TrapMessage message, Map<String, String> headers, Map<String, Object> context) throws TrapAuthenticationException
	{
		return true;
	}

	public String createAuthenticationChallenge(TrapMessage src, Map<String, Object> context)
	{
		return "";

	}
	public String createAuthenticationResponse(TrapMessage challenge, TrapMessage outgoing, Map<String, String> headers, Map<String, Object> context)
	{
		return "";
	}
	
}
