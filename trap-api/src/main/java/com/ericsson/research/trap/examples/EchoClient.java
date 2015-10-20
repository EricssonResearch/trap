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



import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * Connects to the echo server and sends messages to be echoed back.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/EchoClient.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
//BEGIN_INCLUDE(sample)
public class EchoClient implements OnOpen, OnData
{

    /** Public reference to the object */
    static EchoClient echoClient;
    
    // To run the demo, you must have at least one transport on the classpath.
    /**
     * Sets up an echo server and connects the delegate echo client to it.
     * 
     * @param args
     *            Ignored
     * @throws Throwable
     *             Hopefully not.
     */
    public static void main(String[] args) throws Throwable
    {
        // Set up a server
        EchoServer.main(args);
        
        // Create and run our client.
        echoClient = new EchoClient(EchoServer.clientConfig);
    }
    
    /** Reference to the Trap Client */
    public TrapClient client;
    
    /** Message counter */
    public int        counter = 0;
    
    /**
     * Creates and connects a new client to the given config
     * 
     * @param trapCfg
     *            The config to connect to
     * @throws TrapException
     *             Hopefully not
     */
    public EchoClient(String trapCfg) throws TrapException
    {
        // Create a new Trap Client to the specified host.
        this.client = TrapFactory.createClient(trapCfg, true);
        
        // Tell the client to use us as a delegate.
        // We'll only handle two events this time: open and data
        this.client.setDelegate(this, true);
        
        // Start connecting.
        this.client.open();
    }
    
    // Called when the client is open (=ready to send)
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        // Send a message!
        this.sendMessage();
    }
    
    // Called when the client receives data. Since we connect to the echo server, the same message should come back.
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        System.out.println("Got message: " + StringUtil.toUtfString(data));
        this.sendMessage();
    }
    
    private void sendMessage()
    {
        String message = "Message {" + this.counter++ + "}";
        System.out.println("Now sending: " + message);
        
        try
        {
            this.client.send(StringUtil.toUtfBytes(message));
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
    }
    
}
//END_INCLUDE(sample)
