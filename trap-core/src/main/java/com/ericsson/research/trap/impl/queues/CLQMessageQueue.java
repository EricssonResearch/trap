package com.ericsson.research.trap.impl.queues;

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

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;

public class CLQMessageQueue implements MessageQueue, BlockingMessageQueue
{
	
	private final LinkedBlockingQueue<TrapMessage>	messageQueue;
	private long									maxQueueSize	= 1000;
	private long									blockingTimeout	= 30000;
	
	public CLQMessageQueue()
	{
		this.messageQueue = new LinkedBlockingQueue<TrapMessage>((int) this.maxQueueSize);
	}
	
	public CLQMessageQueue(int queueSize)
	{
		this.maxQueueSize = queueSize;
		this.messageQueue = new LinkedBlockingQueue<TrapMessage>(queueSize);
		
	}
	
	public void put(TrapMessage message) throws TrapException
	{
		
		try
		{
			if (!this.messageQueue.offer(message, this.blockingTimeout, TimeUnit.MILLISECONDS))
				throw new TrapException("Could not insert the message!");
		}
		catch (Exception e)
		{
			// The only other exceptions are caused by reflection (=not caused) or runtime (OOM, etc)
			throw new TrapException(e);
		}
		
	}
	
	public TrapMessage peek()
	{
		return this.messageQueue.peek();
	}
	
	public TrapMessage pop()
	{
		return this.messageQueue.poll();
	}
	
	public long size()
	{
		return this.maxQueueSize;
	}
	
	public String getQueueType()
	{
		return TrapEndpoint.BLOCKING_MESSAGE_QUEUE;
	}
	
	public int length()
	{
		return this.messageQueue.size();
	}
	
	public boolean hasMoreThanOne()
	{
		try
		{
			Iterator<TrapMessage> it = this.messageQueue.iterator();
			it.next();
			return it.hasNext();
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public long blockingTimeout()
	{
		return this.blockingTimeout;
	}
	
	public void setBlockingTimeout(long newTimeout)
	{
		this.blockingTimeout = newTimeout;
	}
	
	public String toString()
	{
		return "CLQ/" + this.messageQueue.toString();
	}

	public MessageQueue createNewQueue()
	{
		CLQMessageQueue rv = new CLQMessageQueue((int) this.maxQueueSize);
		rv.blockingTimeout = this.blockingTimeout;
		return rv;
	}
}
