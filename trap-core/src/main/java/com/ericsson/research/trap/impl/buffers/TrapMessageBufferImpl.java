package com.ericsson.research.trap.impl.buffers;

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

import com.ericsson.research.trap.spi.TrapEndpointMessage;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessageBuffer;
import com.ericsson.research.trap.spi.TrapTransport;

/**
 * Circular buffer of TrapMessages and TrapTransports they arrived on. The message buffer works using random inserts and
 * regular reads. The buffer needs to be initialised with a size (can be automatically increased) and initial expected
 * message id. The buffer will use this expected message ID to seed itself with and be able to receive messages from
 * history.
 * <p>
 * The buffer will automatically sort messages for reading. If messages are read in order, there is no performance
 * penalty for accesses. If messages come from outside the buffer's range, there is a performance penalty, based on
 * buffer settings.
 * <p>
 * To put it another way, it is a self-growing, circular object buffer implementing random write and sequential read.
 * 
 * @author Vladimir Katardjiev
 */
public class TrapMessageBufferImpl implements TrapMessageBuffer
{
    
    TrapEndpointMessage[] buffer;
    
    // The next message ID that should be read
    long                  readMessageID  = 0;
    // The next message ID that should be written
    long                  writeMessageID = 0;
    
    // We'll perform input validation to catch corrupted messages.
    // The following sizes give us the min/max values of messages we expect to receive.
    int                   maxMessageId;
    int                   minMessageId;
    
    int                   bufGrowthSize;
    int                   maxBufSize;
    
    public TrapMessageBufferImpl(int bufSize, int maxBufSize, int startMessageId, int minMessageId, int maxMessageId)
    {
        this.bufGrowthSize = bufSize;
        this.maxBufSize = maxBufSize;
        this.minMessageId = minMessageId;
        this.maxMessageId = maxMessageId;
        this.buffer = new TrapEndpointMessage[bufSize];
        
        // StartMessageID tells us which message is the first to arrive.
        // This is useful if we're swapped out in the middle of a conversation.
        this.readMessageID = this.writeMessageID = startMessageId;
        
        this.fillEmptyBuf(this.buffer);
    }
    
