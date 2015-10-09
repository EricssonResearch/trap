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

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;

public class ArrayBlockingMessageQueue implements BlockingMessageQueue
{
	
	TrapMessage[]	buf;
	
	int				startIndex		= 0;
	int				endIndex		= 0;
	
	private long	blockingTimeout	= 30000;
	
	public ArrayBlockingMessageQueue()
	{
		this(1000);
	}
	
	public ArrayBlockingMessageQueue(int i)
	{
		this.buf = new TrapMessage[i];
	}
	
	public synchronized void put(TrapMessage m) throws TrapException
	{
		// Check if full
		if (this.buf[this.endIndex] != null)
		{
			long endTime = this.blockingTimeout() + System.currentTimeMillis();
			
			while (this.buf[this.endIndex] != null)
			{
				long waitTime = endTime - System.currentTimeMillis();
				
				if (waitTime > 0)
				{
					try
					{
						this.wait(waitTime);
					}
					catch (InterruptedException e)
					{
					}
				}
				else
				{
					throw new TrapException("Could not enter buffer within the time limit");
				}
			}
		}
		
		this.buf[this.endIndex] = m;
		
		this.endIndex = (this.endIndex + 1) % this.buf.length;
		
	}
	
	public synchronized TrapMessage peek()
	{
		return this.buf[this.startIndex];
	}
	
	public synchronized TrapMessage pop()
	{
		
		TrapMessage m = this.peek();
		
		if (m != null)
		{
			this.buf[this.startIndex] = null;
			this.startIndex = (this.startIndex + 1) % this.buf.length;
			
			this.notifyAll();
		}
		
		return m;
	}
	
	public synchronized int length()
	{
		int rv = 0;
		
		if (this.startIndex < this.endIndex)
			rv = this.endIndex - this.startIndex;
		else if (this.startIndex > this.endIndex)
			rv = (this.endIndex - this.startIndex) + this.buf.length;
		else if (this.buf[this.startIndex] == null)
			rv = 0;
		else
			rv = this.buf.length;
		
		if (rv < 0)
		{
			System.err.println("DEBUG: Mathematical impossibility. There must be a bug.");
		}
		
		return rv;
	}
	
	public long size()
	{
		return this.buf.length;
	}
	
	public String getQueueType()
	{
		return TrapEndpoint.BLOCKING_MESSAGE_QUEUE;
	}
	
	public long blockingTimeout()
	{
		return this.blockingTimeout;
	}
	
	public void setBlockingTimeout(long newTimeout)
	{
		this.blockingTimeout = newTimeout;
	}
	
	public boolean hasMoreThanOne()
	{
		return this.length() > 1;
	}
	
	public synchronized String toString()
	{
		
		StringBuffer s = new StringBuffer();
		s.append("AB {");
		s.append(this.startIndex);
		s.append("/");
		s.append(this.endIndex);
		s.append("} [");
		
		int numNull = 0;
		int nullIdx = -1;
		
		for (int i=0; i<this.buf.length; i++)
		{
			TrapMessage entry = this.buf[i];
			
			if (entry == null)
			{
				if (nullIdx < 0)
					nullIdx = i;
				
				numNull++;
				continue;
			}
			
			if (numNull > 0)
			{
				s.append(nullIdx);
				s.append("/null");
				if (numNull > 1)
				{
					s.append("x");
					s.append(numNull);
				}
				s.append(", ");
				numNull = 0;
				nullIdx = -1;
			}
			
			s.append(i);
			s.append("/");
			s.append(entry);
			s.append(", ");
		}

		
		if (numNull > 0)
		{
			s.append(nullIdx);
			s.append("/null");
			if (numNull > 1)
			{
				s.append("x");
				s.append(numNull);
			}
			s.append(", ");
			numNull = 0;
			nullIdx = -1;
		}

		s.deleteCharAt(s.length()-1);
		s.deleteCharAt(s.length()-2);
		s.append("]");
		
		return s.toString();
	}

	public MessageQueue createNewQueue()
	{
		ArrayBlockingMessageQueue rv = new ArrayBlockingMessageQueue(this.buf.length);
		rv.setBlockingTimeout(this.blockingTimeout);
		return rv;
	}
}
