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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.queues.LinkedBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedMessageQueue;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.transports.SometimesAvailableLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.ThreadPool;

public class SporadicTransportTest implements OnAccept, OnData
{
	
	private TrapEndpoint					incomingEP;
	private TrapListener					listener;
	private TrapClient						c;
	private TrapEndpoint					s;
	
	private ConcurrentLinkedQueue<byte[]>	receipts		= new ConcurrentLinkedQueue<byte[]>();
	int										receivingCount	= 0;
	private boolean							receiving;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	@Before
	public void setUp() throws Throwable
	{
		
		this.listener = TrapFactory.createListener(null);
		this.listener.disableAllTransports();
		this.listener.enableTransport(SometimesAvailableLoopbackTransport.name);
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.disableAllTransports();
		this.c.enableTransport(SometimesAvailableLoopbackTransport.name);
		this.c.setDelegate(this, true);
		this.c.open();
		
		// Accept
		
		this.s = this.accept();
		
	}
	
	@Test(timeout = 10000)
	public void testNormal() throws Exception
	{
		this.performMessageTests(20);
	}
	
	@Test(timeout = 10000, expected = TrapException.class)
	public void testQueueOverflow() throws Exception
	{
		LinkedMessageQueue q = new LinkedMessageQueue();
		q.resize(10);
		this.s.setQueue(q);
		
		this.performMessageTests(40);
	}
	
	@Test(timeout = 20000)
	public void testBlocking() throws Exception
	{
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(10);
		this.s.setQueue(q);
		
		this.performMessageTests(100);
	}
	
	@Test(timeout = 10000)
	public void testAlwaysBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(100);
	}
	
	@Test(timeout = 10000)
	public void testByte() throws Exception
	{
		
		this.s.setQueueType(TrapEndpoint.REGULAR_BYTE_QUEUE);
		this.performMessageTests(100);
	}
	
	@Test(timeout = 10000)
	public void testByteBlocking() throws Exception
	{
		LinkedByteBlockingMessageQueue q = new LinkedByteBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(128);
		this.s.setQueue(q);
		
		this.performMessageTests(100);
	}
	
	@Test(timeout = 10000)
	public void testIndefiniteBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(Long.MAX_VALUE);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(100);
	}
	
	//	@Test
	public void testSporadicTimeout() throws Exception
	{
		// We should time out on at least one of the following
		for (int i = 0; i < 70; i++)
			if (!this.c.isAlive(0, true, false, i).get())
			{
				//System.out.println(i);
				Assert.assertTrue(i > 0);
			}
	}
	
	public void performMessageTests(final int messages) throws Exception
	{
		
		// Ensure that the correct transport has been loaded
		TrapTransport transport = this.c.getTransport(SometimesAvailableLoopbackTransport.name);
		
		if ((transport == null) || !(transport instanceof SometimesAvailableLoopbackTransport))
			throw new Exception("The transports did not initialise correctly");
		
		final byte[] bytes = "Helloes".getBytes();
		for (int i = 0; i < messages; i++)
		{
			this.s.send(bytes);
		}
		
		this.receiving = true;
		
		ThreadPool.executeCached(new Runnable() {
			
			public void run()
			{
				for (int i = 0; i < messages; i++)
					try
					{
						Assert.assertArrayEquals(SporadicTransportTest.this.receive(), bytes);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				SporadicTransportTest.this.receiving = false;
			}
		});
		
		while (this.receiving == true)
			Thread.sleep(100);
		
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
		while (this.receipts.peek() == null)
			this.wait();
		return this.receipts.poll();
	}
	
	public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, true);
		this.notifyAll();
	}
	
	public synchronized void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		//System.out.println(new String(data));
		this.receipts.add(data);
		this.notifyAll();
	}
}
