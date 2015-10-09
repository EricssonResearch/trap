package com.ericsson.research.trap;

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



import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnStateChange;

/**
 * A TrapListener is a special kind of Trap node capable of listening for incoming connections. It is employed by
 * servers, to await incoming Trap connections. A TrapListener is not an endpoint, despite being created by TrapFactory.
 * TrapListener instances will spawn TrapEndpoints on incoming connections, however.
 * <p>
 * Any configuration set using the {@link TrapSettings} interface on a TrapListener will carry over to spawned
 * TrapEndpoints. Additionally, authentication set here will carry over.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public interface TrapListener extends TrapSettings
{
    /**
     * Stops the Trap Listener. This will close all listening transports, which may affect existing connections. For
     * example, the HTTP transport relies on the listening socket for "established" connections, so any HTTP connections
     * will be terminated.
     */
    public abstract void close();
    
    /**
     * Asks the server to generate a client configuration string, in order to configure the transports to properly
     * connect to this TrapServer instance. Each TrapTransport that supports listening will generate this configuration,
     * and the entire string can then be supplied as-is to the TrapClient.
     * 
     * @return The client configuration string needed to connect here.
     */
    public abstract String getClientConfiguration();
    
    /**
     * Asks the server to generate a client configuration string, in order to configure the transports to properly
     * connect to this TrapServer instance. Each TrapTransport that supports listening will generate this configuration,
     * and the entire string can then be supplied as-is to the TrapClient.
     * 
     * @param hostname
     *            The hostname to use for this host. This hostname will be used for any transport that has not been
     *            explicitly configured.
     * @return The client configuration string needed to connect here
     * @since 1.2.0
     */
    public abstract String getClientConfiguration(String hostname);
    
    /**
     * Starts the TrapListener.
     * <p>
     * Associates a given {@link OnAccept} deelgate to listen for incoming requests, and handle them. The delegate is
     * responsible for initialising the TrapEndoints it receives, by means of configuring them to fit the current
     * server's requirements on blocking, threading, etc.
     * 
     * @param delegate
     *            The object that will be called to handle incoming connections. The delegate MAY implement further
     *            TrapDelegate methods; however, a listener endpoint will not call any other (except
     *            {@link OnStateChange}/{@link OnClose}).
     * @throws TrapException
     *             If an error occurred during the initialization stage of the listeners.
     * @since 1.1
     */
    public abstract void listen(OnAccept delegate) throws TrapException;
    
    /**
     * Starts the TrapListener.
     * <p>
     * Associates a given {@link TrapListenerDelegate} to listen for incoming requests, and handle them. The delegate is
     * responsible for initialising the TrapEndoints it receives, by means of configuring them to fit the current
     * server's requirements on blocking, threading, etc.
     * 
     * @param delegate
     *            The object that will be called to handle incoming connections
     * @param context
     *            An optional object that will be passed to the delegate on each callback
     * @throws TrapException
     *             If an error occurred during the initialization stage of the listeners.
     * @deprecated Use {@link #listen(OnAccept)} instead.
     */
    public abstract void listen(OnAccept delegate, Object context) throws TrapException;
    
    /**
     * Sets an authenticator for this listener. Effectively requires any incoming connection to be authenticated.
     * <p>
     * Note that the instance will be shared between any endpoint. This is only good for static authentication types.
     * For most use cases, authentication should be applied on an individual basis.
     * 
     * @param authenticator
     *            The authenticator to verify incoming authentication
     * @throws TrapException
     *             If the authentication was not acceptable.
     */
    public abstract void setAuthentication(TrapAuthentication authenticator) throws TrapException;
}
