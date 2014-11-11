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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.TrapTransports;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.transports.AbstractTransport;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.UUID;

/**
 * This endpoint listens for incoming connections. It must map incoming connections to existing endpoints, if so
 * available. This mapping must also key the transport implementation to send future reconnects to the same endpoint, if
 * still available
 * 
 * @author Vladimir Katardjiev
 */
public class ListenerTrapEndpoint extends TrapEndpointImpl implements ListenerTrapTransportDelegate, TrapListener
{
    // currently live spawned endpoints
    // Type <String, WeakReference<ServerTrapEndpoint>>
    private final Map<String, WeakEndpointReference>                 endpoints                  = Collections.synchronizedMap(new HashMap<String, WeakEndpointReference>());
    // newly born transports
    // Type <TrapTransport>
    private final Set<TrapTransport>                                 sTransports                = Collections.synchronizedSet(new HashSet<TrapTransport>());
    
    /*
     * It is possible for an endpoint to attempt to multi-connect transports (i.e. connect multiple transports simultaneously).
     * With no further coordination, these transports would spawn separate endpoints on the server end. But the server cannot
     * coordinate them as the client doesn't know which will first-succeed.
     *
     * The solution is simple; each client will generate a unique identifier to map transports from the same client together.
     * This identifier must be long, include a client-specific part, a temporal part and a random part, and is only valid for 30 seconds.
     * There is a risk that if someone guesses this identifier during the 30-second window, he or she can attach to a Trap session
     * without permission (if the session is unauthenticated), but the risk of guessing such a short-lived and high-entropy passphrase is
     * minimal.
     *
     * After 30 seconds, this timer expires. The timer is configurable via the configuration parameter trap.concurrent-connection-window
     */
    
    private long                                                     concurrentConnectionWindow = 30000;
    
    // Type <String, ServerTrapEndpoint>
    private final HashMap<String, WeakReference<ServerTrapEndpoint>> ccEndpoints                = new HashMap<String, WeakReference<ServerTrapEndpoint>>();
    
    // These variables are used to generate Trap IDs, as well as cleanup IDs that are no longer in use.
    private final ReferenceQueue<TrapEndpoint>                       endpointRefs               = new ReferenceQueue<TrapEndpoint>();                                       // Queue used to clean up garbage collected endpoints
                                                                                                                                                                             
    public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
    {
        if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
            this.sTransports.remove(transport);
    }
    
