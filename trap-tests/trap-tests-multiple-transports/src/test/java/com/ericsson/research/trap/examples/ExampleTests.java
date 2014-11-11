package com.ericsson.research.trap.examples;

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

import org.junit.Test;

import com.ericsson.research.trap.examples.AuthenticatedEchoClient;
import com.ericsson.research.trap.examples.AuthenticatedEchoServer;
import com.ericsson.research.trap.examples.AutoConfiguredServer;
import com.ericsson.research.trap.examples.ConfiguredClient;
import com.ericsson.research.trap.examples.ConfiguredServer;
import com.ericsson.research.trap.examples.DelegateEchoClient;
import com.ericsson.research.trap.examples.EchoClient;
import com.ericsson.research.trap.examples.EchoServer;
import com.ericsson.research.trap.examples.MultiplexedClient;
import com.ericsson.research.trap.examples.MultiplexedServer;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

// This class should test the examples in the Trap API
public class ExampleTests
{
    @Test(timeout = 4000)
    public void testEcho() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        EchoClient.main(null);
        
        while (EchoClient.echoClient.counter < 10)
            Thread.sleep(10);
        
        EchoClient.echoClient.client.close();
        EchoServer.echoServer.close();
        EchoClient.echoClient = null;
        EchoServer.echoServer = null;
    }
    
    @Test(timeout = 4000)
    public void testConfigured() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        ConfiguredServer.main(null);
        ConfiguredClient client = new ConfiguredClient(ConfiguredServer.clientConfig);
        
        while (client.counter < 10)
            Thread.sleep(10);
        
        client.client.close();
        ConfiguredServer.listener.close();
        ConfiguredServer.listener = null;
        client = null;
    }
    
    @Test(timeout = 4000)
    public void testMultiplexed() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        MultiplexedClient.main(null);
        
        while (!MultiplexedClient.multiplexedClient.allDone)
            Thread.sleep(10);
        
        MultiplexedClient.multiplexedClient.client.close();
        MultiplexedServer.echoServer.close();
        MultiplexedClient.multiplexedClient = null;
        MultiplexedServer.echoServer = null;
    }
    
    @Test(timeout = 4000)
    public void testAuthenticated() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        AuthenticatedEchoClient.main(null);
        
        while (AuthenticatedEchoClient.echoClient.counter < 2)
            Thread.sleep(10);
        
        AuthenticatedEchoClient.echoClient.client.close();
        AuthenticatedEchoServer.echoServer.close();
        AuthenticatedEchoClient.echoClient = null;
        AuthenticatedEchoServer.echoServer = null;
    }
    
    @Test(timeout = 4000)
    public void testDelegate() throws Throwable
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
        DelegateEchoClient.main(null);
        
        while (DelegateEchoClient.echoClient.counter < 10)
            Thread.sleep(10);
        
        DelegateEchoClient.echoClient.client.close();
        EchoServer.echoServer.close();
        DelegateEchoClient.echoClient = null;
        EchoServer.echoServer = null;
    }

    @Test(timeout = 4000000)
    public void testAutoConfig() throws Throwable
    {

        JDKLoggerConfig.initForPrefixes(Level.INFO);
        AutoConfiguredServer.main(null);
        EchoClient client = new EchoClient(AutoConfiguredServer.clientConfig);
        
        while(client.counter < 2)
            Thread.sleep(10);

        AutoConfiguredServer.echoServer.close();
        AutoConfiguredServer.echoServer = null;
        client.client.close();
        
    }
}
