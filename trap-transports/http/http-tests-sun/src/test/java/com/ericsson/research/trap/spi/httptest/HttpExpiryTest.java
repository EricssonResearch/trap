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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
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
import com.ericsson.research.trap.impl.ClientTrapEndpoint;
import com.ericsson.research.trap.spi.transports.ClientHttpTransport;

/*
 * Tests the HTTP expiry timers
 */
@Ignore // This test is not actually testing anything meaningful.
// Keepalives and other items will destroy it.
public class HttpExpiryTest implements OnAccept, OnData
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
	private TimedHttpTransport		timedHttpTransport;
	boolean							expiredInTime	= false;
	
	@BeforeClass
	public static void setLoggerLevel()
	{
		Logger jl = Logger.getLogger("");
		jl.setLevel(Level.FINEST);
		for (Handler h : jl.getHandlers())
			h.setLevel(Level.ALL);
	}
	
	@Before
	public void setUp() throws Throwable
	{
		this.testBegun = false;
		this.listener = TrapFactory.createListener(null);
		
		this.listener.listen(this);
		
		String cfg = this.listener.getClientConfiguration();
		this.c = TrapFactory.createClient(cfg, true);
		this.c.setDelegate(this, true);
		this.timedHttpTransport = new TimedHttpTransport();
		((ClientTrapEndpoint) this.c).removeTransport(this.c.getTransport("http"));
		((ClientTrapEndpoint) this.c).addTransport(this.timedHttpTransport);
		this.c.open();
		this.c.setAsync(false);
		
		// Accept
		
		this.s = this.accept();
		
		while (this.c.getState() != TrapState.OPEN)
			Thread.sleep(10);
		
		this.expiredInTime = false;
	}
	
	@Test(timeout = 10000)
	public void testNormal() throws Exception
	{
		this.performTimeoutTest(1000);
	}
	
	@Test(timeout = 10000)
	public void testStillHanging() throws Exception
	{
		this.timeout = 1000;

		this.expiredInTime = false;
		this.testBegun = true;
		this.s.send(new byte[] { 42 });
		
		Thread.sleep(1500);
		Assert.assertTrue("Expired prematurely", this.expiredInTime);
	}

	
	int	j	= 0;
	private boolean	testBegun;
	private long	timeout;
	
	public synchronized void performTimeoutTest(final long timeout) throws Exception
	{
		
		this.timeout = timeout;
		long finalTime = System.currentTimeMillis();
		finalTime += timeout + 1000;

		this.expiredInTime = false;
		this.testBegun = true;
		this.s.send(new byte[] { 42 });
		
		while ((System.currentTimeMillis() < finalTime) && !this.expiredInTime)
			Thread.sleep(200);
		
		if (!this.expiredInTime)
			Assert.fail("Did not end in time");

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
	
	class TimedHttpTransport extends ClientHttpTransport
	{
		
		long	startTime	= 0;
		long	endTime		= 0;

		protected HttpURLConnection openConnection(URL u) throws IOException
		{
			if (HttpExpiryTest.this.testBegun)
			{
				this.expirationDelay = HttpExpiryTest.this.timeout;
				if (this.startTime == 0)
				{
					this.startTime = System.currentTimeMillis();
				}
				else
				{
					this.endTime = System.currentTimeMillis();
					
					System.out.println((this.endTime - this.startTime));
					HttpExpiryTest.this.expiredInTime = (this.endTime - this.startTime) <= (HttpExpiryTest.this.timeout + 200); // 200ms tolerance for failure
				}
			}
			return super.openConnection(u);
		}
		
	}
}