    public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
    {
        if (message.getOp() != Operation.OPEN)
        {
            try
            {
                transport.send(new TrapMessageImpl().setOp(Operation.ERROR).setFormat(message.getFormat()), false);
            }
            catch (TrapTransportException e)
            {
            }
            transport.disconnect();
            this.logger.info("Disconnecting transport with ID [{}] due to the first message operation not being open; it was [{}]", new Object[] { transport.getTransportName(), message.getOp() });
            return;
        }
        
        this.logger.debug("New OPEN message. Body length is " + (message.getData() != null ? message.getData().length : 0));
        
        // Parse body. We'll need some of the information here...
        
        TrapConfigurationImpl cfg = new TrapConfigurationImpl(StringUtil.toUtfString(message.getData()));
        String trapId = cfg.getOption(TrapConstants.ENDPOINT_ID);
        
        if (trapId != null && !TrapConstants.ENDPOINT_ID_CLIENT.equalsIgnoreCase(trapId))
        {
            WeakEndpointReference r = this.endpoints.get(trapId);
            TrapEndpointImpl e = null;
            
            if (r != null)
                e = (TrapEndpointImpl) r.get();
            
            if (e == null)
            {
                try
                {
                    transport.send(new TrapMessageImpl().setOp(Operation.ERROR).setFormat(message.getFormat()), false);
                    transport.flushTransport();
                }
                catch (TrapTransportException e1)
                {
                }
                transport.disconnect();
                this.logger.info("Disconnecting transport with ID [{}] due to it trying to connect to non-existent TrapEndpoint session; id [{}]", new Object[] { transport.getTransportName(), trapId });
                
                // We have guaranteed run into a condition whereby we need to clean up.
                ThreadPool.executeCached(this.cleanupTask);
                return;
            }
            
            this.logger.debug("Adding new transport to TrapEndpoint ID {}", trapId);
            e.addTransport(transport, message);
            
        }
        else
        {
            try
            {
                final String token = cfg.getOption(TrapConstants.CONNECTION_TOKEN);
                
                synchronized (this.ccEndpoints)
                {
                    WeakReference<ServerTrapEndpoint> ref = this.ccEndpoints.get(token);
                    ServerTrapEndpoint ep = ref == null ? null : (ServerTrapEndpoint) ref.get();
                    
                    if (ep == null)
                    {
                        this.logger.debug("Creating new TrapEndpoint in response to new transport");
                        ServerTrapEndpoint e = new ServerTrapEndpoint(this);
                        
                        // Propagate all settings to the new endpoint
                        e.configure(this.getConfiguration());
                        e.setAuthentication(this.getAuthentication());
                        e.setBlockingTimeout(this.getBlockingTimeout());
                        e.setQueueType(this.getQueueType());
                        e.setTrapID(UUID.randomUUID());
                        e.setTrapFormat(message.getFormat());
                        
                        WeakEndpointReference r = new WeakEndpointReference(e, this.endpointRefs);
                        r.id = e.getTrapID();
                        
                        // Store it for future connections
                        this.endpoints.put(e.getTrapID(), r);
                        
                        e.setState(TrapState.OPEN);
                        
                        // Notify of new endpoint. NOTE: This means OPEN will never be triggered on server endpoints
                        // This is quite a departure from the previous mechanics.
                        // Moving this until before the transport is added, in order to allow for authentication to be set in a proper place.
                        try
                        {
                            this.acceptDelegate.incomingTrapConnection(e, this, this.delegateContext);
                        }
                        catch (Throwable t)
                        {
                            this.logger.error("Exception while dispatching incoming trap connection", t);
                        }
                        
                        // Attach transport to endpoint, continuing auth
                        e.addTransport(transport, message);
                        
                        // Remove the transport from us for Garbage Collection
                        this.sTransports.remove(transport);
                        
                        if (token != null)
                        {
                            
                            // Add the endpoint to the cached ones.
                            this.ccEndpoints.put(token, new WeakReference<ServerTrapEndpoint>(e));
                            
                            // Schedule a task to remove the endpoint from the cached ones
                            ThreadPool.executeAfter(new Runnable() {
                                
                                public void run()
                                {
                                    synchronized (ListenerTrapEndpoint.this.ccEndpoints)
                                    {
                                        ListenerTrapEndpoint.this.ccEndpoints.remove(token);
                                    }
                                }
                            }, this.concurrentConnectionWindow);
                        }
                    }
                    else
                    {
                        // Add the transport, preventing duplicates
                        ep.addTransport(transport, message);
                        this.sTransports.remove(transport);
                    }
                }
                
            }
            catch (TrapException e)
            {
                this.logger.warn(e.getMessage(), e);
            }
        }
    }
    
    public ListenerTrapEndpoint() throws TrapException
    {
        super();
        this.trapID = "Listener";
        Class<TrapTransport>[] transports;
        
        // Find all available transports
        try
        {
            transports = TrapTransports.getTransportClasses(this.getClass().getClassLoader());
        }
        catch (Exception e)
        {
            throw new TrapException("Error while scanning for the transports", e);
        }
        
        // Instantiate them and add to our list
        for (int i = 0; i < transports.length; i++)
        {
            try
            {
                if (!TrapTransport.class.isAssignableFrom(transports[i]) || transports[i].isAssignableFrom(AbstractTransport.class))
                    continue;
                
                this.logger.trace("Found class: {}", transports[i].getName());
                TrapTransport t = transports[i].newInstance();
                
                // If it's not capable of listening, we're not interested.
                if (!t.canListen())
                    continue;
                
                if (this.config != null)
                    t.setConfiguration(this.config);
                
                this.addTransport(t);
            }
            catch (InstantiationException e1)
            {
                // Probably not a problem, log it as debug
                this.logger.debug("Transport class [{}] failed to instantiate. This is most probably a transport that is not meant to be instantiated here.", transports[i].getName());
            }
            catch (IllegalAccessException e1)
            {
                // Probably not a problem, log it as debug
                this.logger.debug("Transport class [{}] failed to instantiate. This is most probably a transport that is not meant to be instantiated here.", transports[i].getName());
            }
            catch (Exception e)
            {
                this.logger.warn("Failed to instantiate " + transports[i].getName(), e);
            }
        }
    }
    
