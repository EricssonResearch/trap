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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.TrapTransports;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.transports.AbstractTransport;
import com.ericsson.research.trap.utils.HexConverter;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.UUID;

public class ClientTrapEndpoint extends TrapEndpointImpl implements TrapClient, TrapTransportDelegate
{
    public static final String    TRANSPORT_RECONNECT_TIMEOUT = null;
    
    protected boolean             autoConfigure;
    
    /**
     * The list of transports that the recovery thread is trying to connect. This list must be sorted.
     */
    protected Set<TrapTransport>  transportsToConnect         = Collections.synchronizedSet(new TreeSet<TrapTransport>(new Comparator<TrapTransport>() {
                                                                  public int compare(TrapTransport o1, TrapTransport o2)
                                                                  {
                                                                      // Sort untested transports according to priority; smallest priority first.
                                                                      // Note: I've flipped the order of o1 and o2 in the comparator, based on the docs.
                                                                      // This should give the correct sort order of the list.
                                                                      int rv = (o1.getTransportPriority() - o2.getTransportPriority());
                                                                      
                                                                      // Ensure that the transports with equal priority are sorted (i.e. allow unique objects with the same prio, at undefined order)
                                                                      if (rv == 0)
                                                                          return o1.hashCode() - o2.hashCode();
                                                                      else
                                                                          return rv;
                                                                  }
                                                              }));
    
    /**
     * The transports that have failed in some non-fatal way. There will be an attempt taken in the future to recover
     * them.
     */
    protected List<TrapTransport> failedTransports            = Collections.synchronizedList(new LinkedList<TrapTransport>());
    
    protected List<TrapTransport> activeTransports            = Collections.synchronizedList(new LinkedList<TrapTransport>());
    
    protected Object              recoveryLock                = new Object();
    protected boolean             recovering                  = false;
    
    /**
     * The amount of time before a transport is automatically considered for recovery
     */
    long                          transportRecoveryTimeout    = 15 * 60 * 1000;
    
    private String                connectionToken;
    
    public ClientTrapEndpoint(String configuration, Boolean autoConfigure) throws TrapException
    {
        super();
        this.autoConfigure = autoConfigure.booleanValue();
        this.trapID = TrapConstants.ENDPOINT_ID_CLIENT; // Allow the server to override our trap ID.
        
        // Load the appropriate transports
        try
        {
            Class<TrapTransport>[] transports = TrapTransports.getTransportClasses(this.getClass().getClassLoader());
            
            for (int i = 0; i < transports.length; i++)
            {
                if (!TrapTransport.class.isAssignableFrom(transports[i]) || transports[i].isAssignableFrom(AbstractTransport.class))
                    continue;
                
                TrapTransport t;
                
                try
                {
                    t = transports[i].newInstance();
                }
                catch (Exception e)
                {
                    this.logger.debug("Failed to instantiate {}; Most probably a server transport...", transports[i].getName(), e);
                    continue;
                }
                
                if (!t.canConnect())
                    continue;
                
                this.addTransport(t);
                t.setTransportDelegate(this, null);
            }
        }
        catch (Exception e)
        {
            throw new TrapException(e);
        }
        
        this.configure(configuration);
    }
    
    public void configure(String configuration)
    {
        super.configure(configuration);
        
        this.transportRecoveryTimeout = this.config.getLongOption(TRANSPORT_RECONNECT_TIMEOUT, this.transportRecoveryTimeout);
    }
    
    protected TrapConfiguration parseConfiguration(String configuration)
    {
        return new TrapCustomConfiguration(configuration);
    }
    
    public void open() throws TrapException
    {
        
        this.logger.trace("##### CLIENT OPEN ####");
        this.logger.trace("Config is: {}", this.config.toString());
        for (int i = 0; i < this.transports.size(); i++)
        {
            TrapTransport t = this.transports.get(i);
            
            this.logger.trace("Transport [{}] is enabled: {}", t.getTransportName(), Boolean.toString(t.isEnabled()));
        }
        
        this.setState(TrapState.OPENING);
        this.doOpen();
        
        // Start a runnable, every so often,
        ThreadPool.executeAfter(new RecoveryHeartbeat(this), this.transportRecoveryTimeout); // Recover transports after 15 minutes.
    }
    
