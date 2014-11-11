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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.TrapEndpointImpl;
import com.ericsson.research.trap.impl.TrapImplDebugPrinter;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

@RunWith(Parameterized.class)
public class HttpToWebsocketSwitchingTest implements OnAccept, OnData
{
	
	TrapEndpoint					incomingEP;
	TrapListener					listener;
	TrapClient						c;
	TrapEndpoint					s;
	
	ConcurrentLinkedQueue<byte[]>	receipts			= new ConcurrentLinkedQueue<byte[]>();
	AtomicInteger					receivingCount		= new AtomicInteger(0);
	AtomicInteger					processed			= new AtomicInteger(0);
	AtomicInteger					receiving;
	int								messages;
	
	boolean							connected			= false;
	
	byte[]							lastReceivedData	= null;
	byte[]							lastServerData		= null;
	
	public static void main(String[] args)
	{
	}
	
	@Parameterized.Parameters
	public static List<Object[]> data()
	{
		if ("true".equals(System.getProperty("trap.stresstest")))
			return Arrays.asList(new Object[500][0]);
		else
			return Arrays.asList(new Object[10][0]);
	}
	
	@BeforeClass
	public static void setLoggerLevel()
	{
		JDKLoggerConfig.initForPrefixes(Level.INFO);
        TrapImplDebugPrinter.start();
	}
	
	@Before
	public void setUp() throws Throwable
	{
		this.receipts = new ConcurrentLinkedQueue<byte[]>();
		
		this.listener = TrapFactory.createListener(null);
		this.listener.setOption(TrapTransport.OPTION_WARN_ADDRESS, "false");
		this.listener.disableAllTransports();
		this.listener.enableTransport("http");
		this.listener.enableTransport("websocket");
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		
		int start = cfg.indexOf("http://");
		int end = cfg.indexOf("\n", start);
		
		String httpUrl = cfg.substring(start, end);
		
		this.c = TrapFactory.createClient(httpUrl, true);
		this.c.setDelegate(this, true);
		this.c.disableAllTransports();
		this.c.enableTransport("http");
		this.c.enableTransport("websocket");
		this.c.setMaxActiveTransports(1);
		this.c.open();
		
		TrapImplDebugPrinter.addEndpoint((TrapEndpointImpl) this.c);
		// Accept
		
		this.s = this.accept();
        TrapImplDebugPrinter.addEndpoint((TrapEndpointImpl) this.s);
		this.s.setDelegate(new OnData() {
			
			@Override
			public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
			{
				HttpToWebsocketSwitchingTest.this.lastServerData = data;
			}
		}, true);
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(0, 500);
	}
	
	@After
	public void tearDown()
	{
		this.receipts.clear();
		this.c.close();
		this.s.close();
		this.c = null;
		this.s = null;
		this.listener.close();
		this.listener = null;
	}
	
	@Test(timeout = 10000)
	public void testNormal() throws Exception
	{
		
		// Now it's available. Who gets the message?
		
		TrapTransport http = this.c.getTransport("http");
		
		TrapTransport websocket = this.c.getTransport("websocket");
		// From this point on, we'll know which transport received our message(s).
		
		int i = 5;
		do
		{
			this.lastServerData = null;
			byte[] str = ("Hello" + i++).getBytes();
			this.c.send(str);
			
			while (this.lastServerData == null)
			{
				Thread.sleep(0, 500);
			}
		} while (http.isConnected() || !websocket.isConnected());
		
	}
	
	@Test(timeout = 10000)
	public void testHello() throws Exception
	{
		
		// Now it's available. Who gets the message?
		
		TrapTransport http = this.c.getTransport("http");
		
		TrapTransport websocket = this.c.getTransport("websocket");
		// From this point on, we'll know which transport received our message(s).
		
		int i = 5;
		do
		{
			this.lastServerData = null;
			byte[] str = ("Hello" + i++).getBytes();
			this.c.send(str);
			
			while (this.lastServerData == null)
			{
				Thread.sleep(0, 500);
			}
		} while (http.isConnected() || !websocket.isConnected());
		
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
		try
		{
			endpoint.send("Welcome to Trap".getBytes());
		}
		catch (TrapException e)
		{
			e.printStackTrace();
		}
		endpoint.setDelegate(this, true);
		this.notify();
	}
	
	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		this.lastReceivedData = data;
	}
	
	public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
	{
	}
	
	public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
	{
	}
}
