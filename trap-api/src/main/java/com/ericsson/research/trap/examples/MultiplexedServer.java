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

/**
 * A server capable of echoing to the MultiplexedClient.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/MultiplexedServer.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
// BEGIN_INCLUDE(sample)
public class MultiplexedServer implements OnAccept, OnData, OnClose
{
    
    /*
     * This class implements three interfaces:
     * 
     * OnAccept - This will be called by TrapListener endpoints to notify that a new connection is established
     * OnData - TrapEndpoint objects will use this to notify our server that they have received data.
     * OnClose - TrapEndpoint objects will use this to tell us they closed.
     * 
     * The delegate interfaces are optional (though some are recommended, such as OnData). Implement only those
     * that generate events the delegate is interested in.
     */
    
    static TrapListener                echoServer;
    final static HashSet<TrapEndpoint> clients      = new HashSet<TrapEndpoint>();
    static String                      clientConfig = null;
    
    public static void main(String[] args) throws Throwable
    {
        // Create a new listener
        echoServer = TrapFactory.createListener();
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        echoServer.listen(new MultiplexedServer());
        
        // Now to tell clients how to connect. Since we elected no configuration, Trap will allocate random ports.
        clientConfig = echoServer.getClientConfiguration();
        System.out.println("New clients should connect to the following configuration:");
        System.out.println(echoServer.getClientConfiguration());
    }
    
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Endpoints received by this method are NOT strongly referenced, and may be GC'd. Thus, we must retain
        clients.add(endpoint);
        
        // This is the same config as in MultiplexedClient
        endpoint.getChannel(MultiplexedClient.HIGH_PRIO_CHANNEL).setPriority(10);
        endpoint.getChannel(MultiplexedClient.DATA_CHANNEL).setChunkSize(116);
        endpoint.getChannel(MultiplexedClient.STREAM_CHANNEL).setChunkSize(116);
        endpoint.getChannel(MultiplexedClient.HIGH_PRIO_CHANNEL).setChunkSize(116);
        endpoint.getChannel(MultiplexedClient.DATA_CHANNEL).setInFlightBytes(400);
        endpoint.getChannel(MultiplexedClient.STREAM_CHANNEL).setInFlightBytes(400);
        endpoint.getChannel(MultiplexedClient.STREAM_CHANNEL).setStreamingMode(true);
        
        // We also want feedback from the endpoint.
        endpoint.setDelegate(this, true);
    }
    
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        // Log!
        //System.out.println("Multiplexed Server Got message: [" + new String(data) + "] of length " + data.length);
        
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
