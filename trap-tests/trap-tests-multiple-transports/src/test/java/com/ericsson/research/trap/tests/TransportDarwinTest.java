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

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
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
import com.ericsson.research.trap.impl.ClientTrapEndpoint;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

/**
 * This test covers the case that multiple transports (http/ws) connect
 * simultaneously, then the client config drops one. The goal is to stress test
 * the scenario of a JS client connecting
 *
 * @author vladi
 */
@RunWith(Parameterized.class)
public class TransportDarwinTest implements OnAccept, OnData
{
	private static TransportDarwinTest	instance;
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
			return Arrays.asList(new Object[50][0]);
	}

	@BeforeClass
	public static void setLoggerLevel() throws TrapException
	{
		JDKLoggerConfig.initForPrefixes(Level.INFO);

		instance = new TransportDarwinTest();

		instance.listener = TrapFactory.createListener(null);
		instance.listener.disableAllTransports();
		instance.listener.enableTransport("http");
		instance.listener.enableTransport("websocket");
		instance.listener.listen(instance);
	}

	@Before
	public void setUp() throws Throwable
	{
		//Thread.sleep(500);
		//System.out.println("##################### NEW TEST");
		this.receipts = new ConcurrentLinkedQueue<byte[]>();

		String cfg = instance.listener.getClientConfiguration();

		this.c = TrapFactory.createClient(cfg, true);
		this.c.setDelegate(this, false);
		this.c.disableAllTransports();
		this.c.enableTransport("http");
		this.c.enableTransport("websocket");
		this.c.setMaxActiveTransports(1);
		((ClientTrapEndpoint) this.c).setTransportRecoveryTimeout(1500);
		this.c.open();
		// Accept

		this.s = instance.accept();
		this.s.setDelegate(new OnData() {

			@Override
			public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
			{
				//System.out.println("Received (server): " + new String(data));
				TransportDarwinTest.this.lastServerData = data;
			}
		}, false);

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
	}

	@AfterClass
	public static void nowReallyTearDown() throws InterruptedException
	{
		//Thread.sleep(100009);
		instance.listener.close();
		instance.listener = null;
	}

	static int	i	= 0;
	static int	failed	= 0;

	@Test
	//(timeout = 10000)
	public void testNormal() throws Exception
	{

		// Now it's available. Who gets the message?

		TrapTransport http = this.c.getTransport("http");

		TrapTransport websocket = this.c.getTransport("websocket");
		// From this point on, we'll know which transport received our message(s).
		int j = 0;
		do
		{
			this.lastServerData = null;
			byte[] str = ("Hello" + i++).getBytes();
			this.c.send(str);

			j++;

			if (j == 100)
			{
				System.out.println(++failed + " tests did not successfully switch to websockets");
				Assert.assertFalse(false);
				return;
			}

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
		endpoint.setDelegate(this, false);
		this.notify();
	}

	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		//System.out.println("Received: " + new String(data));
		this.lastReceivedData = data;
	}

	public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
	{
	}

	public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
	{
	}
}
