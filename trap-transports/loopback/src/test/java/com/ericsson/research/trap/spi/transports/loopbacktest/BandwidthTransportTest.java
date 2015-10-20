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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapObject;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.queues.LinkedBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteBlockingMessageQueue;
import com.ericsson.research.trap.spi.transports.BandwidthLimitedLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.ThreadPool;

 @RunWith(Parameterized.class)
 @Ignore("This test is prone to race conditions in the TEST class itself.")
public class BandwidthTransportTest implements OnAccept, OnData
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
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.FINE);
	}
	
	// Run the test 10 times, just in case.
	@Parameterized.Parameters
	public static List<Object[]> data()
	{
		return Arrays.asList(new Object[100][0]);
	}
	
	@Before
	public void setUp() throws Throwable
	{
		
		this.listener = TrapFactory.createListener(null);
		this.listener.disableAllTransports();
		this.listener.enableTransport(BandwidthLimitedLoopbackTransport.name);
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.disableAllTransports();
		this.c.enableTransport(BandwidthLimitedLoopbackTransport.name);
		this.c.setDelegate(this, true);
		this.c.open();
		
		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(10);
	}
	
	public static final void main(String[] args) throws Throwable
	{
		Thread.sleep(7000);
		setLoggerLevel();
		BinaryTransportTest at = new BinaryTransportTest();
		at.setUp();
		at.performMessageTests(Integer.MAX_VALUE);
	}
	
	@Test(timeout = 10000)
	public void testNormal() throws Exception
	{
		this.performMessageTests(2000, 10);
	}
	
	@Test(timeout = 20000)
	public void testBlocking() throws Exception
	{
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(10);
		this.s.setQueue(q);
		
		this.performMessageTests(1000, 10);
	}
	
	@Test(timeout = 10000)
	public void testAlwaysBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(1000, 10);
	}
	
	@Test(timeout = 10000)
	public void testByte() throws Exception
	{
		
		this.s.setQueueType(TrapEndpoint.REGULAR_BYTE_QUEUE);
		this.performMessageTests(1000, 10);
	}
	
	@Test(timeout = 10000)
	public void testByteBlocking() throws Exception
	{
		LinkedByteBlockingMessageQueue q = new LinkedByteBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(128);
		this.s.setQueue(q);
		
		this.performMessageTests(1000, 10);
	}
	
	@Test(timeout = 10000)
	public void testIndefiniteBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(Long.MAX_VALUE);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(1000, 10);
	}
	
	@Test
	@Ignore
	public void testChunkingChannels() throws Exception
	{
		
		this.messages = 2;
		
		this.s.send(new byte[16 * 1024], 1, false);
		this.s.send(new byte[16], 2, false);
		
		byte[] r1 = this.receive();
		byte[] r2 = this.receive();
		System.out.println(r1.length);
		System.out.println(r2.length);
	}
	
	@Test
	public void testCompressedChannels() throws Exception
	{
		
		this.messages = 2;
		
		this.s.send(new byte[512 * 1024], 1, true);
		this.s.send(new byte[16], 1, false);
		
		byte[] r1 = this.receive();
		byte[] r2 = this.receive();
		System.out.println(r1.length);
		System.out.println(r2.length);
	}
	
	public void performMessageTests(final int messages, final int size) throws Exception
	{
		
		final byte[] bytes = new byte[size];
		
		this.receiving = new AtomicInteger(4);
		this.receipts = new ConcurrentLinkedQueue<byte[]>();
		this.receivingCount = new AtomicInteger(0);
		this.processed = new AtomicInteger(0);
		this.messages = messages;
		
		for (int k = 0; k < this.receiving.get(); k++)
		{
			ThreadPool.executeFixed(new Runnable() {
				
				public void run()
				{
					int i;
					for (i = 0; i < (messages / BandwidthTransportTest.this.receiving.get()); i++)
						try
						{
							byte[] b = BandwidthTransportTest.this.receive();
							if (b == null)
								continue;
							Assert.assertArrayEquals(b, bytes);
							BandwidthTransportTest.this.processed.incrementAndGet();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					System.out.println("Exiting with " + i + " messages read...");
					BandwidthTransportTest.this.receiving.decrementAndGet();
				}
			});
		}
		
		ThreadPool.executeCached(new Runnable() {
			
			public void run()
			{
				
				for (int i = 0; i < messages; i++)
				{
					try
					{
						BandwidthTransportTest.this.s.send(bytes);
					}
					catch (Throwable e)
					{
						e.printStackTrace();
					}
				}
				
			}
			
		});
		
		while (this.receiving.get() != 0)
			Thread.sleep(10);
		
		Assert.assertEquals(messages, this.processed.get());
		Assert.assertEquals(messages, this.receivingCount.get());
		
	}
	
	void certainSleep(long ms) throws InterruptedException
	{
		long start = System.currentTimeMillis();
		long end = start + ms;
		
		while (System.currentTimeMillis() < end)
			Thread.sleep(1);
	}
	
	long	m	= 0;
	
	@Test
	public void testLiveness() throws Exception
	{
		
		this.s.send("Hello".getBytes());
		this.s.setDelegate(this, false);
		
		// Now check liveness
		Assert.assertTrue(this.s.isAlive(100, true, false, 0).get());
		Assert.assertTrue(this.s.isAlive(1000, false, false, 0).get());
		
		this.certainSleep(1);
		// This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
		Assert.assertFalse(this.s.isAlive(0, false, false, 0).get());
		
		//Thread.sleep(3);
		this.receipts.clear();
		this.c.send(("Hello" + this.m++).getBytes());
		this.certainSleep(5);
		
		// This should succeed; the server has received a message within the last 25 ms.
		boolean recvd = this.s.isAlive(125, false, false, 0).get();
		if (!recvd)
		{
			if (!this.receipts.isEmpty())
			{
				Assert.fail();
			}
			Assert.fail();
		}
		
		// This should fail; the client has not.
		Assert.assertFalse(this.c.isAlive(0, false, false, 0).get());
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
