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

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapObject;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnFailedSending;
import com.ericsson.research.trap.delegates.OnObject;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.delegates.OnSleep;
import com.ericsson.research.trap.delegates.OnStateChange;
import com.ericsson.research.trap.delegates.OnWakeup;

public class NullDelegate implements OnAccept, OnClose, OnError, OnStateChange, OnData, OnFailedSending, OnObject, OnOpen, OnSleep, OnWakeup
{

	public void trapSleep(TrapEndpoint endpoint, Object context)
	{
		// Silently sleep. No error.
	}

	public void trapObject(TrapObject object, int channel, TrapEndpoint endpoint, Object context)
	{
		((TrapEndpointImpl)endpoint).dataDelegate.trapData(object.getSerializedData(), channel, endpoint, context);
	}

	public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context)
	{
		((TrapEndpointImpl)endpoint).logger.warn("Endpoint {} losing data. Failed sending messages, but no delegate caught that.", endpoint);
	}

	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		((TrapEndpointImpl)endpoint).logger.warn("Endpoint {} losing data. Received data with no delegate", endpoint);
	}

	public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
	{
		// Silently sleep.
	}

	public void trapClose(TrapEndpoint endpoint, Object context)
	{
		// Silently sleep.
	}

	public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		((TrapEndpointImpl)endpoint).logger.warn("Endpoint {} losing data. Received incoming connection with no accept delegate", endpoint);
	}

	public void trapWakeup(TrapEndpoint endpoint, Object context)
	{
		// Silently sleep.
	}

	public void trapOpen(TrapEndpoint endpoint, Object context)
	{
		// Silently sleep.		
	}

	public void trapError(TrapEndpoint endpoint, Object context)
	{
		((TrapEndpointImpl) endpoint).closeDelegate.trapClose(endpoint, context);
	}
	
}
