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

import java.util.LinkedList;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.queues.LinkedByteMessageQueue.QueuedMessage;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;
import com.ericsson.research.trap.spi.queues.ResizableMessageQueue;

public class LinkedByteBlockingMessageQueue implements MessageQueue, BlockingMessageQueue, ResizableMessageQueue
{
    
    private final LinkedList<QueuedMessage> messageQueue     = new LinkedList<QueuedMessage>();
    private long                            maxQueueSize     = Integer.MAX_VALUE;
    private long                            messageQueueSize = 0;
    private long                            blockingTimeout  = Long.MAX_VALUE;
    
    public LinkedByteBlockingMessageQueue()
    {
    }
    
    public LinkedByteBlockingMessageQueue(int i)
    {
        this.maxQueueSize = i;
    }
    
    public void put(TrapMessage message) throws TrapException
    {
        synchronized (this.messageQueue)
        {
            QueuedMessage m = new QueuedMessage(message);
            
            // TODO: Insert system messages in the queue at a better position
            if (message.getOp() != Operation.MESSAGE)
            {
                this.messageQueue.addLast(m);
                this.messageQueueSize += m.size();
            }
            // Check the queue length.
            else
            {
                
                if ((this.messageQueueSize + m.size()) > this.maxQueueSize)
                {
                    if (this.blockingTimeout == Long.MAX_VALUE)
                    {
                        // Wait forever
                        while ((this.messageQueueSize + m.size()) > this.maxQueueSize)
                        {
                            try
                            {
                                this.messageQueue.wait();
                            }
                            catch (InterruptedException e)
                            {
                                throw new TrapException(e);
                            }
                        }
                    }
                    else
                    {
                        // Timeout and wait.
                        long endTime = System.currentTimeMillis() + this.blockingTimeout;
                        
                        while ((System.currentTimeMillis() < endTime) && ((this.messageQueueSize + m.size()) > this.maxQueueSize))
                        {
                            try
                            {
                                long waitTime = endTime - System.currentTimeMillis();
                                
                                if (waitTime > 0)
                                    this.messageQueue.wait(waitTime);
                            }
                            catch (Exception e)
                            {
                                throw new TrapException(e);
                            }
                        }
                        
                        // Check if we exited cleanly.
                        if ((this.messageQueueSize + m.size()) > this.maxQueueSize)
                            throw new TrapException("Timed out while waiting for a space in the deferred message queue");
                    }
                }
                
                this.messageQueue.addLast(m);
                this.messageQueueSize += m.size();
            }
        }
    }
    
    public TrapMessage peek()
    {
        synchronized (this.messageQueue)
        {
            Object m = null;
            try
            {
                m = (this.messageQueue.get(0));
                if (m != null)
                    return ((QueuedMessage) m).message();
            }
            catch (IndexOutOfBoundsException e)
            {
            }
            catch (ClassCastException e)
            {
                if (m instanceof TrapMessage)
                    return (TrapMessage) m;
            }
            return null;
        }
    }
    
    public TrapMessage pop()
    {
        synchronized (this.messageQueue)
        {
            try
            {
                QueuedMessage m = this.messageQueue.remove(0);
                this.messageQueueSize -= m.size();
                this.messageQueue.notifyAll();
                return m.message();
            }
            catch (IndexOutOfBoundsException e)
            {
                return null;
            }
        }
    }
    
    public long size()
    {
        return this.maxQueueSize;
    }
    
    public void resize(long newSize)
    {
        this.maxQueueSize = newSize;
    }
    
    public String getQueueType()
    {
        return TrapEndpoint.BLOCKING_BYTE_QUEUE;
    }
    
    public int length()
    {
        return (int) this.messageQueueSize;
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
        return this.messageQueue.size() > 1;
    }
    
    public String toString()
    {
        synchronized (this.messageQueue)
        {
            return this.messageQueue.toString();
        }
    }
    
    public MessageQueue createNewQueue()
    {
        LinkedByteBlockingMessageQueue rv = new LinkedByteBlockingMessageQueue((int) this.maxQueueSize);
        rv.setBlockingTimeout(this.blockingTimeout);
        return rv;
    }
    
}
