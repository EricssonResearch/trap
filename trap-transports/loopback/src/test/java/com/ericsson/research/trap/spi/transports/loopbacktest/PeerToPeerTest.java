package com.ericsson.research.trap.spi.transports.loopbacktest;

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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapPeer.TrapPeerChannel;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.TrapTransports;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.TrapImplDebugPrinter;
import com.ericsson.research.trap.impl.TrapPeerImpl;
import com.ericsson.research.trap.spi.transports.LoopbackTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.StringUtil;

public class PeerToPeerTest
{
    private TrapPeerImpl one;
    private TrapPeerImpl two;
    
    @BeforeClass
    public static void setTransports()
    {
        JDKLoggerConfig.initForPrefixes(Level.ALL);
        TrapTransports.setUseAutodiscoveredTransports(false);
        TrapTransports.addTransportClass(LoopbackTransport.class);
    }
    
    @AfterClass
    public static void unsetTransports()
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        TrapTransports.setUseAutodiscoveredTransports(true);
        TrapTransports.removeTransportClass(LoopbackTransport.class);
        
    }
    
    @After
    public void cleanup() throws Exception
    {
        if (this.one != null)
        {
            TrapImplDebugPrinter.removeEndpoint(this.one);
            this.one.close();
            this.one = null;
        }
        if (this.two != null)
        {
            TrapImplDebugPrinter.removeEndpoint(this.two);
            this.two.close();
            this.two = null;
        }
        
        TrapImplDebugPrinter.stop();
    }
    
    @Test(timeout = 10000)
    public void p2p() throws Exception
    {
        this.one = new TrapPeerImpl();
        this.two = new TrapPeerImpl();
        
        this.one.enableTransport("loopback");
        this.two.enableTransport("loopback");
        
        this.one.open(new TrapPeerChannel() {
            
            public void sendToRemote(byte[] data)
            {
                PeerToPeerTest.this.two.receive(data);
            }
        });
        
        this.two.open(new TrapPeerChannel() {
            
            public void sendToRemote(byte[] data)
            {
                PeerToPeerTest.this.one.receive(data);
            }
        });
        
        while (this.one.getState() != TrapState.OPEN && this.two.getState() != TrapState.OPEN)
            ;
        
        System.err.println("We have a connection!");
        
        TrapImplDebugPrinter.addEndpoint(this.one);
        TrapImplDebugPrinter.addEndpoint(this.two);
        TrapImplDebugPrinter.start();
        
        final AtomicInteger datas = new AtomicInteger(2);
        
        OnData data = new OnData() {
            
            @Override
            public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
            {
                System.err.println("New data: " + StringUtil.toUtfString(data));
                datas.decrementAndGet();
            }
        };
        
        this.one.setDelegate(data, false);
        this.two.setDelegate(data, false);
        
        this.one.send("Hello 2".getBytes());
        this.two.send("Hello 1".getBytes());
        
        while (datas.get() > 0)
            Thread.sleep(10);
        
    }
}