    /*
     * The semantics of doOpen() are altered slightly. Instead of immediately connecting a transport, doOpen will instead
     * activate the recovery thread. The recovery thread is the connectingThread instead, and it will try to connect any
     * transport it is given.
     *
     * This change allows the connecting thread to cover both transports connecting normally (connect) and recovering. Removing
     * this distinction will reduce the amount of places where transports are asked to connect.
     */
    protected void doOpen() throws TrapException
    {
        
        synchronized (this.transportsToConnect)
        {
            // If the list of transports that can be used is empty -> die!
            if (this.transports.size() == 0)
            {
                this.setState(TrapState.ERROR);
                throw new TrapException("No transports available");
            }
            
            // Clear all transport state lists
            this.failedTransports.clear();
            this.availableTransports.clear();
            this.activeTransports.clear();
            this.transportsToConnect.clear();
            
            // Let untestedTransports be the list of transports that we haven't tried.
            synchronized (this.transports)
            {
                this.transportsToConnect.addAll(this.transports);
            }
        }
        
        // Start the recovery thread, which will connect us
        this.kickRecoveryThread();
    }
    
    // One of our transports has changed the state, let's see what happened...
    
    public synchronized void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
    {
        // Super will manage available transports. All we need to consider is what action to take.
        super.ttStateChanged(newState, oldState, transport, context);
        
        // Always remove from active, to clear any confusion... 
        if (newState == TrapTransportState.DISCONNECTING || newState == TrapTransportState.DISCONNECTED || newState == TrapTransportState.ERROR)
            this.activeTransports.remove(transport);
        
        // Don't trigger any recoveries if we've asked to close.
        if ((this.getState() == TrapState.CLOSED) || (this.getState() == TrapState.CLOSING) || (this.getState() == TrapState.ERROR))
            return;
        
        // What to do if we lose a transport
        if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
        {
            
            this.availableTransports.remove(transport);
            this.activeTransports.remove(transport);
            
            if (this.logger.isDebugEnabled())
            {
                StringBuilder logMsg = new StringBuilder();
                logMsg.append("Lost a transport. TrapEndpont state is ");
                logMsg.append(this.getState().toString());
                logMsg.append(". I have ");
                logMsg.append(this.activeTransports.size());
                logMsg.append(" active transports. Name/State follows... ");
                
                synchronized (this.activeTransports)
                {
                    Iterator<TrapTransport> it = this.activeTransports.iterator();
                    
                    while (it.hasNext())
                    {
                        TrapTransport at = it.next();
                        logMsg.append(at.getTransportName());
                        logMsg.append("/");
                        logMsg.append(at.getState());
                        logMsg.append(", ");
                    }
                }
                
                this.logger.debug(logMsg.toString());
            }
            
            if (this.getState() == TrapState.SLEEPING && System.currentTimeMillis() >= this.canReconnectUntil)
            {
                this.logger.debug("Timer expired on reconnect");
                this.setState(TrapState.CLOSED);
                return;
            }
            
            // This was an already connected transport. If we have other transports available, we should silently try and reconnect it in the background
            if ((oldState == TrapTransportState.AVAILABLE) || (oldState == TrapTransportState.UNAVAILABLE) || (oldState == TrapTransportState.CONNECTED))
            {
                
                if (this.activeTransports.size() != 0)
                {
                    // We can just reconnect this transport (if applicable)
                    if (!this.transportsToConnect.contains(transport))
                    {
                        this.transportsToConnect.add(transport);
                    }
                    
                    this.kickRecoveryThread();
                    return;
                }
                
                if (this.getState() == TrapState.OPENING)
                {
                    // The current transport failed. Just drop it in the failed transports pile.
                    // (Failed transports are cycled in at regular intervals)
                    this.failedTransports.add(transport);
                    
                    // Also notify recovery that we have lost a transport. This may schedule another to be reconnected.
                    this.kickRecoveryThread();
                    return;
                }
                else
                {
                    
                    long openTimeout = 1000;
                    
                    if (this.getState() == TrapState.OPEN)
                    {
                        // We have to report that we've lost all our transports.
                        this.setState(TrapState.SLEEPING);
                        
                        // Adjust reconnect timeout
                        this.canReconnectUntil = System.currentTimeMillis() + this.reconnectTimeout;
                        
                        // This is the first time, just reconnect immediately
                        openTimeout = 0;
                    }
                    
                    if (this.getState() != TrapState.SLEEPING)
                    {
                        // We have nothing to do here
                        return;
                    }
                    
                    // this is the point at which the code is asked to reconnect
                    // It should take this into account.
                    
                    // Checks that need to be made at this point:
                    // - when to stop trying to connect
                    // - how long between reconnect intervals
                    // -- IF we should reconnect (e.g. if this was a wakeup call)
                    
                    // For now, we'll be doing recovery any time the timeout hasn't expired
                    // fixed every second. This MUST be changed for wakeup support
                    if (System.currentTimeMillis() < this.canReconnectUntil)
                        ThreadPool.executeAfter(new Runnable() {
                            
                            public void run()
                            {
                                try
                                {
                                    ClientTrapEndpoint.this.doOpen();
                                }
                                catch (TrapException e)
                                {
                                    ClientTrapEndpoint.this.logger.error("Error while reconnecting after all transports failed", e);
                                    return;
                                }
                            }
                        }, openTimeout);
                    else
                        this.setState(TrapState.CLOSED);
                }
            }
            else if (oldState == TrapTransportState.CONNECTING)
            {
                
                // With these new changes, we can always use the new cycle logic.
                // The recovery thread will shift our state if we no longer have any viable connection
                this.cycleTransport(transport, "connectivity failure");
            }
            else
            {
                // disconnecting, so do nothing
                
                if ((this.getState() == TrapState.OPEN) || (this.getState() == TrapState.SLEEPING))
                {
                    if (this.activeTransports.size() == 0)
                        this.cycleTransport(transport, "This transport disconnected (orderly??) while we lost all other transports.");
                }
            }
            
            // There is a possible path through which we can fall through the cracks and not go over to state SLEEPING. We should do that now.
            if (this.getState() == TrapState.OPEN && this.activeTransports.size() == 0)
            {
                // We have to report that we've lost all our transports.
                this.setState(TrapState.SLEEPING);
                
                // Adjust reconnect timeout
                this.canReconnectUntil = System.currentTimeMillis() + this.reconnectTimeout;
            }
            
        }
        
        if (newState == TrapTransportState.CONNECTED)
        {
            if (oldState == TrapTransportState.CONNECTING)
            {
                // send Open(), await reply.
                this.sendOpen(transport);
            }
            else
            {
                this.logger.error("Reached TrapTransportState.CONNECTED from a non-CONNECTING state. We don't believe in this.");
            }
        }
    }
    