    /*
     * On an incoming connection, we need to perform two things. First, we need to add the listener as a delegate of the transport. It will wait for an incoming message, and, once it arrives,
     * dispatch the transport to the appropriate recipient (or a new endpoint, if applicable).
     *
     * (non-Javadoc)
     * @see com.ericsson.research.trap.spi.ListenerTrapTransportDelegate#ttsIncomingConnection(com.ericsson.research.trap.spi.TrapTransport, com.ericsson.research.trap.spi.ListenerTrapTransport, java.lang.Object)
     */
    public void ttsIncomingConnection(final TrapTransport connection, ListenerTrapTransport server, Object context)
    {
        connection.setTransportDelegate(this, null);
        this.sTransports.add(connection);
        
        /*
         * Add a timeout (30 secs) for a transport to get out of sTransports or be forcibly removed.
         */
        
        final WeakReference<TrapTransport> weakConnection = new WeakReference<TrapTransport>(connection);
        
        ThreadPool.executeAfter(new Runnable() {
            
            public void run()
            {
                TrapTransport connection = weakConnection.get();
                
                if (connection != null)
                    ListenerTrapEndpoint.this.sTransports.remove(connection);
                
                while (ListenerTrapEndpoint.this.endpointRefs.poll() != null)
                {
                    try
                    {
                        WeakEndpointReference ref = (WeakEndpointReference) ListenerTrapEndpoint.this.endpointRefs.remove(1);
                        if (ref == null)
                            return;
                        ListenerTrapEndpoint.this.endpoints.remove(Integer.valueOf(ref.id).toString());
                    }
                    catch (IllegalArgumentException e)
                    {
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
            
        }, 30000);
    }
    
    /**
     * @deprecated
     */
    public void listen(OnAccept delegate, Object context) throws TrapException
    {
        this.setDelegateContext(context);
        this.listen(delegate);
    }
    
    public void listen(OnAccept delegate) throws TrapException
    {
        this.setDelegate(delegate, true);
        
        // Now enable listening
        for (int i = 0; i < this.transports.size(); i++)
        {
            ListenerTrapTransport t = (ListenerTrapTransport) this.transports.get(i);
            // Propagate config
            if (t.isEnabled())
                t.listen(this, null);
        }
        
        this.setState(TrapState.OPEN);
    }
    
    public void setDelegateContext(Object delegateContext)
    {
        this.delegateContext = delegateContext;
    }
    
    public String getClientConfiguration()
    {
        return this.getClientConfiguration(null);
    }
    
    public String getClientConfiguration(String hostname)
    {
        try
        {
            TrapConfigurationImpl out = new TrapConfigurationImpl();
            
            if (hostname != null)
                out.setOption(TrapEndpoint.OPTION_AUTO_HOSTNAME, hostname);
            
            for (int i = 0; i < this.transports.size(); i++)
            {
                ListenerTrapTransport t = ((ListenerTrapTransport) this.transports.get(i));
                if (t.isEnabled())
                    t.getClientConfiguration(out, hostname);
            }
            return out.toString();
        }
        catch (AutoconfigurationDisabledException e)
        {
            this.logger.debug("Auto configuration disabled due to improperly configured transport; ", e.getMessage());
            return "";
        }
    }
    
    protected void reconnect(long timeout)
    {
        throw new IllegalStateException("Cannot reconnect a listener. What you're doing is strange.");
    }
    
    /**
     * This stored task will be executed on incoming connections or received messages to clear out old connections from
     * RAM. This does imply there is a lag between an endpoint/transport ID clearing up, and it becoming available. For
     * this reason, this task will be invoked synchronously as well (hence it is synchronized).
     */
    //@formatter:off

	Runnable	cleanupTask	= new Runnable()
	{

		public synchronized void run()
		{
			WeakEndpointReference r;
			while ((r = (WeakEndpointReference) ListenerTrapEndpoint.this.endpointRefs.poll()) != null)
			{
				ListenerTrapEndpoint.this.endpoints.remove(Integer.valueOf(r.id).toString());
			}
		}

	};

	//@formatter:on
    
    public void setAuthentication(TrapAuthentication authentication) throws TrapException
    {
        this.authentication = authentication;
    }
    
    public void close()
    {
        this.setState(TrapState.CLOSING);
        super.onEnd(null, null);
    }
    
    public void configure(String configuration)
    {
        super.configure(configuration);
        this.concurrentConnectionWindow = this.config.getIntOption("trap.concurrent-connection-window", (int) this.concurrentConnectionWindow);
    }
    
}

class WeakEndpointReference extends WeakReference<TrapEndpoint>
{
    
    protected String id;
    
    public WeakEndpointReference(TrapEndpoint referent, ReferenceQueue<TrapEndpoint> q)
    {
        super(referent, q);
    }
    
}
