package com.ericsson.research.trap.spi.sockettest;

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

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

@Ignore
public class ServletTest implements OnAccept
{
    
    @BeforeClass
    public static void setLoggerLevel()
    {
        JDKLoggerConfig.initForPrefixes(Level.FINE);
    }
    
    @Test
    public void testHostPortConfig() throws Exception
    {
        String config = "trap.transport.websocket.host = 127.0.0.1\ntrap.transport.websocket.port=51442";
        TrapListener listener = TrapFactory.createListener(config);
        listener.listen(this);
        
        Thread.sleep(100);
        String configuration = listener.getClientConfiguration();
        System.out.println(configuration);
        
        Assert.assertTrue(configuration.contains("trap.transport.websocket.wsuri = ws://127.0.0.1:51442/ws"));
        
        listener.close();
    }
    
    /*
     * Test that the autoconf host overrides regular host
     */
    @Test
    public void testAutoconfHostnamePortConfig() throws Exception
    {
        String config = "trap.transport.websocket.host = 127.0.0.1\ntrap.transport.websocket.port=51443\ntrap.transport.websocket.autoconfig.host=ericsson.com";
        TrapListener listener = TrapFactory.createListener(config);
        listener.listen(this);
        
        Thread.sleep(100);
        String configuration = listener.getClientConfiguration();
        System.out.println(configuration);
        Assert.assertTrue(configuration.contains("trap.transport.websocket.wsuri = ws://ericsson.com:51443/"));
        
        listener.close();
    }
    
    /*
     * Test that the autoconf port overrides regular port
     */
    
    @Test
    public void testAutoconfPortConfig() throws Exception
    {
        String config = "trap.transport.websocket.host = 127.0.0.1\ntrap.transport.websocket.port=51444\ntrap.transport.websocket.autoconfig.port=1000";
        TrapListener listener = TrapFactory.createListener(config);
        listener.listen(this);
        
        Thread.sleep(100);
        String configuration = listener.getClientConfiguration();
        System.out.println(configuration);
        
        Assert.assertTrue(configuration.contains("trap.transport.websocket.wsuri = ws://127.0.0.1:1000/"));
        
        listener.close();
    }
    
    /*
     * Test that the autoconf overrides regular config
     */
    
    @Test
    public void testAutoconfConfig() throws Exception
    {
        String config = "trap.transport.websocket.host = 127.0.0.1\ntrap.transport.websocket.port=51445\ntrap.transport.websocket.autoconfig.port=1000\ntrap.transport.websocket.autoconfig.host=ericsson.com";
        TrapListener listener = TrapFactory.createListener(config);
        listener.listen(this);
        
        Thread.sleep(100);
        String configuration = listener.getClientConfiguration();
        System.out.println(configuration);
        
        Assert.assertTrue(configuration.contains("trap.transport.websocket.wsuri = ws://ericsson.com:1000/"));
        
        listener.close();
    }
    
    @Override
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // TODO Auto-generated method stub
        
    }
}
