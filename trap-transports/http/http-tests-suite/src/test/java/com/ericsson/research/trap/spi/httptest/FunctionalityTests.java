package com.ericsson.research.trap.spi.httptest;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.logging.Level;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.spi.TrapHostingTransport;
import com.ericsson.research.trap.spi.TrapHostingTransport.TrapHostable;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.UUID;

public class FunctionalityTests implements OnAccept
{
    
    private TrapListener listener;
    private TrapClient c;
    private TrapEndpoint incomingEP;
    private TrapEndpoint s;

    @Before
    public void setUp() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.ALL);
        this.listener = TrapFactory.createListener(null);
		listener.disableTransport("websocket");
        this.listener.listen(this);
        
        String cfg = this.listener.getClientConfiguration();
        this.c = TrapFactory.createClient(cfg, true);
        this.c.setDelegate(this, true);
        this.c.open();
        this.c.setAsync(false);
        
        // Accept
        
        this.s = this.accept();
        
        while (this.c.getState() != TrapState.OPEN)
            Thread.sleep(10);
    }

    protected synchronized TrapEndpoint accept() throws InterruptedException
    {
        try
        {
            while (this.incomingEP == null)
                this.wait();
            
            return this.incomingEP;
        }
        finally
        {
            this.incomingEP = null;
        }
    }

    public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        this.incomingEP = endpoint;
        endpoint.setDelegate(this, true);
        this.notify();
    }
    
    @After
    public void tearDown() throws Throwable
    {
        this.c.close();
        this.s.close();
        this.listener.close();
    }
    
    @Test
    public void testHosted() throws Exception
    {
        final String testValue = UUID.randomUUID();
        TrapHostingTransport host = this.listener.getHostingTransport(TrapTransportProtocol.HTTP);
        URI uri = host.addHostedObject(new TrapHostable("text/plain") {
            @Override
            public byte[] getBytes()
            {
                return testValue.getBytes();
            }
        }, "hosted");
        
        Assert.assertEquals("http", uri.getScheme());
        
        InputStream is = uri.toURL().openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        
        String string = br.readLine();
        Assert.assertEquals(testValue, string);
    }
    
    @Test(timeout=5000)
    public void testClientDisconnect() throws Exception
    {
        this.c.close();
        while(this.c.getState() != TrapState.CLOSED && this.s.getState() != TrapState.CLOSED) {
            Thread.sleep(100);
            System.out.println(this.c.getState() + " and " + this.s.getState());
        };
    }
    
    @Test(timeout=5000)
    public void testServerDisconnect() throws Exception
    {
        this.s.close();
        while(this.c.getState() != TrapState.CLOSED && this.s.getState() != TrapState.CLOSED) {
            Thread.sleep(100);
            System.out.println(this.c.getState() + " and " + this.s.getState());
        };
    }
    
    @Test(timeout=5000)
    public void testListenerClose() throws Exception
    {
        this.testServerDisconnect();
        this.listener.close();
    }
}
