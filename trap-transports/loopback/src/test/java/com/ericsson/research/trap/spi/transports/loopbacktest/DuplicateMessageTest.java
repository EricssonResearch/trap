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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Assert;
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
import com.ericsson.research.trap.impl.queues.LinkedBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteBlockingMessageQueue;
import com.ericsson.research.trap.spi.transports.AsynchronousLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.ThreadPool;

@RunWith(Parameterized.class)
public class DuplicateMessageTest implements OnAccept, OnData
{
	
	TrapEndpoint					incomingEP;
	TrapListener					listener;
	TrapClient						c;
	TrapEndpoint					s;
	
	ConcurrentLinkedQueue<byte[]>	receipts		= new ConcurrentLinkedQueue<byte[]>();
	LinkedList<String>				sent			= new LinkedList<String>();
	ConcurrentSkipListSet<String>	received		= new ConcurrentSkipListSet<String>();
	AtomicInteger					receivingCount	= new AtomicInteger(0);
	AtomicInteger					processed		= new AtomicInteger(0);
	AtomicInteger					receiving;
	int								messages;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
        JDKLoggerConfig.initForPrefixes(Level.INFO);
	}
	
	@Parameterized.Parameters
	public static List<Object[]> data()
	{
		if ("true".equals(System.getProperty("trap.stresstest")))
			return Arrays.asList(new Object[100][0]);
		else
			return Arrays.asList(new Object[1][0]);
	}
	
	@Before
	public void setUp() throws Throwable
	{
		
		this.listener = TrapFactory.createListener(null);
		this.listener.disableAllTransports();
		this.listener.enableTransport(AsynchronousLoopbackTransport.name);
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.disableAllTransports();
		this.c.enableTransport(AsynchronousLoopbackTransport.name);
		this.c.setDelegate(this, true);
		this.c.open();
		
		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(5);
	}
	
	@After
	public void cleanUp() throws Exception
	{
		this.received.clear();
		this.sent.clear();
		this.receipts.clear();
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
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 20000)
	public void testBlocking() throws Exception
	{
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(10);
		this.s.setQueue(q);
		
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 10000)
	public void testAlwaysBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 10000)
	public void testByte() throws Exception
	{
		
		this.s.setQueueType(TrapEndpoint.REGULAR_BYTE_QUEUE);
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 10000)
	public void testByteBlocking() throws Exception
	{
		LinkedByteBlockingMessageQueue q = new LinkedByteBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(128);
		this.s.setQueue(q);
		
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 10000)
	public void testIndefiniteBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(Long.MAX_VALUE);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(1000);
	}
	
	public void performMessageTests(final int messages) throws Exception
	{
		
		this.sent = new LinkedList<String>();
		this.received = new ConcurrentSkipListSet<String>();
		this.receivingCount = new AtomicInteger(0);
		this.processed = new AtomicInteger(0);
		this.receiving = new AtomicInteger(1);
		this.receipts = new ConcurrentLinkedQueue<byte[]>();
		this.receivingCount = new AtomicInteger(0);
		this.processed = new AtomicInteger(0);
		this.messages = messages;
		
		new Thread(new Runnable() {
			
			public void run()
			{
				try
				{
					int i;
					for (i = 0; i < (messages * 2); i++)
					{
						try
						{
							byte[] b = DuplicateMessageTest.this.receive();
							if (b == null)
								continue;
							
							String s = new String(b);
							
							if (DuplicateMessageTest.this.received.contains(s))
							{
								Assert.fail("Received duplicate message with id: [" + s + "].");
							}
							
							DuplicateMessageTest.this.received.add(s);
							DuplicateMessageTest.this.processed.incrementAndGet();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					System.out.println("[" + DuplicateMessageTest.this + "] Exiting with " + i + " messages read...");
					DuplicateMessageTest.this.receiving.decrementAndGet();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
		
		// Generate the data...
		
		for (int i = 0; i < messages; i++)
			this.sent.add(Integer.toString(i));
		
		ThreadPool.executeCached(new Runnable() {
			
			public void run()
			{
				
				Iterator<String> it = DuplicateMessageTest.this.sent.iterator();
				
				while (it.hasNext())
					try
					{
						DuplicateMessageTest.this.s.send(it.next().getBytes());
					}
					catch (TrapException e)
					{
						e.printStackTrace();
					}
				
			}
			
		});
		
		while (this.receiving.get() != 0)
			Thread.sleep(5);
		
		Assert.assertEquals(messages, this.processed.get());
		Assert.assertEquals(messages, this.receivingCount.get());
		
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
			Thread.sleep(0, 500);
		}
		return b;
	}
	
	public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, true);
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
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
	}
}
