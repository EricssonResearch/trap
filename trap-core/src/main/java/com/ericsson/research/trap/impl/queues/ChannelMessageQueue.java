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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import com.ericsson.research.trap.impl.TrapChannelImpl;
import com.ericsson.research.trap.spi.TrapMessage;

/**
 * A pseudo message queue that pulls its input from the endpoint's channels. The channel list is rebuilt on demand.
 * 
 * @author Vladimir Katardjiev
 */

public class ChannelMessageQueue
{
    RoundRobinChannelSelector[] priorities = new RoundRobinChannelSelector[0];
    long                        cPrio      = 0;
    int                         cPrioIndex = 0;
    long                        cPrioBytes = 0;
    @SuppressWarnings("unused")
    private TrapMessage lastPeek;
    
    public ChannelMessageQueue()
    {
    }
    
    public void rebuild(Collection<TrapChannelImpl> channels)
    {
        LinkedList<TrapChannelImpl> sortedChannels = new LinkedList<TrapChannelImpl>();
        sortedChannels.addAll(channels);
        
        Collections.sort(sortedChannels, new Comparator<TrapChannelImpl>() {
            
            public int compare(TrapChannelImpl o1, TrapChannelImpl o2)
            {
                return o2.getPriority() - o1.getPriority();
            }
        });
        
        LinkedList<RoundRobinChannelSelector> prioList = new LinkedList<RoundRobinChannelSelector>();
        int lastPriority = Integer.MIN_VALUE;
        RoundRobinChannelSelector sel = null;
        
        for (TrapChannelImpl c : sortedChannels)
        {
            
            if (c.getPriority() != lastPriority)
            {
                sel = new RoundRobinChannelSelector();
                prioList.add(sel);
                lastPriority = c.getPriority();
            }
            sel.addChannel(c);
        }
        
        synchronized (this)
        {
            this.priorities = prioList.toArray(this.priorities);
            this.setPrioIndex(0);
        }
    }
    
    public synchronized TrapMessage peek()
    {
        
        for (int i = this.cPrioIndex; i < this.priorities.length; i++)
        {
            TrapMessage msg = this.priorities[i].peek();
            if (msg != null)
            {
                this.cPrioIndex = i;
                lastPeek = msg;
                return msg;
            }
        }
        lastPeek = null;
        return null;
    }
    
    public synchronized TrapMessage pop()
    {
        TrapMessage popped = this.priorities[this.cPrioIndex].pop();
        long bs = popped.length();
        this.cPrioBytes += bs;
        
        if (this.cPrioBytes > this.cPrio)
            this.setPrioIndex(this.cPrioIndex + 1);
        
        return popped;
    }
    
    public void rewind()
    {
        this.setPrioIndex(0);
    }
    
    protected synchronized void setPrioIndex(int idx)
    {
        if (idx < this.priorities.length)
        {
            this.cPrioIndex = idx;
            this.cPrio = this.priorities[this.cPrioIndex].getPriority();
            this.cPrioBytes = 0;
        }
    }
    
    public synchronized String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[\n");
        
        for (int i = 0; i < this.priorities.length; i++)
        {
            RoundRobinChannelSelector cs = this.priorities[i];
            
            sb.append("\t");
            sb.append(cs.getPriority());
            sb.append(": ");
            sb.append(cs.toString());
            sb.append("\n");
        }
        
        sb.append("\n]");
        
        return sb.toString();
    }
    
    public void rebuild(TrapChannelImpl[] channels)
    {
        LinkedList<TrapChannelImpl> cList = new LinkedList<TrapChannelImpl>();
        
        for (int i = 0; i < channels.length; i++)
        {
            if (channels[i] != null)
                cList.add(channels[i]);
        }
        
        this.rebuild(cList);
    }
}

class RoundRobinChannelSelector
{
    TrapChannelImpl[] channels    = new TrapChannelImpl[0];
    int               currChannel = 0;
    
    public int getPriority()
    {
        return this.channels.length > 0 ? this.channels[0].getPriority() : Integer.MIN_VALUE;
    }
    
    public TrapMessage peek()
    {
        try
        {
            int start = this.currChannel;
            int end = this.currChannel + this.channels.length;
            
            for (int i = start; i < end; i++)
            {
                TrapMessage m = this.channels[this.currChannel % this.channels.length].peek();
                
                if (m != null)
                    return m;
                
                this.currChannel++;
            }
            
            return null;
        }
        finally
        {
            this.currChannel = this.currChannel % this.channels.length;
        }
    }
    
    public TrapMessage pop()
    {
        TrapMessage rv = this.channels[this.currChannel].pop();
        this.currChannel++;
        return rv;
    }
    
    public void addChannel(TrapChannelImpl c)
    {
        this.channels = Arrays.copyOf(this.channels, this.channels.length + 1);
        this.channels[this.channels.length - 1] = c;
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{");
        
        for (int i = 0; i < this.channels.length; i++)
        {
            if (i > 0)
                sb.append(", ");
            sb.append(this.channels[i].toString());
        }
        
        sb.append("}");
        
        return sb.toString();
    }
}
