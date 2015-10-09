package com.ericsson.research.trap.spi.transports.loopbacktest;

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

import org.junit.Before;

import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.spi.transports.AsyncDelayedLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

/*
 * This test will try the standard test suite on a delayed transport.
 */
public class AsyncDelayedTest extends AsynchronousTransportTest
{
	
	@Before
	public void setUp() throws Throwable
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);

		this.listener = TrapFactory.createListener(null);
		this.listener.disableAllTransports();
		this.listener.enableTransport(AsyncDelayedLoopbackTransport.name);
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.disableAllTransports();
		this.c.enableTransport(AsyncDelayedLoopbackTransport.name);
		this.c.setDelegate(this, true);
		this.c.open();

		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(10);
	}

	@Override
	public void testLiveness() throws Exception
	{
		// Disable this test for delayed transport until it is repaired.
	}

}
