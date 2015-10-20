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



import java.util.HashSet;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * This example focuses on tweaking a server's configuration. Not all options here are applicable to all applications,
 * but they should provide a good overview of how a listener can be tweaked.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/ConfiguredServer.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
//BEGIN_INCLUDE(sample)
public class ConfiguredServer implements OnAccept, OnData, OnClose
{
    // This class focuses on the configuration options one might want to do.
    
    /** A reference to the Trap Listener */
    static TrapListener          listener;
    
    /** GC roots for the connected clients */
    static final HashSet<TrapEndpoint> clients      = new HashSet<TrapEndpoint>();
    
    /** TrapCFG for clients that wish to connect */
    static String                clientConfig = null;
    
    /**
     * Creates a new configured server
     * 
     * @param args
     *            Ignored
     * @throws Throwable
     *             Hopefully not
     */
    public static void main(String[] args) throws Throwable
    {
        // Create a new listener
        listener = TrapFactory.createListener();
        
        // If a single transport is unneeded, we can flip it off.
        listener.disableTransport("loopback");
        
        // Sometimes, allowing Trap full freedom in selecting transports is not desired.
        // In those cases, it is beneficial to provide an explicit transport selection.
        // This should only be done when needed, though.
        listener.disableAllTransports();
        listener.enableTransport("http");
        listener.enableTransport("websocket");
        listener.enableTransport("socket");
        
        // Configuration should go from most generic to most specific.
        // These options will be inherited by the endpoint, so the listener may be configured as if it were an endpoint
        listener.setOption(TrapEndpoint.OPTION_LOGGERPREFIX, "example");
        
        // Transport options will query the most specific first, then go for generic.
        // Thus, we can set the host for all transports in one swoop.
        listener.setOption("host", "localhost");
        
        // While ports are an individual choice that we should respect as such
        listener.configureTransport("http", "port", "4000");
        listener.configureTransport("websocket", "port", "4001");
        listener.configureTransport("socket", "port", "4002");
        
        // Transports can still listen to individual hosts though
        listener.configureTransport("socket", "host", "127.0.0.1");
        
        // Sometimes, the host a transport needs to LISTEN to, and the host they should PRESENT to the client will differ.
        // For example, an AWS server will listen on a private IP, but needs to tell the client to connect via the public one.
        // To that end, autoconfig.host/port are available
        listener.setOption("host", "internal.example.net");
        listener.setOption("autoconfig.host", "external.example.net");
        
        // In order to start this example, we'll need to set these to values that can run on the target machine, though.
        listener.setOption("host", "0.0.0.0");
        listener.setOption("autoconfig.host", "localhost");
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        listener.listen(new ConfiguredServer());
        
        // Now to tell clients how to connect. Since we elected no configuration, Trap will allocate random ports.
        clientConfig = listener.getClientConfiguration();
        System.out.println("New clients should connect to the following configuration:");
        System.out.println(listener.getClientConfiguration());
    }
    
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Endpoints received by this method are NOT strongly referenced, and may be GC'd. Thus, we must retain
        clients.add(endpoint);
        
        // We also want feedback from the endpoint.
        endpoint.setDelegate(this, true);
    }
    
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        // Log!
        System.out.println("Got message: " + StringUtil.toUtfString(data));
        
        try
        {
            // Echo the data back with the same parameters.
            endpoint.send(data, channel, false);
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
    }
    
    public void trapClose(TrapEndpoint endpoint, Object context)
    {
        // Remove the strong reference to allow garbage collection
        clients.remove(endpoint);
    }
    
}
//END_INCLUDE(sample)