    public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
    {
        super.ttMessageReceived(message, transport, context);
    }
    
    private void sendOpen(TrapTransport transport)
    {
        TrapMessage m = this.createMessage().setOp(Operation.OPEN);
        TrapConfigurationImpl body = new TrapConfigurationImpl();
        if (this.autoConfigure)
        {
            try
            {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(StringUtil.toUtfBytes(this.getConfiguration()));
                body.setOption(TrapConfigurationImpl.CONFIG_HASH_PROPERTY, HexConverter.toString(digest.digest()));
            }
            catch (NoSuchAlgorithmException e)
            {
                this.logger.warn("Could not compute client configuration hash", e);
            }
        }
        
        if (this.connectionToken == null)
        {
            synchronized (this)
            {
                if (this.connectionToken == null)
                    this.connectionToken = UUID.randomUUID();
            }
        }
        
        body.setOption(TrapConstants.ENDPOINT_ID_CLIENT, this.trapID);
        body.setOption(TrapConstants.CONNECTION_TOKEN, this.connectionToken);
        body.setOption(TrapEndpoint.OPTION_MAX_CHUNK_SIZE, "" + this.getMaxChunkSize());
        body.setOption(TrapEndpoint.OPTION_ENABLE_COMPRESSION, Boolean.toString(this.compressionEnabled));
        body.setOption(TrapEndpoint.OPTION_AUTO_HOSTNAME, this.config.getOption(TrapEndpoint.OPTION_AUTO_HOSTNAME));
        m.setData(StringUtil.toUtfBytes(body.toString()));
        
        try
        {
            transport.send(m, false);
        }
        catch (TrapTransportException e)
        {
            this.cycleTransport(transport, "open message send failure");
        }
    }
    
    private void cycleTransport(TrapTransport transport, String reason)
    {
        this.logger.debug("Cycling transports due to {} {}...", transport.getTransportName(), reason);
        
        this.activeTransports.remove(transport);
        this.failedTransports.add(transport);
        
        transport.setTransportDelegate(this.nullDelegate, null);
        transport.disconnect();
        
        // Recover only if we have active transports. Otherwise do open...
        if (this.transportsToConnect.size() == 0)
        {
            this.logger.trace("No more transports to connect...");
            
            if (this.activeTransports.size() != 0)
            {
                this.logger.trace("At least one active transport remaining... no cycling necessary. Transports list [{}]", this.activeTransports.toString());
                return; // No problem.
            }
            
            if (this.getState() == TrapState.OPENING)
            {
                this.logger.error("Could not open a connection on any transport...");
                this.setState(TrapState.ERROR);
                return;
            }
            
            this.logger.trace("Scheduling new open...");
            
            ThreadPool.executeAfter(new Runnable() {
                
                public void run()
                {
                    try
                    {
                        ClientTrapEndpoint.this.doOpen();
                    }
                    catch (TrapException e)
                    {
                    	logger.error("Failed to reopen Trap Endpoint due to {}", e, e);
                    }
                }
            }, 1000);
        }
        else
            this.kickRecoveryThread();
        
    }
    
