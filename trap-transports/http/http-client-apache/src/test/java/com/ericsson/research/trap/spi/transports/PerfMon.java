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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Ignore;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;

@Ignore
public class PerfMon
{
	
	static int	f	= 0;
	
	public static void main(String[] args) throws Exception
	{
		Logger jl = Logger.getLogger("");
		jl.setLevel(Level.FINEST);
		for (Handler h : jl.getHandlers())
			h.setLevel(Level.ALL); 
		TrapClient client = TrapFactory.createClient("http://localhost:8080/wrt/_trap30/8eb1904c4", false);
		long messages = 100000000;
		long initial = 2000;
		final AtomicLong initialMessages = new AtomicLong(initial);
		final AtomicLong messagesToSend = new AtomicLong(messages - initialMessages.get() + 1);
		final AtomicLong recv = new AtomicLong(messages);
		final AtomicLong startTime = new AtomicLong();
		final byte[] data = new byte[] { 'h', 'e', 'l', 'l', 'o' };
		
		client.setDelegate(new TrapEndpointDelegate() {
			
			@Override
			public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
			{
				System.err.println(newState);
				if (newState == TrapState.OPEN && oldState == TrapState.OPENING)
				{
					startTime.set(System.currentTimeMillis());
					
					while (initialMessages.decrementAndGet() > 0)
					{
						try
						{
							Thread.sleep(10);
							endpoint.send(data);
						}
						catch (Exception e)
						{
							
						}
					}
				}
				
				if (newState == TrapState.SLEEPING)
				{
					System.err.println("Why???");
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
				if ((++f % 10000) == 0)
					System.out.println(new String(data));
				@SuppressWarnings("unused")
				long i = 0;
				if ((i = messagesToSend.decrementAndGet()) > 0)
				{
					try
					{
						//endpoint.send(Long.toString(i).getBytes());
						endpoint.send(data);
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
			while (recv.decrementAndGet() > 0)
			{
				System.out.println("Waiting as recv is " + recv.get());
				recv.wait();
			}
		}
		
		System.out.println("Finished. Took " + (System.currentTimeMillis() - startTime.get()) + "ms");
		System.exit(0);
	}
	
}
