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

import junit.framework.Assert;

import org.junit.Test;

import com.ericsson.research.trap.impl.TrapMessageImpl;
import com.ericsson.research.trap.spi.TrapEndpointMessage;
import com.ericsson.research.trap.spi.TrapMessageBuffer;

public class TrapMessageBufferTest
{
    
    int min = 1;
    int max = 10;
    
    public static String expect(Integer... args)
    {
        String rv = "[";
        for (Integer i : args)
            rv += i == null ? "null, " : "OK/C0/" + i + "/0, ";
        rv = rv.substring(0, rv.length() - 2);
        return rv + "]";
    }
    
    @Test
    public void testSequence() throws Exception
    {
        this.min = 1;
        this.max = 10;
        
        // Simple buffer, should not fault
        TrapMessageBufferImpl b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        // Perform a sequence that generates 10 messages, takes 10 messages.
        this.produce(b, 10, new Sequencer(1));
        this.consume(b, 10, false);
        
        // Assert the buffer is empty.
        TrapEndpointMessage[] buffer = b.buffer;
        
        for (TrapEndpointMessage m : buffer)
            Assert.assertNull(m.getMessage());
    }
    
    // Test some simple add/remove
    @Test
    public void testMix() throws Exception
    {
        this.min = 1;
        this.max = 10;
        
        // Simple buffer, should not fault
        TrapMessageBufferImpl b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        // Perform a sequence that generates 10 messages, takes 10 messages.
        Sequencer seq = new Sequencer(1);
        for (int i = 0; i < 10; i++)
        {
            this.produce(b, 1, seq);
            this.consume(b, 1, false);
        }
        
        // Assert the buffer is empty.
        TrapEndpointMessage[] buffer = b.buffer;
        
        for (TrapEndpointMessage m : buffer)
            Assert.assertNull(m.getMessage());
    }
    
