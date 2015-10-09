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



import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.auth.TrapAuthenticationException;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.UID;

/**
 * A simple authenticated server, using a static authentication algorithm. This example is intended to highlight how to
 * add the authentication capabilities into Trap. Trap itself does not ship with any.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/AuthenticatedEchoServer.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
//BEGIN_INCLUDE(sample)
public class AuthenticatedEchoServer implements OnAccept, OnData, OnClose
{
    /*
	 * Let's create a new authenticator. This will be a very simple authenticator. It will attach a random value to the message ID and test that they match.
	 * @formatter:off
	 */
	/** A simple authenticator implementation. Has very little state. */
	static TrapAuthentication authenticator = new TrapAuthentication() {
		
		String secret = UID.randomUID();
		
		public boolean verifyAuthentication(TrapMessage message, Map<String, String> headers, Map<String, Object> context) throws TrapAuthenticationException
		{
			String authData = message.getAuthData();
			String expected = this.secret + "/" + message.getMessageId();
			
			if (authData == null)
				System.out.println("Huh");
			
			System.out.println("Verifying auth data [" + authData + "] against expected [" + expected + "] for transport " + ((TrapTransport) context.get(TrapContextKeys.Transport)).getTransportName() + " and message " + message);
			
			return expected.equals(authData);
		}
		
		public Collection<String> getContextKeys(Collection<String> availableKeys) throws TrapException
		{
			System.out.println("Being added to a transport... Got offered keys: " + availableKeys);
			return availableKeys;
		}
		
		public String createAuthenticationResponse(TrapMessage challenge, TrapMessage outgoing, Map<String, String> headers, Map<String, Object> context)
		{
			// There's no challenge
			System.out.println("Crafting auth response for " + outgoing);
			String expected = this.secret + "/" + outgoing.getMessageId();
			return expected;
		}
		
		public String createAuthenticationChallenge(TrapMessage src, Map<String, Object> context)
		{
			// No challenge is ever created
			return "";
		}
	};
	
	/* @formatter:on
	 * This class implements three interfaces:
	 * 
	 * OnAccept – This will be called by TrapListener endpoints to notify that a new connection is established
	 * OnData – TrapEndpoint objects will use this to notify our server that they have received data.
	 * OnClose – TrapEndpoint objects will use this to tell us they closed.
	 * 
	 * The delegate interfaces are optional (though some are recommended, such as OnData). Implement only those
	 * that generate events the delegate is interested in.
	 */
    /** A reference to the listener, so we can shut it off */
    static TrapListener             echoServer;
    
    /** References to the connected endpoints, to prevent them from being garbage collected */
    static final HashSet<TrapEndpoint> clients       = new HashSet<TrapEndpoint>();
    
    /** Trap configuration for clients that wish to connect */
    static String                      clientConfig  = null;
    
    /**
     * Starts an authenticated echo server
     * 
     * @param args
     *            None
     * @throws Throwable
     *             Hopefully not.
     */
    public static void main(String[] args) throws Throwable
    {
        // Create a new listener
        echoServer = TrapFactory.createListener();
        
        echoServer.disableTransport("loopback");
        echoServer.disableTransport("asyncloopback");
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        echoServer.listen(new AuthenticatedEchoServer());
        
        // Now to tell clients how to connect. Since we elected no configuration, Trap will allocate random ports.
        clientConfig = echoServer.getClientConfiguration();
        System.out.println("New clients should connect to the following configuration:");
        System.out.println(echoServer.getClientConfiguration());
    }
    
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Endpoints received by this method are NOT strongly referenced, and may be GC'd. Thus, we must retain
        clients.add(endpoint);
        
        // Add the authenticator
        try
        {
            endpoint.setAuthentication(authenticator);
        }
        catch (TrapException e)
        {
            // This will happen if the authenticator cannot negotiate context parameters with the transports.
            // We know it won't happen because our authenticator does no negotiation.
            e.printStackTrace();
        }
        
        // We also want feedback from the endpoint.
        endpoint.setDelegate(this, true);
    }
    
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        // Log!
        System.out.println("Echo Server Got message: [" + StringUtil.toUtfString(data) + "] of length " + data.length);
        
        try
        {
            // Echo the data back with the same parameters.
            Thread.sleep(200);
            endpoint.send(data, channel, false);
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
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

