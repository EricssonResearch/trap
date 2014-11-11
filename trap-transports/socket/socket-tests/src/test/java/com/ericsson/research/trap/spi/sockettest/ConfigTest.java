package com.ericsson.research.trap.spi.sockettest;

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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;

public class ConfigTest implements OnAccept
{
	private TrapListener	listener;

	@After
	public void cleanup()
	{
		if (this.listener != null)
			this.listener.close();
		this.listener = null;
	}
	
	@Test
	public void testHostPortConfig() throws Exception
	{
		String config = "trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port=51442";
		this.listener = TrapFactory.createListener(config);
		this.listener.listen(this);
		
		Thread.sleep(100);
		String configuration = this.listener.getClientConfiguration();
		
		Assert.assertTrue(configuration.contains("trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port = 51442"));
		
	}
	
	/*
	 * Test that the autoconf host overrides regular host
	 */
	@Test
	public void testAutoconfHostnamePortConfig() throws Exception
	{
		String config = "trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port=51442\ntrap.transport.socket.autoconfig.host=ericsson.com";
		this.listener = TrapFactory.createListener(config);
		this.listener.listen(this);
		
		Thread.sleep(100);
		String configuration = this.listener.getClientConfiguration();
		
		Assert.assertTrue(configuration.contains("trap.transport.socket.host = ericsson.com\ntrap.transport.socket.port = 51442"));
	}
	
	/*
	 * Test that the autoconf port overrides regular port
	 */

	@Test
	public void testAutoconfPortConfig() throws Exception
	{
		String config = "trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port=51442\ntrap.transport.socket.autoconfig.port=1000";
		this.listener = TrapFactory.createListener(config);
		this.listener.listen(this);
		
		Thread.sleep(100);
		String configuration = this.listener.getClientConfiguration();
		
		Assert.assertTrue(configuration.contains("trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port = 1000"));
	}
	
	/*
	 * Test that the autoconf overrides regular config
	 */

	@Test
	public void testAutoconfConfig() throws Exception
	{
		String config = "trap.transport.socket.host = 127.0.0.1\ntrap.transport.socket.port=51442\ntrap.transport.socket.autoconfig.port=1000\ntrap.transport.socket.autoconfig.host=ericsson.com";
		this.listener = TrapFactory.createListener(config);
		this.listener.listen(this);
		
		Thread.sleep(100);
		String configuration = this.listener.getClientConfiguration();
		
		Assert.assertTrue(configuration.contains("trap.transport.socket.host = ericsson.com\ntrap.transport.socket.port = 1000"));
	}

	@Override
	public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		// TODO Auto-generated method stub

	}
}
