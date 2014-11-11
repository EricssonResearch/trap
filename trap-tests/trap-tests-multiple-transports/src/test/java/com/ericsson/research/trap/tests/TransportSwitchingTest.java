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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.TrapEndpointImpl;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

public class TransportSwitchingTest implements OnAccept, OnData
{
	
	TrapEndpoint					incomingEP;
	TrapListener					listener;
	TrapClient						c;
	TrapEndpoint					s;
	
	ConcurrentLinkedQueue<byte[]>	receipts		= new ConcurrentLinkedQueue<byte[]>();
	AtomicInteger					receivingCount	= new AtomicInteger(0);
	AtomicInteger					processed		= new AtomicInteger(0);
	AtomicInteger					receiving;
	int								messages;
	
	boolean							connected		= false;
	private TrapTransport			lastReceivedTransport;
	
	public static void main(String[] args)
	{
	}

	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	@Before
	public void setUp() throws Throwable
	{
		this.listener = TrapFactory.createListener(null);
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();

		this.c = TrapFactory.createClient(cfg, true);
		this.c.setDelegate(this, false);
		this.c.open();
		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(10);
	}
	
	@Test(timeout = 10000)
	@Ignore("This test is invalid; we can't test Trap like this...")
	public void testNormal() throws Exception
	{
		
		TrapTransportDelegate serverDelegate = new TrapTransportDelegate() {
			
			@Override
			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
				((TrapTransportDelegate) TransportSwitchingTest.this.s).ttStateChanged(newState, oldState, transport, context);
			}
			
			@Override
			public void ttMessagesFailedSending(@SuppressWarnings("rawtypes") Collection messages, TrapTransport transport, Object context)
			{
				((TrapTransportDelegate) TransportSwitchingTest.this.s).ttMessagesFailedSending(messages, transport, context);
			}
			
			@Override
			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
				System.out.println("Server received message on : " + transport.getTransportName());
				System.out.println(message.getOp());
				System.out.println(new String(message.getData()));
				TransportSwitchingTest.this.lastReceivedTransport = transport;
				((TrapTransportDelegate) TransportSwitchingTest.this.s).ttMessageReceived(message, transport, context);
			}

			@Override
			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
				((TrapTransportDelegate) TransportSwitchingTest.this.s).ttMessageSent(message, transport, context);
			}

            @Override
            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
		};

		// Now hijack.
		TrapTransport socket = this.c.getTransport("socket");

		TrapTransport websocket = this.c.getTransport("websocket");
		// From this point on, we'll know which transport received our message(s).
		
		for (Object t : this.s.getTransports())
			((TrapTransport) t).setTransportDelegate(serverDelegate, null);
		
		Thread.sleep(100);

		System.out.println("Socket connection status: " + socket.isConnected());
		System.out.println("WebSocket connection status: " + websocket.isConnected());
		
		
		socket.setTransportPriority(Integer.MIN_VALUE);
		
		((TrapEndpointImpl) this.c).sort();

		System.out.println("Socket is available...");
		Thread.sleep(10);
		
		for (Object t : this.s.getTransports())
			((TrapTransport) t).setTransportDelegate(serverDelegate, null);

		// Now it's available. Who gets the message?
		this.lastReceivedTransport = null;
		this.c.send("Hello".getBytes());
		
		while (this.lastReceivedTransport == null)
		{
			Thread.sleep(100);
			System.out.println(this.lastReceivedTransport);
		}
		
		Assert.assertEquals(socket.getTransportName(), this.lastReceivedTransport.getTransportName());
		
		socket.setTransportPriority(0);
		websocket.setTransportPriority(Integer.MIN_VALUE);
		
		((TrapEndpointImpl) this.c).sort();

		System.out.println("Socket is available...");
		this.lastReceivedTransport = null;
		Thread.sleep(10);
		
		for (Object t : this.s.getTransports())
			((TrapTransport) t).setTransportDelegate(serverDelegate, null);

		// Now it's available. Who gets the message?
		this.c.send("Hello".getBytes());
		
		while (this.lastReceivedTransport == null)
		{
			Thread.sleep(100);
			System.out.println(this.lastReceivedTransport);
		}
		
		Assert.assertEquals(websocket.getTransportName(), this.lastReceivedTransport.getTransportName());
	}
	
	protected synchronized TrapEndpoint accept() throws InterruptedException
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
	
	@SuppressWarnings("unused")
	private byte[] receive() throws Exception
	{
		byte[] b = null;
		while (((b = this.receipts.poll()) == null) && (this.processed.get() != this.messages))
		{
			Thread.sleep(1);
		}
		return b;
	}
	
	public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, false);
		this.notify();
	}
	
	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		this.receivingCount.incrementAndGet();
		this.receipts.add(data);
		
		if (this.receipts.size() > 10000)
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
	}
	
	public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
	{
	}
	
	public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
	{
	}
}
