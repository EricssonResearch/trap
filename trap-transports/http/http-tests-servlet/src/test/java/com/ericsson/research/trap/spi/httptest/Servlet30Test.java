package com.ericsson.research.trap.spi.httptest;

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

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import com.ericsson.research.trap.utils.ThreadPool;

@Ignore
@RunWith(Arquillian.class)
public class Servlet30Test implements OnAccept, OnData
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
	
	static
	{
		System.setProperty("jbossHome", "target/jboss-7.1.1.Final");
	}
	
	@Deployment
	@OverProtocol("Servlet 3.0")
	public static WebArchive createDeployment()
	{ 
		/*Logger jl = Logger.getLogger("");
		jl.setLevel(Level.FINEST);
		
		for (Handler h : jl.getHandlers())
			h.setLevel(Level.ALL);
		*/
		System.err.println("Creating class...");
		
		WebArchive archive = ShrinkWrap.create(WebArchive.class);
		//archive.addClass(AsynchronousTransportTest.class);
		//archive.addClass(HTTPServlet30.class);
		archive.addAsResource("log4j.properties", ArchivePaths.create("log4j.properties"));
		
		System.err.println("About to resolve...");
		
		File[] files = Maven.resolver().offline().loadPomFromFile("pom.xml").resolve("com.ericsson.research.trap.transports:http-server-servlet-3.0", "com.ericsson.research.trap.transports:http-client-sun").withTransitivity().asFile();
		archive.addAsLibraries(files);
		//archive.addAsWebInfResource(new File("src/test/resources", "web-3.0-test.xml"), "web.xml");
		//archive.setWebXML(new File("src/test/resources", "web-3.0-test.xml"));
		
		System.out.println(archive.toString(true));
		
		return archive;
	}
	
	@SuppressWarnings("unused")
	@BeforeClass
	public static void setLoggerLevel()
	{
		if (true)
			return;
		
		Logger jl = Logger.getLogger(""); 
		jl.setLevel(Level.FINEST);
		
		Logger.getLogger("sun.net").setLevel(Level.WARNING);
		
		jl.addHandler(new ConsoleHandler());
		for (Handler h : jl.getHandlers())
			h.setLevel(Level.ALL);
		
	}
	
	@Before
	public void setUp() throws Throwable
	{
		
		this.listener = TrapFactory.createListener(null);
		this.listener.disableAllTransports();
		this.listener.enableTransport("http");
		
		this.listener.configureTransport("http", "autoconfig.port", "14512");
		
		System.out.println(this.listener.getTransport("http").getClass());
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		
		System.out.println(cfg);
		
		this.c = TrapFactory.createClient(cfg, true);
		this.c.setDelegate(this, true);
		this.c.open();
		this.c.setAsync(false);
		
		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(10);
	}
	
	@After
	public void cleanUp() throws Throwable
	{
	}
	
	@Test(timeout = 10000000)
	public void testNormal() throws Exception
	{
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 10000000)
	public void testNormal2() throws Exception
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
	
	@Test(timeout = 20000)
	public void testAlwaysBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(1);
		this.s.setQueue(q);
		
		// TODO: OpenJDK has some hideous bug here where performance takes a stupidly large nosedive
		// when using Trap blocking transports.
		this.performMessageTests(20);
	}
	
	@Test(timeout = 10000)
	public void testByte() throws Exception
	{
		
		this.s.setQueueType(TrapEndpoint.REGULAR_BYTE_QUEUE);
		this.performMessageTests(1000);
	}
	
	@Test(timeout = 20000)
	public void testByteBlocking() throws Exception
	{
		LinkedByteBlockingMessageQueue q = new LinkedByteBlockingMessageQueue();
		q.setBlockingTimeout(1000);
		q.resize(128);
		this.s.setQueue(q);
		
		// TODO: OpenJDK has some hideous bug here where performance takes a stupidly large nosedive
		// when using Trap blocking transports.
		this.performMessageTests(20);
	}
	
	@Test(timeout = 10000)
	public void testIndefiniteBlocking() throws Exception
	{
		
		LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
		q.setBlockingTimeout(Long.MAX_VALUE);
		q.resize(1);
		this.s.setQueue(q);
		
		this.performMessageTests(20);
	}
	
	int	j	= 0;
	
	public void performMessageTests(final int messages) throws Exception
	{
		
		final byte[] bytes = "Helloes".getBytes();
		
		this.receiving = new AtomicInteger(1);
		this.receipts = new ConcurrentLinkedQueue<byte[]>();
		this.receivingCount = new AtomicInteger(0);
		this.processed = new AtomicInteger(0);
		this.messages = messages;
		
		for (int k = 0; k < this.receiving.get(); k++)
		{
			ThreadPool.executeFixed(new Runnable() {
				
				public void run()
				{
					for (int i = 0; i < (messages / Servlet30Test.this.receiving.get()); i++)
					{
						try
						{
							byte[] b = Servlet30Test.this.receive();
							if (b == null)
								continue;
							if (b.length == 0)
								System.err.println("We have a problem!");
							Assert.assertArrayEquals(b, bytes);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					Servlet30Test.this.receiving.decrementAndGet();
				}
			});
		}
		
		ThreadPool.executeCached(new Runnable() {
			
			public void run()
			{
				
				try
				{
					Servlet30Test.this.s.send(bytes);
					Thread.sleep(200);
				}
				catch (TrapException e1)
				{
					e1.printStackTrace();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				for (int i = 1; i < messages; i++)
				{
					try
					{
						Servlet30Test.this.s.send(bytes);
					}
					catch (Throwable e)
					{
						e.printStackTrace();
					}
				}
				try
				{
					Servlet30Test.this.s.send(bytes);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
				System.out.println("Done queueing");
			}
			
		});
		
		while (this.receiving.get() != 0)
			Thread.sleep(100);
		
		this.s.close();
		this.c.close();
		this.listener.close();
		
		this.s = null;
		this.c = null;
		this.listener = null;
		System.gc();
		
	}
	
	@Test
	public void testLiveness() throws Exception
	{
		
		this.s.send("Hello".getBytes());
		Thread.sleep(20);
		
		// Now check liveness
		Assert.assertTrue(this.s.isAlive(100, true, false, 0).get());
		Assert.assertTrue(this.s.isAlive(1000, false, false, 0).get());
		
		Thread.sleep(10);
		// This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
		Assert.assertFalse(this.s.isAlive(0, false, false, 0).get());
		
		Thread.sleep(10);
		this.c.send("Hello".getBytes());
		Thread.sleep(25);
		
		// This should succeed; the server has received a message within the last 25 ms.
		Assert.assertTrue(this.s.isAlive(45, false, false, 0).get());
		
		// This should fail; the client has not.
		Assert.assertFalse(this.c.isAlive(5, false, false, 0).get());
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
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, true);
		this.notify();
	}
	
	int	f	= 0;
	
	public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
	{
		if (data.length == 0)
			System.err.println("We have a problem!");
		
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
}
