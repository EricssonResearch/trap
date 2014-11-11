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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.transports.DisconnectingLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class DisconnectedTransportRecovery
{
	
	TrapListener	tl;
	TrapEndpoint	s;
	TrapClient		c;
	
	boolean			done	= false;
	boolean			success	= true;
	AtomicInteger	i;
	
	byte[] intToByte(int i)
	{
		return Integer.valueOf(i).toString().getBytes();
	}
	
	int byteToInt(byte[] b)
	{
		return Integer.parseInt(new String(b));
	}

	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	@Test
	public void testTenMessages() throws Exception
	{
		this.i = new AtomicInteger();
		
		// Set up an echo server.
		this.tl = TrapFactory.createListener(null);
		this.tl.disableAllTransports();
		this.tl.configureTransport("disconnectingloopback", "enabled", "true");
		this.tl.listen(new OnAccept() {
			
			@Override
			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
				
				DisconnectedTransportRecovery.this.s = endpoint;
				DisconnectedTransportRecovery.this.s.setDelegate(new TrapEndpointDelegate() {
					
					@Override
					public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
					{
						
					}
					
					@Override
					public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
					{
						
					}
					
					@Override
					public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
					{
						try
						{
							System.out.println("Server received " + DisconnectedTransportRecovery.this.byteToInt(data));
							DisconnectedTransportRecovery.this.s.send(data);
						}
						catch (TrapException e)
						{
							e.printStackTrace();
						}
					}
				}, true);
				
			}
		});
		
		// With the echo server set up, we can set up the client.
		String cfg = this.tl.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.disableAllTransports();
		this.c.configureTransport("disconnectingloopback", "enabled", "true");
		this.c.setKeepaliveInterval(10);
		
		this.c.setDelegate(new TrapEndpointDelegate() {
			
			@Override
			public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
			{
				
				if (newState == TrapState.OPEN && oldState == TrapState.OPENING)
				{
					try
					{
						DisconnectedTransportRecovery.this.c.send(DisconnectedTransportRecovery.this.intToByte(DisconnectedTransportRecovery.this.i.incrementAndGet()));
					}
					catch (TrapException e)
					{
						e.printStackTrace();
					}
				}
				
			}
			
			@Override
			public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
			{
				
			}
			
			@Override
			public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
			{
				int received = DisconnectedTransportRecovery.this.byteToInt(data);
				System.err.println(received);
				
				if (DisconnectedTransportRecovery.this.i.get() < received)
				{
					DisconnectedTransportRecovery.this.success = false;
					DisconnectedTransportRecovery.this.done = true;
				}
				else if (DisconnectedTransportRecovery.this.i.get() >= 100)
				{
					DisconnectedTransportRecovery.this.done = true;
				}
				else
				{
					try
					{
						DisconnectedTransportRecovery.this.c.send(DisconnectedTransportRecovery.this.intToByte(DisconnectedTransportRecovery.this.i.incrementAndGet()));
					}
					catch (TrapException e)
					{
						e.printStackTrace();
					}
				}
				
				if (DisconnectedTransportRecovery.this.i.get() == 50)
				{
					
					TrapTransport transport;
					try
					{
						transport = DisconnectedTransportRecovery.this.c.getTransport("disconnectingloopback");
						((DisconnectingLoopbackTransport) transport).dropConnection();
					}
					catch (TrapException e)
					{
						e.printStackTrace();
					}
				}
				
			}
		}, true);
		
		this.done = false;
		this.c.open();
		
		while (!this.done)
			Thread.sleep(10);
		
		Assert.assertTrue(this.success);
		
	}
}
