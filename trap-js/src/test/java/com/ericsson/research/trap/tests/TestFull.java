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

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;

import org.codehaus.jstestrunner.junit.JSTestSuiteRunner;
import org.codehaus.jstestrunner.junit.JSTestSuiteRunner.ResourceBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

@RunWith(JSTestSuiteRunner.class)
@ResourceBase("target")
public class TestFull
{
	private static TrapListener	listener;
	public static ConcurrentSkipListSet<TrapEndpoint>	endpoints	= new ConcurrentSkipListSet<TrapEndpoint>();

	@BeforeClass
	public static void test() throws Exception
	{
		
		/*ConsoleHandler h = new ConsoleHandler() {
			
			@Override
			public void publish(LogRecord arg0)
			{
				// TODO Auto-generated method stub
				super.publish(arg0);
				System.err.println(arg0.getMessage());
			}
			
		};
		h.setLevel(Level.FINEST);
		Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).setLevel(Level.FINEST);
		Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).addHandler(h);
		Logger.getLogger("com").setLevel(Level.FINEST);
		Logger.getLogger("com").addHandler(h);*/
	    
	    JDKLoggerConfig.initForPrefixes(Level.ALL);

		listener = TrapFactory.createListener(null);
		listener.getTransport("socket").configure("port", "41235");
		//					listener.getTransport("websocket").configure("host", "155.53.234.51");
		listener.getTransport("websocket").configure("port", "41234");
		listener.getTransport("http").configure("port", "8081");
		//listener.getTransport("http").configure("host", "155.53.234.118");
		listener.listen(new OnAccept() {
			
			@Override
			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
				try
				{
					endpoint.send("Welcome".getBytes());
				}
				catch (TrapException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				endpoints.add(endpoint);
				endpoint.setDelegate(new TrapEndpointDelegate() {
					
					@Override
					public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void trapFailedSending(@SuppressWarnings("rawtypes") Collection datas, TrapEndpoint endpoint, Object context)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
					{
						try
						{
							endpoint.send(data);
						}
						catch (TrapException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, false);
				
			}
		});
		
		// Apparently, there needs to be a callback on server setup? Did not know or anticipate this as needed...
		Thread.sleep(1000);
	}
	
	@AfterClass
	public static void close() throws Exception
	{
		listener.close();
		listener = null;
		endpoints = null;
	}
}
