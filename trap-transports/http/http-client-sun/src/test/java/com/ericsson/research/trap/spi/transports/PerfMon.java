package com.ericsson.research.trap.spi.transports;

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
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;

public class PerfMon
{
	@Test
	public void main() throws Exception
	{
		//Logger jl = Logger.getLogger("");
		//jl.setLevel(Level.FINEST);
		//for (Handler h : jl.getHandlers())
		//	h.setLevel(Level.ALL);
		TrapClient client = TrapFactory.createClient("http://localhost:8080/wrt/_trap30/3d2aa01a1", false);
		long messages = 10000000;
		long initial = 500;
		final AtomicLong mid = new AtomicLong();
		final AtomicLong rid = new AtomicLong();
		final AtomicLong initialMessages = new AtomicLong(initial);
		final AtomicLong messagesToSend = new AtomicLong(messages - initialMessages.get());
		final AtomicLong recv = new AtomicLong(messages);
		final AtomicLong startTime = new AtomicLong();
		@SuppressWarnings("unused")
		final byte[] data = new byte[] { 'h', 'e', 'l', 'l', 'o' };
		
		client.setDelegate(new TrapEndpointDelegate() {
			
			@Override
			public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
			{
				System.err.println(newState);
				if (newState == TrapState.OPEN && oldState == TrapState.OPENING)
				{
					startTime.set(System.currentTimeMillis());
					
					@SuppressWarnings("unused")
					long i;
					while ((i = initialMessages.decrementAndGet()) >= 0)
					{
						try
						{
							endpoint.send(Long.toString(mid.getAndIncrement()).getBytes());
						}
						catch (Exception e)
						{
							
						}
					}
				}
			}
			
			@Override
			public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
			{
				@SuppressWarnings("unused")
				long i = 0;
				long parsed = Long.parseLong(new String(data));
				
				if (parsed != rid.getAndIncrement())
					System.out.println(parsed);
				if ((i = messagesToSend.decrementAndGet()) >= 0)
				{
					try
					{
						endpoint.send(Long.toString(mid.getAndIncrement()).getBytes());
						//endpoint.send(data);
					}
					catch (Exception e)
					{
						
					}
				}
				
				if (recv.decrementAndGet() <= 0)
				{
					synchronized (recv)
					{
						recv.notifyAll();
					}
				}
				
			}
		}, true);
		
		client.open();
		
		synchronized (recv)
		{
			while (recv.get() > 0)
			{
				System.out.println("Waiting as recv is " + recv.get());
				recv.wait(1000);
			}
		}
		
		System.out.println("Finished. Took " + (System.currentTimeMillis() - startTime.get()) + "ms");
		System.exit(0);
	}
	
}
