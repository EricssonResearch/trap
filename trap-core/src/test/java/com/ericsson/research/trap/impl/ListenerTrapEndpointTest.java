package com.ericsson.research.trap.impl;

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.transports.AbstractTransport;

public class ListenerTrapEndpointTest
{
	ListenerTrapEndpoint	lte;
	TrapEndpoint			tempEp	= null;
	boolean					tempCond	= false;

	@Before
	public void setUp() throws Exception
	{
		// Set up the proper constants for this test.
		// We're replacing some Trap constants to get a workeable runtime.
		this.lte = new ListenerTrapEndpoint();
		this.lte.configure("");
		this.tempEp = null;
		this.tempCond = false;
	}

	@After
	public void tearDown() throws Exception
	{
	}

	static void setFinalStatic(Field field, Object newValue) throws Exception
	{
		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	@Test(expected = IllegalStateException.class)
	public void testForbiddenMethods() throws Exception
	{
		this.lte.reconnect(0);
	}

	/*
	 * This test ensures that, when presented with illegal invalid messages, the listener refuses
	 * to accept anything except OPEN.
	 */
	@Test(timeout = 10000)
	public void testInvalidInitialMessage() throws SecurityException, NoSuchFieldException, Exception
	{

		TrapTransport t = new AbstractTransport() {
			
			public boolean canConnect()
			{
				return true;
			}
			
			public boolean canListen()
			{
				return true;
			}


			public String getTransportName()
			{
				return "foo";
			}

			public String getProtocolName()
			{
				return "lte";
			}

			@Override
			public void fillAuthenticationKeys(HashSet<String> keys)
			{
			}
			@Override
			public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
			{
			}

			@Override
			protected void internalDisconnect()
			{
			}

			@Override
			protected void internalConnect() throws TrapException
			{
			}

			@Override
			public void connect()
			{
				this.state = TrapTransportState.AVAILABLE;
			}

			public void flushTransport()
			{
				// TODO Auto-generated method stub
				
			}
		};

		t.setTransportDelegate(new TrapTransportDelegate() {

			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
			}

			public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
			{
			}

			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
			}

			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
			}

            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
            }
		}, null);

		this.lte.listen(new OnAccept() {

			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
			}
		});

		// This test SHOULD fail when we receive an i that exceeds the total allowed.
		for (int i = 2; i < 32; i++)
		{
			TrapMessage m;

			try
			{
				m = new TrapMessageImpl().setOp(Operation.getType(i));
			}
			catch (UnsupportedOperationException e)
			{
				// This is fine; I sometimes ask for operations that don't exist.
				continue;
			}

			t.connect();
			this.lte.ttMessageReceived(m, t, null);
			Assert.assertTrue(t.getState() == TrapTransportState.DISCONNECTING);

		}

	}

	/**
	 * This test will ensure that transports added to a listener and then
	 * disconnect are properly cleaned up.
	 */
	@Test()
	@Ignore
	public void testTransportAllocDealloc() throws Exception
	{
		TrapTransport t = new AbstractTransport() {
			
			public boolean canConnect()
			{
				return true;
			}
			
			public boolean canListen()
			{
				return true;
			}


			public String getTransportName()
			{
				return "foo";
			}

			public String getProtocolName()
			{
				return "lte";
			}

			@Override
			public void fillAuthenticationKeys(HashSet<String> keys)
			{
			}

			@Override
			public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
			{
			}

			@Override
			protected void internalDisconnect()
			{
				this.setState(TrapTransportState.DISCONNECTED);
			}

			@Override
			protected void internalConnect() throws TrapException
			{
			}

			@Override
			public void connect()
			{
				this.state = TrapTransportState.AVAILABLE;
			}

			@Override
			protected void finalize() throws Throwable
			{
				ListenerTrapEndpointTest.this.tempCond = true;
				super.finalize();
			}

			public void flushTransport()
			{
				// TODO Auto-generated method stub
				
			}

		};

		t.setTransportDelegate(new TrapTransportDelegate() {

			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
			}

			public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
			{
			}

			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
			}

			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
			}

            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
            }
		}, null);

		this.lte.listen(new OnAccept() {

			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
			}

		});

		t.connect();

		// Now add the new server connection properly.
		this.lte.ttsIncomingConnection(t, null, null);

		// Now disconnect it
		t.disconnect();
		t = null;

		// Now gc!
		do
		{
			Thread.sleep(100);
			System.gc();
			Thread.sleep(100);
		} while (this.tempCond == false);
	}

	/**
	 * This test will ensure that transports added to an actual TrapEndpoint are
	 * handed over (i.e. deallocated only when the endpoint dies).
	 */
	@Test(timeout = 10000)
	public void testTransportHandover() throws Exception
	{

		TrapTransport t = new AbstractTransport() {
			
			public boolean canConnect()
			{
				return true;
			}
			
			public boolean canListen()
			{
				return true;
			}


			public String getTransportName()
			{
				return "foo";
			}

			public String getProtocolName()
			{
				return "lte";
			}

			@Override
			public void fillAuthenticationKeys(HashSet<String> keys)
			{
			}

			@Override
			public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
			{
				ListenerTrapEndpointTest.this.tempCond = true;
			}

			@Override
			protected void internalDisconnect()
			{
				this.setState(TrapTransportState.DISCONNECTED);
			}

			@Override
			protected void internalConnect() throws TrapException
			{
			}

			@Override
			public void connect()
			{
				this.state = TrapTransportState.AVAILABLE;
			}

			@Override
			protected void finalize() throws Throwable
			{
				ListenerTrapEndpointTest.this.tempCond = true;
				super.finalize();
			}

			public void flushTransport()
			{
				// TODO Auto-generated method stub
				
			}

		};

		t.setTransportDelegate(new TrapTransportDelegate() {

			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
			}

			public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
			{
			}

			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
			}

			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
			}

            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
            }
		}, null);

		this.lte.listen(new OnAccept() {

			public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
			{
				ListenerTrapEndpointTest.this.tempEp = endpoint;
			}

		});

		t.connect();

		// Now add the new server connection properly.
		this.lte.ttsIncomingConnection(t, null, null);

		// Then add an incoming connection
		this.lte.ttMessageReceived(new TrapMessageImpl().setOp(Operation.OPEN), t, null);

		// t is SEP...
		t = null;

		// This is synchronous, so assert we have an endpoint.
		Assert.assertNotNull(this.tempEp);

		this.tempCond = false;

		// Perform a test sending of a message. (This won't actually send a message, but send tempCond to true)
		this.tempEp.send(new byte[0]);

		Thread.sleep(25);

		Assert.assertTrue(this.tempCond);
		this.tempCond = false;

		// Now disconnect the endpoint
		this.tempEp.close();

		// Clear everything
		this.tempEp = null;

		// Now gc! We want to gc until the transport is gone.
		do
		{
			Thread.sleep(100);
			System.gc();
			Thread.sleep(100);
		} while (this.tempCond == false);
	}

}
