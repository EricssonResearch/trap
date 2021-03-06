package com.ericsson.research.trap.tests;

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

import java.util.logging.Level;

import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class CleanupTest
{
	
	@Test
	@Ignore // There's no guarantee that all the listeners are gone post cleanup, so we'll get an exception here.
	public void testCleanup() throws Exception
	{
        JDKLoggerConfig.initForPrefixes(Level.ALL);
		String cfg = "trap.transport.socket.port = 12015\ntrap.transport.websocket.port = 12016\ntrap.transport.http.port=12017\n";
		TrapListener listener = TrapFactory.createListener(cfg);
		
		listener.listen(new OnAccept() {
			
			@Override
			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		listener.getClientConfiguration();
		
		// Stop
		listener.close();
		
		// Start again
		
		listener.listen(new OnAccept() {
			
			@Override
			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		listener.getClientConfiguration();
	}

}