    public void attemptTransportRecovery()
    {
        synchronized (this.transportsToConnect)
        {
            this.transportsToConnect.addAll(this.transports);
            this.transportsToConnect.removeAll(this.activeTransports);
        }
        
        this.kickRecoveryThread();
    }
    
    protected void kickRecoveryThread()
    {
        this.logger.trace("Recovery thread....");
        synchronized (this.recoveryLock)
        {
            if (this.recovering)
            {
                this.logger.trace("... already running");
                return;
            }
            this.recovering = true;
        }
        ThreadPool.executeCached(new Runnable() {
            public void run()
            {
                try
                {
                    for (;;)
                    {
                        
                        try
                        {
                            for (;;)
                            {
                                if ((ClientTrapEndpoint.this.getState() == TrapState.CLOSING) || (ClientTrapEndpoint.this.getState() == TrapState.CLOSED) || (ClientTrapEndpoint.this.getState() == TrapState.CLOSED))
                                    return;
                                
                                TrapTransport first = null;
                                
                                synchronized (ClientTrapEndpoint.this.transportsToConnect)
                                {
                                    first = ClientTrapEndpoint.this.transportsToConnect.iterator().next();
                                }
                                try
                                {
                                    // Removing the transport in this try/finally means we'll immediately add it to failed transports if there is any exception
                                    // This ensures we never lose a transport.
                                    synchronized (ClientTrapEndpoint.this.transportsToConnect)
                                    {
                                        ClientTrapEndpoint.this.transportsToConnect.remove(first);
                                    }
                                    
                                    // Drop any transports that have made their way in here, but cannot be used
                                    if (!first.canConnect())
                                        continue;
                                    
                                    if (!first.isEnabled())
                                        continue;
                                    
                                    first.init();
                                    first.setTransportDelegate(ClientTrapEndpoint.this, null);
                                    first.setConfiguration(ClientTrapEndpoint.this.config);
                                    first.setFormat(ClientTrapEndpoint.this.getTrapFormat());
                                    ClientTrapEndpoint.this.activeTransports.add(first);
                                    
                                    first.connect();
                                }
                                catch (Exception e)
                                {
                                    
                                    // Exception for one transport must not affect the others.
                                    ClientTrapEndpoint.this.logger.debug("Transport {} failed to reconnect due to {}", (first != null ? first.getTransportName() : "null"), e);
                                    if (e instanceof NullPointerException)
                                    {
                                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                        e.printStackTrace(new PrintStream(bos, true, "UTF-8"));
                                        ClientTrapEndpoint.this.logger.debug(bos.toString("UTF-8"));
                                    }
                                    
                                    if (first != null)
                                    {
                                        // Then clean up after the transport. This transport MUST NOT succeed
                                        first.forceError();
                                        first.setTransportDelegate(ClientTrapEndpoint.this.nullDelegate, null);
                                        first.disconnect();
                                        
                                        // Finally, schedule it for later recovery.
                                        ClientTrapEndpoint.this.failedTransports.add(first);
                                        ClientTrapEndpoint.this.activeTransports.remove(first);
                                    }
                                }
                            }
                        }
                        catch (NoSuchElementException e)
                        {
                            // This is when we've looped through all transports, so don't print any exception.
                        }
                        finally
                        {
                            synchronized (ClientTrapEndpoint.this.recoveryLock)
                            {
                                if (ClientTrapEndpoint.this.transportsToConnect.size() == 0)
                                {
                                    ClientTrapEndpoint.this.recovering = false;
                                    
                                    // Time to handle error cases.
                                    // Basically, if the recovery thread is here, and we have zero active transports, we have a problem.
                                    
                                    if (ClientTrapEndpoint.this.activeTransports.size() == 0)
                                    {
                                        if (ClientTrapEndpoint.this.getState() == TrapState.OPENING)
                                        {
                                            // If we're opening, and we run out of active transports, we have an issue.
                                            ClientTrapEndpoint.this.logger.error("Trap failed to connect; no transport connected successfully");
                                            ClientTrapEndpoint.this.setState(TrapState.ERROR);
                                        }
                                        else
                                        {
                                            // We still don't have transports. lolwut?
                                            ClientTrapEndpoint.this.logger.debug("No transports left active after recovery thread has run...");
                                        }
                                    }
                                    
                                    return;
                                }
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                	logger.warn("Unhandled exception in connection handler", t);
                }
            }
        });
    }
    
    protected void reconnect(long timeout) throws TrapException
    {
        // On the client, we'll use the transports list in order to reconnect, so we have to just d/c and clear available transports.
        synchronized (this.transports)
        {
            for (int i = 0; i < this.transports.size(); i++)
            {
                TrapTransport t = this.transports.get(i);
                
                // Make the disconnect silent to not affect the parent's state.
                t.setTransportDelegate(this.nullDelegate, null);
                t.disconnect();
            }
        }
        
        // Restart connection attempts
        this.doOpen();
        
        long endTime = System.currentTimeMillis() + (timeout * this.transports.size());
        
        try
        {
            while ((this.getState() == TrapState.SLEEPING))
            {
                synchronized (this)
                {
                    long waitTime = endTime - System.currentTimeMillis();
                    if (waitTime <= 0)
                        break;
                    this.wait(waitTime);
                }
            }
        }
        catch (InterruptedException e)
        {
            this.logger.warn(e.getMessage(), e);
        }
        
        // Error cleanup.. If, at this point, we STILL do not have a connection, we must DIE.
        if (this.getState() != TrapState.OPEN)
            this.setState(TrapState.CLOSED);
    }
    
    public long getTransportRecoveryTimeout()
    {
        return this.transportRecoveryTimeout;
    }
    
    public void setTransportRecoveryTimeout(long transportRecoveryTimeout)
    {
        this.transportRecoveryTimeout = transportRecoveryTimeout;
    }
    
    protected void setState(TrapState newState)
    {
        super.setState(newState);
    }
    
    protected void onOpened(TrapMessage message, TrapTransport transport)
    {
        
        super.onOpened(message, transport);
        
        if (!TrapConstants.ENDPOINT_ID_CLIENT.equals(this.trapID))
        {
            if ((message.getData() != null) && (message.getData().length > 0))
            {
                // Message data should contain new configuration
                this.logger.debug("Received new configuration from server...");
                
                String str = StringUtil.toUtfString(message.getData());
                this.logger.trace("Configuration was [{}]", str);
                
                // Previously we overwrote the client's config with the server supplied one.
                // This fix should only replace/extend.
                this.config.initFromString(str);
                this.configure(this.config.toString());
                
                // Connect non-connected transports
                this.makeRecoveryAttempt();
            }
        }
        
    };
    
    public Collection<TrapTransport> getActiveTransports()
    {
        return this.activeTransports;
    }
    
    protected void makeRecoveryAttempt()
    {
        
        this.failedTransports.clear();
        
        for (int i = 0; i < this.transports.size(); i++)
        {
            TrapTransport t = this.transports.get(i);
            
            // Check if t is active
            boolean active = false;
            for (int j = 0; j < this.activeTransports.size(); j++)
            {
                if (this.activeTransports.get(j).getTransportName().equals(t.getTransportName()))
                {
                    active = true;
                    break;
                }
            }
            
            if (!active && !this.transportsToConnect.contains(t))
            {
                this.transportsToConnect.add(t);
            }
        }
        
        // Now make them connect
        this.kickRecoveryThread();
    }
    
    protected TrapTransportDelegate nullDelegate = new TrapTransportDelegate() {
                                                     
                                                     public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
                                                     {
                                                     }
                                                     
                                                     public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
                                                     {
                                                     }
                                                     
                                                     public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
                                                     {
                                                     }
                                                     
                                                     public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
                                                     {
                                                     }
                                                     
                                                     public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
                                                     {
                                                     }
                                                 };
    
}

class RecoveryHeartbeat implements Runnable
{
    
    boolean                                         cancelled = false;
    private final WeakReference<ClientTrapEndpoint> self;
    
    public void cancel()
    {
        this.cancelled = true;
    }
    
    public RecoveryHeartbeat(ClientTrapEndpoint self)
    {
        this.self = new WeakReference<ClientTrapEndpoint>(self);
    }
    
    public void run()
    {
        if (this.cancelled)
            return;
        
        ClientTrapEndpoint ep = this.self.get();
        
        if (ep == null)
            return;
        
        // Also drop out if the state forbids it. Prevents us looping for errors
        if ((ep.getState() == TrapState.CLOSED) || (ep.getState() == TrapState.CLOSING) || (ep.getState() == TrapState.ERROR))
            return;
        
        ep.makeRecoveryAttempt();
        
        // Schedule us to repeat
        ThreadPool.executeAfter(this, ep.transportRecoveryTimeout);
    }
    
}
