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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class BasicTrapTest implements OnAccept, OnData
{
	
	private TrapEndpoint	incomingEP;
	private byte[]			rcvData;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	@Test (timeout = 10000)
	public void testConnect() throws Exception
	{
		
		// Creates a TrapListener using the default configuration.
		// "null" in this case will let all settings come from source.
		TrapListener listener = TrapFactory.createListener(null);
		
		// Ensure we're using the synchronous transport for this test. Synchronous transports
		// are not recommended for regular usage, but this is a very specific test on
		// basic functionality.
		listener.disableAllTransports();
		listener.enableTransport("loopback");
		
		// Asks the listener to begin listening and to send callbacks to this object
		// We have no context, so it is null. Note that this will cause any
		// autoconfiguration parameters to kick in (e.g. a TCP listener on port 0
		// would decide to listen on the available port 41392, and the Loopback
		// Transport will create a new ID for this session)
		//
		// Note that this also sets us as the listener's delegate.
		listener.listen(this);
		
		// This line asks the listener to generate the configuration file for the client.
		// The cfg string contains all configuration the client needs to connect to
		// the listener.
		String cfg = listener.getClientConfiguration();
		
		// The client is initialised with the configuration from the server.
		// This includes configurations for all enabled transports.
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.disableAllTransports();
		c.enableTransport("loopback");
		
		// Sets us as the client delegate.
		c.setDelegate(this, false);
		
		// Connect
		c.open();
		
		// Accept (this is synchronous, see below)
		TrapEndpoint s = this.accept();
		
		// Send some data
		byte[] data = "ET Phone Home".getBytes();
		
		s.send(data);
		
		// The actual data would arrive as rcvData
		while (this.rcvData == null)
			Thread.sleep(10);
		
		Assert.assertArrayEquals(data, this.rcvData);
		
	}
	
	@Test(timeout = 10000)
	public void testDisconnect() throws Exception
	{
		
		TrapListener listener = TrapFactory.createListener(null);
		listener.disableTransport("asyncloopback");
		listener.enableTransport("loopback");
		
		listener.listen(this);
		
		String cfg = listener.getClientConfiguration();
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.setDelegate(this, false);
		c.enableTransport("loopback");
		c.open();
		
		// Accept
		
		TrapEndpoint s = this.accept();
		
		while (c.getState() != TrapState.OPEN)
			Thread.sleep(5);
		
		s.send("Hello".getBytes());
		c.send("Hello".getBytes());
		
		c.close();
		
		while (c.getState() != TrapState.CLOSED)
			Thread.sleep(5);
		
		while (s.getState() != TrapState.CLOSED)
			Thread.sleep(5);
		
		Assert.assertEquals(c.getState(), TrapState.CLOSED);
		Assert.assertEquals(s.getState(), TrapState.CLOSED);
	}
	
	private synchronized TrapEndpoint accept() throws InterruptedException
	{
		try
		{
			while (this.incomingEP == null)
				this.wait();
			
			return this.incomingEP;
		}
		finally
		{
			this.incomingEP = null;
		}
	}
	
	public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, false);
		this.notifyAll();
	}
	
	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		this.rcvData = data;
	}
	
}
