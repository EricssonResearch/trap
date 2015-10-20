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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.TrapMessageImpl;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;

public class TransportMessageAckTest
{
	@Test
	public void testBasicAck() throws Exception
	{

		StubbedTransport t = new StubbedTransport() {

			@Override
			public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
			{
				this.receiveMessage(message);
			}

			public void flushTransport()
			{
				// TODO Auto-generated method stub
				
			}
		}; 

		TrapMessageImpl m = new TrapMessageImpl();
		m.setMessageId(123);

		t.addTransitMessage(m);
		t.acknowledgeTransitMessage(m);

	}

	@Test(timeout=10000)
	public void testDelayedAck() throws Exception
	{

		final ConcurrentLinkedQueue<TrapMessage> mAcks = new ConcurrentLinkedQueue<TrapMessage>();

		StubbedTransport t = new StubbedTransport() {

			@Override
			public void internalSend(final TrapMessage message, boolean expectMore) throws TrapTransportException
			{
				mAcks.add(message);
			}

			public void flushTransport()
			{
				// TODO Auto-generated method stub
				
			}
		};
		
		t.delegate = new TrapTransportDelegate() {
			
			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
			}
			
			public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
			{
			}
			
			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
			}
			
			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
			}

            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
            }
		};

		for (int i = 100000; i < 101050; i++)
		{
			TrapMessageImpl m = new TrapMessageImpl();
			m.setMessageId(i);

			t.addTransitMessage(m);
			t.acknowledgeTransitMessage(m);
		}

		while (t.messagesInTransit.size() > 0)
		{
			TrapMessage poll = mAcks.poll();
			if (poll != null)
				t.receiveMessage(poll);
		}

	}
}

abstract class StubbedTransport extends AbstractTransport
{
	
	public boolean canConnect()
	{
		return true;
	}
	
	public boolean canListen()
	{
		return true;
	}


	public StubbedTransport()
	{
		this.state = TrapTransportState.AVAILABLE;
	}

	public String getTransportName()
	{
		// TODO Auto-generated method stub
		return "stubbed";
	}

	public String getProtocolName()
	{
		return "stubbed";
	}

	@Override
	protected void internalConnect() throws TrapException
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void internalDisconnect()
	{
		// TODO Auto-generated method stub

	}

}