    @Test
    public void testOverflow() throws Exception
    {
        this.min = 1;
        this.max = 10;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        // Perform a sequence that generates 10 messages, takes 10 messages.
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 10, seq);
        this.consume(b, 5, false);
        this.produce(b, 5, seq);
    }
    
    @Test
    public void testWrap() throws Exception
    {
        this.min = 1;
        this.max = 20;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        // Perform a sequence that generates 10 messages, takes 10 messages.
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 10, seq);
        Assert.assertEquals(expect(10, 1, 2, 3, 4, 5, 6, 7, 8, 9), b.toString());
        
        this.consume(b, 5, false);
        Assert.assertEquals(expect(10, null, null, null, null, null, 6, 7, 8, 9), b.toString());
        
        this.produce(b, 5, seq);
        Assert.assertEquals(expect(10, 11, 12, 13, 14, 15, 6, 7, 8, 9), b.toString());
        
        this.consume(b, 5, false);
        Assert.assertEquals(expect(null, 11, 12, 13, 14, 15, null, null, null, null), b.toString());
        
        this.produce(b, 5, seq);
        Assert.assertEquals(expect(20, 11, 12, 13, 14, 15, 16, 17, 18, 19), b.toString());
        
        this.consume(b, 5, false);
        Assert.assertEquals(expect(20, null, null, null, null, null, 16, 17, 18, 19), b.toString());
        
    }
    
    @Test
    public void testUnevenWrap() throws Exception
    {
        this.min = 1;
        this.max = 20;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        // Perform a sequence that generates 10 messages, takes 10 messages.
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 10, seq);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[null, null, null, null, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[1, 2, 3, 4, null, null, null, null, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[null, null, 3, 4, 5, 6, 7, 8, null, null]", Arrays.toString(b.buffer));
        
    }
    
    @Test
    public void testSizeMismatch() throws Exception
    {
        this.min = 1;
        this.max = 22;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(10, 10, 1, this.min, this.max);
        
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 10, seq);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[null, null, null, null, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[11, 12, 1, 2, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[11, 12, 1, 2, null, null, null, null, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[11, 12, 1, 2, 3, 4, 5, 6, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 4, false);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, null, null, null, null]", Arrays.toString(b.buffer));
        
    }
    
    @Test
    public void testOddSizeMismatch() throws Exception
    {
        this.min = 1;
        this.max = 23;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(10, 12, 1, this.min, this.max);
        
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 10, seq);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 5, false);
        //Assert.assertEquals("[null, null, null, null, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 5, seq);
        //Assert.assertEquals("[11, 12, 1, 2, 5, 6, 7, 8, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 5, false);
        //Assert.assertEquals("[11, 12, 1, 2, null, null, null, null, 9, 10]", Arrays.toString(b.buffer));
        
        this.produce(b, 5, seq);
        //Assert.assertEquals("[11, 12, 1, 2, 3, 4, 5, 6, 9, 10]", Arrays.toString(b.buffer));
        
        this.consume(b, 10, false);
        //Assert.assertEquals("[1, 2, 3, 4, 5, 6, null, null, null, null]", Arrays.toString(b.buffer));
        
        for (int i = 1; i < 10; i++)
        {
            this.produce(b, i, seq);
            this.consume(b, i, false);
        }
        
    }
    
    @Test
    public void testBufferResize() throws Exception
    {
        this.min = 1;
        this.max = 16;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(2, 8, 1, this.min, this.max);
        
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[1, 2, 3, 4]", Arrays.toString(b.buffer));
        
        this.consume(b, 2, false);
        //Assert.assertEquals("[null, null, 3, 4]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[7, 8, 3, 4, 5, 6]", Arrays.toString(b.buffer));
        
        this.consume(b, 2, false);
        //Assert.assertEquals("[7, 8, null, null, 5, 6]", Arrays.toString(b.buffer));
        
        this.produce(b, 4, seq);
        //Assert.assertEquals("[9, 10, 11, 12, 5, 6, 7, 8]", Arrays.toString(b.buffer));
        
        this.consume(b, 6, false);
        //Assert.assertEquals("[null, null, 11, 12, null, null, null, null]", Arrays.toString(b.buffer));
        
    }
    
    @Test
    public void testBufferNonResize() throws Exception
    {
        this.min = 1;
        this.max = 16;
        
        // Simple buffer, should not fault
        TrapMessageBufferImpl b = new TrapMessageBufferImpl(2, 8, 1, this.min, this.max);
        
        Sequencer seq = new Sequencer(1);
        
        this.produce(b, 2, seq);
        Assert.assertEquals(expect(2, 1), b.toString());
        
        this.consume(b, 1, false);
        Assert.assertEquals(expect(2, null), b.toString());
        
        this.produce(b, 1, seq);
        Assert.assertEquals(expect(2, 3), b.toString());
        
        this.consume(b, 1, false);
        Assert.assertEquals(expect(null, 3), b.toString());
        
        this.produce(b, 1, seq);
        Assert.assertEquals(expect(4, 3), b.toString());
        
        this.consume(b, 2, false);
        Assert.assertEquals(expect(null, null), b.toString());
        
        Assert.assertEquals(2, b.buffer.length);
        
    }
    
    @Test
    public void testHugeBuffer() throws Exception
    {
        this.min = 1;
        this.max = 100000;
        
        // Simple buffer, should not fault
        TrapMessageBuffer b = new TrapMessageBufferImpl(50, 10000, 1, this.min, this.max);
        
        Sequencer seq = new Sequencer(1);
        
        for (int i = 0; i < 100; i++)
        {
            this.produce(b, 10000, seq);
            this.consume(b, 10000, false);
        }
        
    }
    
    @Test
    public void testReorder() throws Exception
    {
        // Size 2, Max 4, start 1, min 1, max 10
        TrapMessageBufferImpl b = new TrapMessageBufferImpl(2, 4, 1, 1, 10);
        
        TrapEndpointMessage out = new TrapEndpointMessage();
        
        // Start with unordered messages
        b.put(new TrapMessageImpl().setMessageId(2), null);
        System.out.println(b.toString());
        
        // Grow the buffer, out of order
        b.put(new TrapMessageImpl().setMessageId(4), null);
        System.out.println(b.toString());
        
        // No objects should be available. The buffer is expecting ID 0.
        int available = b.available();
        Assert.assertEquals(0, available);
        
        // And should return null.
        b.fetch(out);
        Assert.assertNull(out.getMessage());
        
        // But if we skip, it should return one.
        b.put(new TrapMessageImpl().setMessageId(1), null);
        System.out.println(b.toString());
        
        // Now expect two messages to be available (there's still a hole)
        available = b.available();
        Assert.assertEquals(2, available);
        
        b.fetch(out);
        Assert.assertNotNull(out.getMessage());
        Assert.assertEquals(1, out.getMessage().getMessageId());
        
        // We still lack a message.
        b.put(new TrapMessageImpl().setMessageId(3), null);
        System.out.println(b.toString());
        
        // Now expect three messages to be available
        available = b.available();
        Assert.assertEquals(3, available);
        
        
    }
    
    int lastSeq = -1;
    
    private void consume(TrapMessageBuffer b, int i, boolean skip)
    {
        TrapEndpointMessage m = new TrapEndpointMessage();
        for (int j = 0; j < i; j++)
        {
            b.fetch(m);
            
            int seq = m.getMessage().getMessageId();
            
            if ((seq == this.min) && (this.lastSeq == this.max))
                this.lastSeq = seq;
            else if (seq > this.lastSeq)
                this.lastSeq = seq;
            else
                Assert.fail();
            
        }
    }
    
    private void produce(TrapMessageBuffer b, int i, Sequencer seq)
    {
        for (int j = 0; j < i; j++)
        {
            TrapMessageImpl m = new TrapMessageImpl();
            m.setMessageId(seq.seq());
            b.put(m, null);
        }
    }
    
    class Sequencer
    {
        private long i;
        
        public Sequencer(int start)
        {
            this.i = start;
        }
        
        public int seq()
        {
            long rv = this.i++;
            
            if (rv > TrapMessageBufferTest.this.max)
            {
                rv = this.i = TrapMessageBufferTest.this.min;
                this.i++;
            }
            return (int) rv;
        }
    }
}
