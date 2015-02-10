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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;

public class CABMessageQueue implements BlockingMessageQueue
{
	
	ArrayBlockingQueue<TrapMessage>	queue;
	private long					timeout	= 30000;
	private int						queueSize;
	
	public CABMessageQueue()
	{
		this(1000);
	}
	
	public CABMessageQueue(int i)
	{
		this.queueSize = i;
		this.queue = new ArrayBlockingQueue<TrapMessage>(i);
	}
	
	public void put(TrapMessage m) throws TrapException
	{
		try
		{
			if (!this.queue.offer(m, this.blockingTimeout(), TimeUnit.MILLISECONDS))
				throw new TrapException("The specified timeout of " + this.timeout + "ms expired without being able to enqueue the message.");
		}
		catch (InterruptedException e)
		{
			//e.printStackTrace();
			throw new TrapException(e);
		}
	}
	
	public TrapMessage peek()
	{
		return this.queue.peek();
	}
	
	public TrapMessage pop()
	{
		return this.queue.poll();
	}
	
	public int length()
	{
		return this.queue.size();
	}
	
	public long size()
	{
		return this.queueSize;
	}
	
	public String getQueueType()
	{
		return TrapEndpoint.BLOCKING_MESSAGE_QUEUE;
	}
	
	public boolean hasMoreThanOne()
	{
		return this.queue.size() > 1;
	}
	
	public long blockingTimeout()
	{
		return this.timeout;
	}
	
	public void setBlockingTimeout(long newTimeout)
	{
		this.timeout = newTimeout;
	}
	
	public String toString()
	{
		return "CAB/" + this.queue.toString();
	}
	
	public MessageQueue createNewQueue()
	{
		CABMessageQueue rv = new CABMessageQueue(this.queueSize);
		rv.timeout = this.timeout;
		return rv;
	}
	
}
