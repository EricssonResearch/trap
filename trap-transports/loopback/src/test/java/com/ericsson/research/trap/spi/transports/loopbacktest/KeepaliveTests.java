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

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.transports.DisconnectingLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class KeepaliveTests implements OnAccept, OnData
{
	private TrapEndpoint	incomingEP;
	@SuppressWarnings("unused") // Useful for debugging
	private byte[]			rdata;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	//@Test
	public void testIsAlive() throws Exception
	{
		
		TrapListener listener = TrapFactory.createListener(null);
		listener.disableAllTransports();
		listener.enableTransport("loopback");
		
		listener.listen(this);
		
		String cfg = listener.getClientConfiguration();
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.setDelegate(this, true);
		c.disableAllTransports();
		c.enableTransport("loopback");
		c.open();
		
		// Accept
		
		TrapEndpoint s = this.accept();
		s.send("Hello".getBytes());
		
		// Now check liveness
		Assert.assertTrue(s.isAlive(0, true, false, 100).get());
		Assert.assertTrue(s.isAlive(100, false, false, 0).get());
		
		Thread.sleep(10);
		// This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
		Assert.assertFalse(s.isAlive(0, false, false, 0).get());
		
		Thread.sleep(10);
		c.send("Hello".getBytes());
		
		Thread.sleep(10);
		
		// This should succeed; the server has received a message within the last 10 ms.
		Assert.assertTrue(s.isAlive(20, false, false, 0).get());
		
		// This should fail; the client has not.
		Assert.assertFalse(c.isAlive(20, false, false, 0).get());
	}
	
	@Test
	public void testKeepaliveRecovery() throws Exception
	{
		
		TrapListener listener = TrapFactory.createListener(null);
		listener.disableAllTransports();
		listener.enableTransport("disconnectingloopback");
		
		listener.listen(this);
		
		String cfg = listener.getClientConfiguration();
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.setDelegate(this, true);
		c.disableAllTransports();
		c.enableTransport("disconnectingloopback");
		// Enable keepalives (1-sec interval)
		c.setKeepaliveInterval(1);
		c.setKeepaliveExpiry(1000);
		c.open();
		
		// Accept
		
		TrapEndpoint s = this.accept();
		
		while (c.getState() != TrapState.OPEN)
			Thread.sleep(10);
		
		s.send("Hello".getBytes());
		
		// Now check liveness
		Assert.assertTrue(s.isAlive(0, true, false, 100).get());
		Assert.assertTrue(s.isAlive(100, false, false, 0).get());
		
		// Drop the connection
		TrapTransport transport = c.getTransport("disconnectingloopback");
		((DisconnectingLoopbackTransport) transport).dropConnection();
		
		// Give the algorithm sufficient time to detect this
		Thread.sleep(2500);
		
		// This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
		Assert.assertFalse(s.isAlive(0, false, false, 0).get());
		
		Thread.sleep(10);
		c.send("Helloes".getBytes());
		
		Thread.sleep(10);
		
		// This should succeed; the server has received a message within the last 10 ms.
		Assert.assertTrue(s.isAlive(20, false, false, 0).get());
	}
	
	//@Test
	public void testKeepaliveConfig() throws Exception
	{
		
		TrapListener listener = TrapFactory.createListener(null);
		listener.disableAllTransports();
		listener.enableTransport("disconnectingloopback");
		//listener.setKeepaliveInterval(1);
		//listener.setKeepaliveExpiry(1000);
		
		listener.listen(this);
		
		String cfg = listener.getClientConfiguration();
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.setDelegate(this, true);
		c.disableAllTransports();
		c.enableTransport("disconnectingloopback");
		// Enable keepalives (1-sec interval)
		c.open();
		
		// Accept
		
		TrapEndpoint s = this.accept();
		
		while (c.getState() != TrapState.OPEN)
			Thread.sleep(10);
		
		s.send("Hello".getBytes());
		
		// Now check liveness
		Assert.assertTrue(s.isAlive(0, true, false, 100).get());
		Assert.assertTrue(s.isAlive(100, false, false, 0).get());
		
		// Drop the connection
		TrapTransport transport = c.getTransport("disconnectingloopback");
		((DisconnectingLoopbackTransport) transport).dropConnection();
		
		// Give the algorithm sufficient time to detect this
		Thread.sleep(2500);
		
		// This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
		Assert.assertFalse(s.isAlive(0, false, false, 0).get());
		
		Thread.sleep(10);
		c.send("Helloes".getBytes());
		
		Thread.sleep(10);
		
		// This should succeed; the server has received a message within the last 10 ms.
		Assert.assertTrue(s.isAlive(20, false, false, 0).get());
		
		// This should fail; the client has not.
		Assert.assertFalse(c.isAlive(20, false, false, 0).get());
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
		endpoint.setDelegate(this, true);
		this.notifyAll();
	}
	
	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		System.out.println(new String(data));
		this.rdata = data;
	}
	
}
