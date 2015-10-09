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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.spi.transports.AsyncDelayedLoopbackTransport;
import com.ericsson.research.trap.spi.transports.AsynchronousLoopbackTransport;
import com.ericsson.research.trap.spi.transports.BandwidthLimitedLoopbackTransport;
import com.ericsson.research.trap.spi.transports.BinaryLoopbackTransport;
import com.ericsson.research.trap.spi.transports.CanBeDisconnectedLoopbackTransport;
import com.ericsson.research.trap.spi.transports.DisconnectingLoopbackTransport;
import com.ericsson.research.trap.spi.transports.ReorderingLoopbackTransport;
import com.ericsson.research.trap.spi.transports.SometimesAvailableLoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

@RunWith(Parameterized.class)
public class LoopbackConnectionPerformanceTest implements OnAccept
{

	TrapEndpoint										incomingEP;
	static TrapListener									listener;
	static TrapClient									c;
	static TrapEndpoint									s;
	private static LoopbackConnectionPerformanceTest	instance;
	private static String								cfg;

	ConcurrentLinkedQueue<byte[]>						receipts		= new ConcurrentLinkedQueue<byte[]>();
	AtomicInteger										receivingCount	= new AtomicInteger(0);
	AtomicInteger										processed		= new AtomicInteger(0);
	AtomicInteger										receiving;
	int													messages;

	@BeforeClass
	public static void setUp() throws Throwable
	{
	    
	    //Thread.sleep(20000);
	    
		JDKLoggerConfig.initForPrefixes(Level.INFO);
		instance = new LoopbackConnectionPerformanceTest();

		listener = TrapFactory.createListener(null);
		listener.disableAllTransports();
		listener.enableTransport(AsynchronousLoopbackTransport.name);

		listener.listen(instance);
		cfg = listener.getClientConfiguration();
        cfg += "trap.transport." + AsyncDelayedLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + BandwidthLimitedLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + BinaryLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + CanBeDisconnectedLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + DisconnectingLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + ReorderingLoopbackTransport.name + ".enabled = false\n";
        cfg += "trap.transport." + SometimesAvailableLoopbackTransport.name + ".enabled = false\n";

	}

	@Parameterized.Parameters
	public static List<Object[]> data()
	{
		if ("true".equals(System.getProperty("trap.stresstest")))
			return Arrays.asList(new Object[100][0]);
		else
			return Arrays.asList(new Object[10][0]);
	}

	@Test(timeout = 1000000)
	public void testNormal() throws Exception
	{

		for (int i = 0; i < 1000; i++)
		{
			c = TrapFactory.createClient(cfg, true);
			c.disableAllTransports();
			c.enableTransport(AsynchronousLoopbackTransport.name);
			c.setDelegate(instance, true);
			c.setAsync(true);
			c.open();

			// Accept

			s = instance.accept();
			s.setAsync(true);

			while (c.getState() != TrapState.OPEN)
				Thread.yield();

			c.close();
			s.close();
		}
	}

	protected TrapEndpoint accept() throws InterruptedException
	{
		try
		{
			while (this.incomingEP == null)
				Thread.yield();

			return this.incomingEP;
		}
		finally
		{
			this.incomingEP = null;
		}
	}

	public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
	{
		//System.out.println("Incoming Connection");
		this.incomingEP = endpoint;
		endpoint.setDelegate(this, true);
	}
}