    private void fillEmptyBuf(TrapEndpointMessage[] buffer)
    {
        for (int i = 0; i < buffer.length; i++)
        {
            TrapEndpointMessage m = buffer[i];
            
            if (m == null)
                buffer[i] = new TrapEndpointMessage();
        }
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.research.trap.impl.buffers.TrapMessageBuffer#available()
     */
    public synchronized int available()
    {
        // This is a simple calculation. The (next) expected write message minus the (next) expected read message ID.
        return (int) (this.writeMessageID - this.readMessageID);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.research.trap.impl.buffers.TrapMessageBuffer#put(com.ericsson.research.trap.spi.TrapMessage, com.ericsson.research.trap.spi.TrapTransport)
     */
    public synchronized void put(TrapMessage m, TrapTransport t) throws IllegalArgumentException
    {
        
        // Step 1: Input validation.
        long messageId = m.getMessageId();
        
        if (messageId > this.maxMessageId || messageId < this.minMessageId)
            throw new IllegalArgumentException("Message ID [" + messageId + "] outside of acceptable range [" + this.minMessageId + ", " + this.maxMessageId + "].");
        
        // Message IDs can be reused (and reusing them won't cleanly fit in the buffer.
        // In those wrapping cases, we'll need to up the effective messageId appropriately.
        // TODO: Better constant?
        if (messageId < this.readMessageID)
        {
            if ((this.readMessageID - messageId) > (this.maxMessageId - this.minMessageId) / 2)
                messageId += this.maxMessageId - this.minMessageId + 1;
            else
                return; // Skip duplicated message.
        }
        
        // Assert that the message has a chance at fitting inside the buffer
        if (messageId > (this.readMessageID + this.maxBufSize))
            throw new IllegalArgumentException("Message ID [" + messageId + "] outside of buffer size. First message has ID [" + this.readMessageID + "] and max buffer size is " + this.maxBufSize);
        
        // Assert the message has not already been written.
        if (messageId < this.writeMessageID)
            return; // This should be a safe operation. It just means the message is duplicated.
            
        // At this point in time we know that:
        // 1) The message will fit in the buffer [writeMessageID, readMessageId+maxBufSize]
        // 2) The message is in that range and has not already been written.
        // We now need to ensure the buffer is large enough
        // This is a STRICT equality. Proof: buffer.length == 1, buffer[0] != null => buffer is full
        if (messageId >= (this.readMessageID + this.buffer.length))
        {
            // This is a simple operation. Resize the old buffer into a new one by flushing out the current messages.
            int newSize = (int) (messageId - this.readMessageID);
            // The new size should be a multiple of bufGrowthSize
            newSize /= this.bufGrowthSize;
            newSize++;
            newSize *= this.bufGrowthSize;
            
            TrapEndpointMessage[] newBuf = new TrapEndpointMessage[newSize];
            this.fillEmptyBuf(newBuf);
            
            // Move all slots from the old buffer to the new one, recalculating the modulus as applicable.
            // We have to move all slots as we don't track which ones have been filled.
            //for (long readId = this.readMessageID; readId < this.readMessageID + buffer.length; readId++)
            //{
            //	newBuf[(int) (readId % newBuf.length)] = buffer[(int) (readId % buffer.length)];
            //}
            for (int i = 0; i < this.buffer.length; i++)
            {
                TrapEndpointMessage tmp = this.buffer[i];
                if (tmp.getMessage() != null)
                    newBuf[tmp.getMessage().getMessageId() % newBuf.length] = tmp;
            }
            
            this.buffer = newBuf;
            
        }
        
        // Where are we now? Well, that's the rad part. We now know that messageId will comfortably fit in our world so all we need to do is fill it.
        TrapEndpointMessage slot = this.buffer[(int) (messageId % this.buffer.length)];
        slot.setMessage(m);
        slot.t = t;
        
        //System.out.println("Wrote message with ID " + messageId + " and expected ID " + writeMessageID);
        
        // Final step is to increment the writeMessageId entry, if applicable.
        if (messageId == this.writeMessageID)
        {
            do
            {
                long expectedMessageId = this.writeMessageID;
                
                if (expectedMessageId > this.maxMessageId)
                    expectedMessageId -= this.maxMessageId - this.minMessageId + 1;
                
                // Bug catch verification. Logically, writeMessageID should be the message ID of the current slot's message. If they don't match, we're in deep doodoo
                if (slot.getMessage().getMessageId() != expectedMessageId)
                    throw new IllegalStateException("Trap Message Buffer corrupted. Unexpected message ID found. This needs debugging...");
                
                // Increment by one.
                this.writeMessageID++;
                // Fetch the next entry
                slot = this.buffer[(int) (this.writeMessageID % this.buffer.length)];
                
                //System.out.println("Slot.m: " + slot.m + " and rem size " + available() + " vs buffer " + buffer.length);
                //System.out.println((slot.m != null && (this.writeMessageID - this.readMessageID) < buffer.length));
            } while (slot.getMessage() != null && (this.writeMessageID - this.readMessageID) < this.buffer.length);
        }
        
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.research.trap.impl.buffers.TrapMessageBuffer#fetch(com.ericsson.research.trap.spi.TrapEndpointMessage, boolean)
     */
    public synchronized boolean fetch(TrapEndpointMessage target)
    {
        
        // Nothing to read here, move along...
        if (this.readMessageID >= this.writeMessageID)
        {
            target.setMessage(null);
            target.t = null;
            return false;
        }
        try
        {
            TrapEndpointMessage m = null;
            for (;;)
            {
                
                m = this.buffer[(int) (this.readMessageID % this.buffer.length)];
                
                if (m.getMessage() != null)
                {
                    //System.out.println("Read message with ID " + m.m.getMessageId() + " and expected ID " + this.readMessageID);
                    this.readMessageID++;
                    target.setMessage(m.getMessage());
                    target.t = m.t;
                    m.setMessage(null);
                    m.t = null;
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        finally
        {
            // If we have wrapped around the messages, we can finally throw ourselves a bone and reduce the message IDs to handle wrapping gracefully.
            if (this.readMessageID > this.maxMessageId)
            {
                // The easiest way is to just create a new buffer and refill it.
                // This is a fairly expensive operation, but it should only happen once every billion messages or so, so we can consider
                // the cost amortized.
                TrapEndpointMessage[] newBuf = new TrapEndpointMessage[this.buffer.length];
                this.fillEmptyBuf(newBuf);
                
                this.readMessageID -= this.maxMessageId - this.minMessageId + 1;
                this.writeMessageID -= this.maxMessageId - this.minMessageId + 1;
                
                // Recalculation can and should be based on the message IDs. This prevents us from doing expensive errors.
                for (int i = 0; i < newBuf.length; i++)
                {
                    TrapEndpointMessage tmp = this.buffer[i];
                    if (tmp.getMessage() != null)
                        newBuf[tmp.getMessage().getMessageId() % newBuf.length] = tmp;
                }
                
                this.buffer = newBuf;
            }
        }
    }
    
    public String toString()
    {
        return Arrays.toString(this.buffer);
    }
    
}
