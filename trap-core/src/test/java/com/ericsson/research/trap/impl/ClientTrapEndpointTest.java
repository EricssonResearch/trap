package com.ericsson.research.trap.impl;

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

import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.trap.TestTransport1;
import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapKeepalivePredictor;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.transports.AbstractTransport;

public class ClientTrapEndpointTest
{
    
    ListenerTrapEndpoint lte;
    
    @Before
    public void setUp() throws Exception
    {
        this.lte = new ListenerTrapEndpoint();
        this.lte.configure(null);
        class LteTransport extends AbstractTransport implements ListenerTrapTransport
        {
            
            public boolean canConnect()
            {
                return true;
            }
            
            public boolean canListen()
            {
                return true;
            }
            
            public String getTransportName()
            {
                return "LteTransport";
            }
            
            public String getProtocolName()
            {
                return "lte";
            }
            
            public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
            {
            }
            
            protected void internalConnect() throws TrapException
            {
            }
            
            protected void internalDisconnect()
            {
            }
            
            public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
            {
            }
            
            public void fillAuthenticationKeys(HashSet<String> keys)
            {
            }
            
			public void init()
            {
            }
            
            public TrapKeepalivePredictor getKeepalivePredictor()
            {
                return null;
            }
            
            public void setKeepalivePredictor(TrapKeepalivePredictor newPredictor)
            {
            }
            
            public void setKeepaliveExpiry(long newExpiry)
            {
            }
            
            public boolean isConfigured(boolean client, boolean server)
            {
                return true;
            }
            
            public void flushTransport()
            {
                // TODO Auto-generated method stub
                
            }
            
            public void getClientConfiguration(TrapConfiguration destination, String hostname)
            {
                // TODO Auto-generated method stub
                
            }
        }
        this.lte.addTransport(new LteTransport());
    }
    
    @Test
    public void testConnect() throws TrapException
    {
        ClientTrapEndpoint cte = new ClientTrapEndpoint(null, false);
        
        TrapTransport t1 = this.createTransport("transport1");
        cte.addTransport(t1);
        t1.setTransportDelegate(cte, null);
        
        TrapTransport t2 = this.createTransport("transport2");
        cte.addTransport(t2);
        t2.setTransportDelegate(cte, null);
        
        cte.open();
    }
    
    @Test
    public void testHttpConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("http://traphost.com", false);
        Assert.assertEquals("trap.transport.http.host = traphost.com\ntrap.transport.http.port = 80\ntrap.transport.http.url = http://traphost.com", cte.getConfiguration().trim());
    }
    
    @Test
    public void testHttpsConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("https://traphost.com", false);
        Assert.assertEquals("trap.transport.http.host = traphost.com\ntrap.transport.http.port = 443\ntrap.transport.http.url = https://traphost.com", cte.getConfiguration().trim());
    }
    
    @Test
    public void testWsConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("ws://traphost.com", false);
        Assert.assertEquals("trap.transport.websocket.host = traphost.com\ntrap.transport.websocket.port = 80\ntrap.transport.websocket.wsuri = ws://traphost.com", cte.getConfiguration().trim());
    }
    
    @Test
    public void testWssConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("wss://traphost.com", false);
        Assert.assertEquals("trap.transport.websocket.host = traphost.com\ntrap.transport.websocket.port = 443\ntrap.transport.websocket.wsuri = wss://traphost.com", cte.getConfiguration().trim());
    }
    
    @Test
    public void testSocketConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("socket://traphost.com:443", false);
        Assert.assertEquals("trap.transport.socket.host = traphost.com\ntrap.transport.socket.port = 443", cte.getConfiguration().trim());
    }
    
    @Test
    public void testSocketsConfig() throws Exception
    {
        TrapClient cte = TrapFactory.createClient("sockets://traphost.com:443", false);
        Assert.assertEquals("trap.transport.socket.host = traphost.com\ntrap.transport.socket.port = 443\ntrap.transport.socket.secure = true", cte.getConfiguration().trim());
    }
    
    @Test
    public void testAddTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt = new TestTransport1("foo", "foo", true, true);
        
        cte.addTransport(tt);
        
        Assert.assertTrue(cte.getTransports().contains(tt));
    }
    
    @Test
    public void testEnableTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt = new TestTransport1("foo", "foo", true, true);
        
        if (tt.isEnabled())
            tt.disable();
        
        Assert.assertFalse(tt.isEnabled());
        
        cte.addTransport(tt);
        
        Assert.assertTrue(cte.getTransports().contains(tt));
        
        cte.enableTransport("foo");
        Assert.assertTrue(tt.isEnabled());
        
        // Ensure that re-enabling doesn't change anything
        cte.enableTransport("foo");
        Assert.assertTrue(tt.isEnabled());
        
        
    }
    
    @Test
    public void testReplaceTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt1 = new TestTransport1("foo", "foo", true, true);
        TrapTransport tt2 = new TestTransport1("foo", "foo", true, true);

        cte.addTransport(tt1);
        Assert.assertTrue(cte.getTransports().contains(tt1));
        
        cte.addTransport(tt2);
        Assert.assertTrue(cte.getTransports().contains(tt2));
        Assert.assertFalse(cte.getTransports().contains(tt1));
    }
    
    @Test
    public void testNonReplaceTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt1 = new TestTransport1("foo", "foo", true, true);
        TrapTransport tt2 = new TestTransport1("foo", "foo", true, true);
        
        tt1.setTransportPriority(0);
        tt2.setTransportPriority(1);

        cte.addTransport(tt1);
        Assert.assertTrue(cte.getTransports().contains(tt1));
        
        cte.addTransport(tt2);
        Assert.assertFalse(cte.getTransports().contains(tt2));
        Assert.assertTrue(cte.getTransports().contains(tt1));
    }
    
    @Test
    public void testSkipInvalidTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt1 = new TestTransport1("foo", "foo", false, false);
        
        cte.addTransport(tt1);
        Assert.assertFalse(cte.getTransports().contains(tt1));
        
    }
    
    @Test
    public void testRemoveTransport() throws Exception
    {
        ClientTrapEndpoint cte = (ClientTrapEndpoint) TrapFactory.createClient("sockets://traphost.com:443", false);
        TrapTransport tt = new TestTransport1("foo", "foo", true, true);
        
        cte.addTransport(tt);
        Assert.assertTrue(cte.getTransports().contains(tt));
        
        cte.removeTransport(tt);
        Assert.assertFalse(cte.getTransports().contains(tt));
    }
    
    private TrapTransport createTransport(final String name)
    {
        return new AbstractTransport() {
            
            public boolean canListen()
            {
                return true;
            }
            
            public String getTransportName()
            {
                return name;
            }
            
            public String getProtocolName()
            {
                return "lte";
            }
            
            public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
            {
                System.out.println(this.getTransportName() + " is sending " + message);
                //setState(TrapTransportState.ERROR);
            }
            
            protected void internalDisconnect()
            {
            }
            
            protected void internalConnect() throws TrapException
            {
                this.setState(TrapTransportState.CONNECTED);
            }
            
            public void fillAuthenticationKeys(HashSet<String> keys)
            {
            }
            
			public void init()
            {
                // TODO Auto-generated method stub
                
            }
            
            public boolean canConnect()
            {
                return true;
            }
            
            public void flushTransport()
            {
                // TODO Auto-generated method stub
                
            }
        };
    }
    
}
