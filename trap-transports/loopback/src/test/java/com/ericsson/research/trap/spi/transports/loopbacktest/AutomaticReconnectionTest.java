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

import java.util.Collection;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapObject;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.transports.CanBeDisconnectedLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class AutomaticReconnectionTest implements OnAccept, OnData
{

	private TrapEndpoint	incomingEP;
	private byte[]			rcvData;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	@Test
	//(timeout = 10000)
	public void testConnect() throws Exception
	{
		TrapListener listener = TrapFactory.createListener(null);
		listener.disableAllTransports();
		listener.configureTransport("canbedisconnectedloopback", "enabled", "true");
		
		listener.listen(this);
		
		String cfg = listener.getClientConfiguration();
		TrapClient c = TrapFactory.createClient(cfg, true);
		c.disableAllTransports();
		c.configureTransport("canbedisconnectedloopback", "enabled", "true");
		c.setDelegate(this, false);
		c.open();
		
		// Accept
		
		TrapEndpoint s = this.accept();
		
		// Ensure that the correct transport has been loaded
		TrapTransport transport = c.getTransport("canbedisconnectedloopback");
		
		if ((transport == null) || !(transport instanceof CanBeDisconnectedLoopbackTransport))
			throw new Exception("The transports did not initialise correctly");
		
		// Wait for it to finish opening...
		while (c.getState() != TrapState.OPEN)
			Thread.sleep(10);

		s.send("Hello".getBytes());
		
		// Assert reception
		byte[] rcv = this.receive();
		Assert.assertArrayEquals(rcv, "Hello".getBytes());
		
		// Assert connection
		//Assert.assertTrue(s.isAlive(0, true, false, 100));
		
		Thread.sleep(10);

		System.out.println("Now disconnecting...");
		// Now throw away our connection!
		((CanBeDisconnectedLoopbackTransport) transport).dropConnection();
		
		// Give the implementation time to reconnect
		Thread.sleep(50);

		// This should be reconnected
		Assert.assertTrue(c.isAlive(10, true, false, 2000).get());
		Assert.assertTrue(s.isAlive(10, true, false, 200).get());

		byte[] stuff = "Areyouokay".getBytes();
		c.send(stuff);
		rcv = this.receive();
		Assert.assertArrayEquals(rcv, stuff);
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
	
	private synchronized byte[] receive() throws Exception
	{
		try
		{
			while(this.rcvData == null)
				this.wait();
			return this.rcvData;
		}
		finally
		{
			this.rcvData = null;
		}
	}

	public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, false);
		this.notifyAll();
	}
	
	public synchronized void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		this.rcvData = data;
		this.notifyAll();
	}
	
	public void trapObject(TrapObject object, int channel, TrapEndpoint endpoint, Object context)
	{
	}
	
	public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
	{
	}
	
	public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
	{
	}
}
