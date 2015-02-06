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

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.research.trap.TrapChannel;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapKeepalivePolicy;
import com.ericsson.research.trap.TrapObject;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnFailedSending;
import com.ericsson.research.trap.delegates.OnObject;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.delegates.OnSleep;
import com.ericsson.research.trap.delegates.OnStateChange;
import com.ericsson.research.trap.delegates.OnWakeup;
import com.ericsson.research.trap.delegates.TrapDelegate;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;
import com.ericsson.research.trap.impl.queues.ArrayBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.ChannelMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedMessageQueue;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapHostingTransport;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.Callback.SingleArgumentCallback;
import com.ericsson.research.trap.utils.Future;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public abstract class TrapEndpointImpl implements TrapEndpoint, TrapTransportDelegate, Comparable<TrapEndpointImpl>
{
    
    protected HashMap<String, TrapTransport> transportsMap         = new HashMap<String, TrapTransport>();
    protected LinkedList<TrapTransport>      transports            = new LinkedList<TrapTransport>();
    protected TrapConfiguration              config                = new TrapConfigurationImpl();
    
    // Linked list so we can sort it. We can look into a faster access method... someday.
    // Must be synchronized...
    protected LinkedList<TrapTransport>      availableTransports   = new LinkedList<TrapTransport>();
    private TrapState                        state                 = TrapState.CLOSED;
    
    // As we have few enough channels, an array will suffice.
    private final Object                     channelsLock          = new Object();
    protected TrapChannelImpl[]              channels              = new TrapChannelImpl[2];
    protected MessageQueue                   templateMessageQueue  = new LinkedBlockingMessageQueue();
    protected final ChannelMessageQueue      messageQueue          = new ChannelMessageQueue();
    protected boolean                        messageQueueRebuild   = false;
    
    protected int                            maxActiveTransports   = 1;
    
    protected Object                         sendingLock           = new Object();
    private boolean                          sending               = false;
    
    Object                                   delegateContext;
    
    private static final NullDelegate        nullDelegate          = new NullDelegate();
    
    OnAccept                                 acceptDelegate        = nullDelegate;
    OnClose                                  closeDelegate         = nullDelegate;
    OnData                                   dataDelegate          = nullDelegate;
    OnError                                  errorDelegate         = nullDelegate;
    OnFailedSending                          failedSendingDelegate = nullDelegate;
    OnObject                                 objectDelegate        = nullDelegate;
    OnOpen                                   openDelegate          = nullDelegate;
    OnSleep                                  sleepDelegate         = nullDelegate;
    OnStateChange                            statechangeDelegate   = nullDelegate;
    OnWakeup                                 wakeupDelegate        = nullDelegate;
    
    protected String                         trapID                = "UNDEFINED";
    protected Format                         trapFormat            = TrapConstants.MESSAGE_FORMAT_DEFAULT;
    
    protected TrapAuthentication             authentication        = new NullAuthentication();
    protected Logger                         logger;
    
    // Timeouts & Keepalives.
    /**
     * Last known activity of the connection. Activity is defined as any form of message FROM the client. In general,
     * the TrapEndpoint will not concern itself with ensuring this value is continually updated, as that is mostly
     * unnecessary overhead. It will update it during the following conditions:
     * <p>
     * <ul>
     * <li>A transport disconnects. Even in this case, the lastActivity field will only represent some most recent
     * communication with the remote side, unless all transports have disconnected.
     * <li>The application specifically queries. In this case, the TrapEndpoint will specifically ensure that
     * lastActivity has the most recent value.
     * </ul>>
     */
    protected long                           lastAlive             = 0;
    
    /**
     * The last known timestamp where we can reliably wake up the underlying transports. If we have a wakeup mechanism,
     * this will be a non-negative value, and represents when we can unilaterally tell the application the connection is
     * permanently dead (unless we can extend the wakeup mechanism).
     */
    protected long                           canWakeupUntil        = 0;
    
    /**
     * The last permitted timestamp for the client to re-establish connectivity. This must be equal to or greater than
     * canWakeupUntil, in order to maintain the same promise to the application.
     */
    protected long                           canReconnectUntil     = 0;
    
    /**
     * The number of milliseconds that the endpoint can wait for a reconnect to occur. This must correspond with the
     * server side, how long that one is willing to wait for a new incoming connection on an endpoint
     */
    protected long                           reconnectTimeout      = 180000;
    
    /**
     * The number of milliseconds that the endpoint should wait for a response (and/or attempt to reconnect/resend) to
     * do an orderly close. After this time, the transport will simply deallocate all of its resources and vanish.
     */
    protected long                           keepaliveExpiry       = 5000;
    
    /**
     * The active keepalive policy or timeout
     */
    protected int                            keepaliveInterval     = TrapKeepalivePolicy.DISABLED;
    
    protected Future                         keepaliveTask         = null;
    
    /**
     * Signifies that this TrapEndpoint is running in async mode.
     * 
     * @see TrapEndpoint#setAsync(boolean)
     */
    protected boolean                        async                 = true;
    protected boolean                        abortSending          = false;
    protected boolean                        compressionEnabled    = TrapConstants.COMPRESSION_ENABLED_DEFAULT;
    
    // Note that async mode does NOT resend messages unless asyncInorder = true
    
    public TrapEndpointImpl() throws TrapException
    {
        super();
        this.logger = LoggerFactory.getLogger(this.getClass());
        
        // Automatically try to use the most efficient queue...
        try
        {
            this.setQueueType(BLOCKING_MESSAGE_QUEUE);
        }
        catch (Throwable t)
        {
        }
        
        TrapChannelImpl tc = new TrapChannelImpl(this, 0);
        tc.setPriority(Integer.MAX_VALUE);
        this.channels[0] = tc;
        
        this.messageQueue.rebuild(this.channels);
    }
    
    public void enableTransport(String transportName) throws TrapException
    {
        if (this.isTransportEnabled(transportName))
            return;
        
        this.getTransport(transportName).enable();
    }
    
    public void disableTransport(String transportName)
    {
        try
        {
            if (!this.isTransportEnabled(transportName))
                return;
            
            this.getTransport(transportName).disable();
        }
        catch (TrapException e)
        {
        }
    }
    
    public void disableAllTransports()
    {
        for (int i = 0; i < this.transports.size(); i++)
            this.transports.get(i).disable();
    }
    
    public boolean isTransportEnabled(String transportName)
    {
        try
        {
            return this.getTransport(transportName).isEnabled();
        }
        catch (TrapException e)
        {
            return false;
        }
    }
    
    public String getConfiguration()
    {
        return this.config.toString();
    }
    
    protected TrapConfiguration parseConfiguration(String configuration)
    {
        return new TrapConfigurationImpl(configuration);
    }
    
    public void configure(String configuration)
    {
        this.config = this.parseConfiguration(configuration);
        
        String loggerString = this.config.getOption(TrapEndpoint.OPTION_LOGGERPREFIX);
        if (loggerString != null)
            this.logger = LoggerFactory.getLogger(loggerString + StringUtil.getLoggerComponent(this));
        
        Iterator<Entry<String, TrapTransport>> it = this.transportsMap.entrySet().iterator();
        
        while (it.hasNext())
        {
            Entry<String, TrapTransport> entry = it.next();
            TrapTransport t = entry.getValue();
            t.setConfiguration(this.config);
        }
        
        int option = this.config.getIntOption("trap.keepalive.interval", this.keepaliveInterval);
        this.setKeepaliveInterval(option);
        
        option = this.config.getIntOption("trap.keepalive.expiry", (int) this.keepaliveExpiry);
        this.setKeepaliveExpiry(option);
        
        this.compressionEnabled = this.config.getBooleanOption(TrapEndpoint.OPTION_ENABLE_COMPRESSION, this.compressionEnabled);
        
    }
    
    public void configureTransport(String transportName, String configurationKey, String configurationValue) throws TrapException
    {
        this.getTransport(transportName).configure(configurationKey, configurationValue);
    }
    
    public Collection<TrapTransport> getTransports()
    {
        return this.transportsMap.values();
    }
    
    public TrapTransport getTransport(String transportName) throws TrapException
    {
        
        TrapTransport t = this.transportsMap.get(transportName);
        
        if (t == null)
            throw new TrapException("Unknown Transport");
        
        return t;
    }
    
    public boolean addTransport(TrapTransport t)
    {
        
        if (!t.canConnect() && !t.canListen() && t.getState() == TrapTransportState.DISCONNECTED)
        {
            this.logger.debug("Attempting to add transport class [{}] for handler [{}] that can neither connect nor listen. Skipping...", t.getClass().getName(), t.getTransportName());
            return false;
        }
        
        synchronized (this.transports)
        {
            TrapTransport old = this.transportsMap.get(t.getTransportName());
            
            if (old != null)
            {
                
                int oldPrio = old.getTransportPriority();
                int newPrio = t.getTransportPriority();
                
                // Strict lesser equality. This allows us to be replaced by, well, the same transport.
                if (oldPrio < newPrio)
                {
                    this.logger.debug("Attempting to add new handler for [{}] when the old handler had a higher priority. New class was [{}]/{}, old class was [{}]{}. Skipping...", new Object[] { t.getTransportName(), t.getClass().getName(), t.getTransportPriority(), old.getClass().getName(), old.getTransportPriority() });
                    return false;
                }
                
                this.transports.remove(old);
            }
        }
        
        t.setFormat(this.getTrapFormat());
        
        this.transportsMap.put(t.getTransportName(), t);
        synchronized (this.transports)
        {
            this.transports.add(t);
        }
        
        return true;
    }
    
    public void removeTransport(TrapTransport t)
    {
        this.transportsMap.remove(t.getTransportName());
        synchronized (this.transports)
        {
            this.transports.remove(t);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.ericsson.research.trap.TrapEndpoint#close()
     *
     * The close implementation should send an END message to the other side.
     * The other side will immediately send an END back and close all its transports.
     * Consequently, this side will not be aware whether or not it will receive an END
     * back (the transports may fail).
     */
    public void close()
    {
        
        if (this.getState() != TrapState.OPEN)
        {
            // We can't close a non-open connection.
            
            if (this.getState() == TrapState.SLEEPING)
            {
                // TODO: We should WAKE UP then DISCONNECT.
                this.setState(TrapState.CLOSING);
                this.onEnd(null, null);
            }
            else
            {
                if ((this.getState() == TrapState.CLOSING) || (this.getState() == TrapState.CLOSED))
                {
                    // Harmless call.
                    return;
                }
                
                if (this.getState() == TrapState.ERROR)
                {
                    // Technically harmless call, but we will log it to point out potential laziness in the coding of the error handling of our parent.
                    this.logger.debug("Called close() on an endpoint in state ERROR. This might be caused by recovery code shared between regular and normal states");
                    return;
                }
                
                if (this.getState() == TrapState.OPENING)
                {
                    // TODO: This one is troublesome. close() has been called on a connection that is opening().
                    // I think we can handle it normally (i.e. switch to closing and just end()) but it might be worth investigating
                    // We will log.
                    this.logger.debug("Called close() on an endpoint in state OPENING. This message is logged for debug purposes (if we don't fully close).");
                }
            }
        }
        this.setState(TrapState.CLOSING);
        
        // We'll send END to the other side
        // After that has happened, we'll close (in onend)
        
        try
        {
            this.send(this.createMessage().setOp(Operation.END));
        }
        catch (TrapException e)
        {
            this.logger.error("Setting TrapState to ERROR due to an error while disconnecting that may have left the implementation in an inconsistent state");
            this.setState(TrapState.ERROR);
            // TODO: Cleanup/recovery?
        }
        
    }
    
    public void send(byte[] data) throws TrapException
    {
        this.send(data, 1, false);
    }
    
    public void send(byte[] data, int channel, boolean useCompression) throws TrapException
    {
        if (data == null)
            throw new NullPointerException("Data cannot be null. It may be a byte array with length zero, but null is forbidden.");
        this.send(this.createMessage().setOp(Operation.MESSAGE).setData(data).setChannel(channel).setCompressed(useCompression && this.compressionEnabled));
    }
    
    public void send(TrapObject object) throws TrapException
    {
        this.send(object, 1, false);
    }
    
    public void send(TrapObject object, int channel, boolean useCompression) throws TrapException
    {
        TrapMessage m = new TrapObjectMessage(object);
        m.setOp(Operation.MESSAGE);
        m.setChannel(channel);
        m.setCompressed(useCompression && this.compressionEnabled);
        this.send(m);
        
    }
    
    public void send(TrapMessage message) throws TrapException
    {
        
        if (message == null)
            throw new NullPointerException("Cannot send null message.");
        
        if ((this.getState() != TrapState.OPEN) && (message.getOp() != Operation.END) && this.getState() != TrapState.SLEEPING) // EXCEPT if we are (re-)sending the END message to terminate
            throw new TrapException("Tried to send to non-open Trap session");
        
        TrapChannelImpl channel = (this.getChannel(message.getChannel()));
        channel.assignMessageID(message);
        
        try
        {
            TrapTransport first = TrapEndpointImpl.this.availableTransports.get(0);
            if (first != null && first.isObjectTransport())
            {
                try
                {
                    first.send(message, false);
                    return;
                }
                catch (Throwable t)
                {
                    
                }
            }
        }
        catch (Throwable t)
        {
        }
        
        channel.send(message);
        
        /*
        // There's a specific state (sleeping) that allows us to send a message by waking up the session
        if (this.getState() == TrapState.SLEEPING)
        {
            // TODO: Wakeup
            // Wakeup comes defer, for defer may block. Then we must wait until defer unblocks.
            synchronized (this)
            {
                // Assign message id (if not already set)
                if (message.getMessageId() == 0)
                {
                    int messageId = TrapEndpointImpl.this.messageId++;
                    
                    if (messageId > TrapEndpointImpl.this.maxMessageId)
                        TrapEndpointImpl.this.messageId = messageId = 1;
                    
                    message.setMessageId(messageId);
                }
            }
            
            // Defer message.
            this.deferMessage(message);
            return;
        }
        
        // All other states do not allow the sending of messages.
        
        synchronized (this)
        {
            // Assign message id (if not already set)
            if (message.getMessageId() == 0)
            {
                int messageId = TrapEndpointImpl.this.messageId++;
                
                if (messageId > TrapEndpointImpl.this.maxMessageId)
                    TrapEndpointImpl.this.messageId = messageId = 1;
                
                message.setMessageId(messageId);
            }
        }
        
        TrapTransport first = null;
        
        try
        {
            first = (TrapTransport) TrapEndpointImpl.this.availableTransports.get(0);
        }
        catch (Throwable t)
        {
            first = null;
        }
        
        if (first.isObjectTransport())
        {
            try
            {
                first.send(message, false);
                return;
            }
            catch (Throwable t)
            {
                
            }
        }
        this.deferMessage(message);
        
        */
        
        this.kickSendingThread();
    }
    String stName = null;
    // @formatter:off
    Runnable sendingThread = new Runnable() {
        public void run()
        {
            stName = Thread.currentThread().getName();
            try
            {
                for (;;)
                {
                    TrapTransport first = null;
                    
                    if (TrapEndpointImpl.this.messageQueueRebuild)
                    {
                        // We don't need to synchronize this. At worst, we'll cause one more rebuild than strictly necessary
                        // but rebuilds should be rare enough for it to not matter.
                        TrapEndpointImpl.this.messageQueueRebuild = false;
                        TrapEndpointImpl.this.messageQueue.rebuild(TrapEndpointImpl.this.channels);
                    }

                    try
                    {
                    // Double while intentional. I let the thread linger for a moment to reduce the number of calls to the thread pool.
                    while ((TrapEndpointImpl.this.messageQueue.peek() != null) && !TrapEndpointImpl.this.abortSending )
                    {
                        try
                        {
                            first = TrapEndpointImpl.this.availableTransports.get(0);
                        }
                        catch (Throwable t)
                        {
                            first = null;
                        }
                        if (first != null)
                        {

                            if (TrapEndpointImpl.this.logger.isTraceEnabled())
                                TrapEndpointImpl.this.logger.trace("Now selecting transport [{}] for sending", first.getTransportName());

                            while (first.isAvailable())
                            {
                                try
                                {
                                    TrapMessage m = TrapEndpointImpl.this.messageQueue.peek();
                                    if (m == null)
                                        break;

                                    first.send(m, true);
                                    TrapEndpointImpl.this.messageQueue.pop();
                                }
                                catch (TrapTransportException e)
                                {
                                    TrapEndpointImpl.this.logger.debug(e.getMessage(), e);
                                }
                                catch (Exception e)
                                {
                                    TrapEndpointImpl.this.logger.debug(e.getMessage(), e);

                                    // What should happen if we get an exception here? We don't want this loop to continue, that's for sure.
                                    // The first transport is clearly inadequate for the task.
                                    if (first.getState() == TrapTransportState.AVAILABLE)
                                    {
                                        
                                        // Now, the problem here is that the regular API only allows us to do a graceful disconnect.
                                        // If we do that, though, recovery code won't be initialised.
                                        TrapEndpointImpl.this.logger.warn("Forcibly removing transport {} from available due to infinite loop protection. This code should not occur with a well-behaved transport.", first.getTransportName());
                                        TrapEndpointImpl.this.logger.warn("Caused by {}", e.getMessage(), e);

                                        first.forceError();
                                    }
                                    else
                                    {
                                        // Transport is no longer unavailable, loop should be broken.
                                    }
                                }
                            }
                            
                            first.flushTransport();

                            if (!first.isAvailable())
                            {
                                synchronized (TrapEndpointImpl.this)
                                {
                                    if (!first.isAvailable())
                                        TrapEndpointImpl.this.availableTransports.remove(first);
                                }
                            }
                        }
                        else
                            break; // Break out of the sending loop
                    }
                    }
                    finally
                    {
                        TrapEndpointImpl.this.messageQueue.rewind();
                    }

                    // This is an exit condition. To prevent this exit condition from creating unnecessary hits
                    // in the thread pool, we'll let the thread yield before exiting.
                    Thread.yield();
                    synchronized (TrapEndpointImpl.this.sendingLock)
                    {
                        try
                        {
                            first = TrapEndpointImpl.this.availableTransports.get(0);
                        }
                        catch (Throwable t)
                        {
                        }

                        // Also block on messageQueue; this should prevent unexpected exits.
                        synchronized (TrapEndpointImpl.this.messageQueue)
                        {

                            if ((TrapEndpointImpl.this.messageQueue.peek() == null) || (first == null) || TrapEndpointImpl.this.abortSending)
                            {
                                //System.out.println("######### Ending condition: " + (TrapEndpointImpl.this.messageQueueSize == 0) + (first == null));
                                TrapEndpointImpl.this.logger.trace("Send loop end: First: {}, Available: {}, EP: {}, MQ: {}", new Object[]{ first, TrapEndpointImpl.this.availableTransports, TrapEndpointImpl.this, TrapEndpointImpl.this.messageQueue });

                                synchronized (sendingLock)
                                {
                                    
                                    // Make one more loop to catch any MQ rebuilds needed.
                                    if (messageQueueRebuild)
                                        continue;
                                    
                                    TrapEndpointImpl.this.setSending(false);
                                    TrapEndpointImpl.this.messageQueue.rewind();
                                    
                                    // The final check. If we've gotten this far out, it's worth it.
                                    if ((TrapEndpointImpl.this.messageQueue.peek() != null) && first != null)
                                    {
                                        kickSendingThread();
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
                TrapEndpointImpl.this.logger.error("Exception while processing sending on {}: ", TrapEndpointImpl.this, t);
            }
            finally
            {
                if (TrapEndpointImpl.this.abortSending)
                {
                    synchronized(TrapEndpointImpl.this.sendingLock)
                    {
                        TrapEndpointImpl.this.abortSending = false;
                        TrapEndpointImpl.this.sendingLock.notifyAll();
                    }
                }
            }
        }
    };
    // @formatter:on
    
    protected void kickSendingThread()
    {
        synchronized (this.sendingLock)
        {
            if (this.isSending())
                return;
            this.setSending(true);
        }
        
        ThreadPool.executeCached(this.sendingThread);
    }
    
    public synchronized void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
    {
        this.logger.trace("Transport {} changed state from {} on endpoint {}", new Object[] { transport, oldState, this });
        if (newState == TrapTransportState.AVAILABLE)
        {
            this.addTransportToAvailable(transport);
            return;
        }
        
        // newState is NOT available. Remove the transport from availableTransports, if it was there
        synchronized (this)
        {
            this.availableTransports.remove(transport);
        }
        
        // Now we'll enter failure modes.
        if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
        {
            
            if ((this.getState() == TrapState.CLOSED) || (this.getState() == TrapState.CLOSING))
            {
                
                // Make sure we update our state properly when all transports have disconnected.
                if (this.getState() == TrapState.CLOSING)
                {
                    
                    // Verify if this was the last open transport.
                    for (int i = 0; i < this.transports.size(); i++)
                    {
                        TrapTransport t = this.transports.get(i);
                        if ((t.getState() != TrapTransportState.ERROR) && (t.getState() != TrapTransportState.DISCONNECTED))
                            return; // If there is at least one open transport, we won't change state.
                    }
                    
                    this.setState(TrapState.CLOSED);
                    
                }
            }
        }
    }
    
    public void setQueue(MessageQueue newQueue)
    {
        this.templateMessageQueue = newQueue;
    }
    
    public void setQueueType(String type)
    {
        if (TrapEndpoint.BLOCKING_BYTE_QUEUE.equals(type))
        {
            this.templateMessageQueue = new LinkedByteBlockingMessageQueue();
        }
        else if (TrapEndpoint.BLOCKING_MESSAGE_QUEUE.equals(type))
        {
            try
            {
                Class<?> c = Class.forName("com.ericsson.research.trap.impl.queues.CLQMessageQueue");
                Constructor<?> constructor = c.getConstructor(new Class[] { Integer.TYPE });
                this.templateMessageQueue = (MessageQueue) constructor.newInstance(1000);
            }
            catch (Throwable t)
            {
                this.templateMessageQueue = new ArrayBlockingMessageQueue();
            }
        }
        else if (TrapEndpoint.REGULAR_BYTE_QUEUE.equals(type))
        {
            this.templateMessageQueue = new LinkedByteMessageQueue();
        }
        else if (TrapEndpoint.REGULAR_MESSAGE_QUEUE.equals(type))
        {
            this.templateMessageQueue = new LinkedMessageQueue();
        }
        else
        {
            throw new IllegalArgumentException("No such queue type");
        }
        
    }
    
    public String getQueueType()
    {
        return this.templateMessageQueue.getQueueType();
    }
    
    public void setBlockingTimeout(long timeout)
    {
        if (this.messageQueue instanceof BlockingMessageQueue)
            ((BlockingMessageQueue) this.messageQueue).setBlockingTimeout(timeout);
    }
    
    public long getBlockingTimeout()
    {
        if (this.messageQueue instanceof BlockingMessageQueue)
            return ((BlockingMessageQueue) this.messageQueue).blockingTimeout();
        else
            return -1;
    }
    
    @Deprecated
    public void setDelegate(final com.ericsson.research.trap.TrapEndpointDelegate delegate, Object context)
    {
        
        TrapEndpointDelegate wrapper = new TrapEndpointDelegate() {
            
            public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
            {
                delegate.trapData(data, endpoint, context);
            }
            
            public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context)
            {
                delegate.trapFailedSending(datas, endpoint, context);
            }
            
            public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
            {
                delegate.trapStateChange(newState, oldState, endpoint, context);
            }
        };
        
        this.setDelegateContext(context);
        this.setDelegate(wrapper, true);
    }
    
    public void setDelegate(TrapDelegate delegate, boolean replaceAllExisting)
    {
        if (replaceAllExisting)
        {
            this.acceptDelegate = nullDelegate;
            this.closeDelegate = nullDelegate;
            this.dataDelegate = nullDelegate;
            this.errorDelegate = nullDelegate;
            this.failedSendingDelegate = nullDelegate;
            this.objectDelegate = nullDelegate;
            this.openDelegate = nullDelegate;
            this.sleepDelegate = nullDelegate;
            this.statechangeDelegate = nullDelegate;
            this.wakeupDelegate = nullDelegate;
        }
        
        // Delegate can implement ANY of these optional interfaces, so they can NOT be else'd away
        if (delegate instanceof OnAccept)
            this.acceptDelegate = (OnAccept) delegate;
        if (delegate instanceof OnClose)
            this.closeDelegate = (OnClose) delegate;
        if (delegate instanceof OnData)
            this.dataDelegate = (OnData) delegate;
        if (delegate instanceof OnError)
            this.errorDelegate = (OnError) delegate;
        if (delegate instanceof OnFailedSending)
            this.failedSendingDelegate = (OnFailedSending) delegate;
        if (delegate instanceof OnObject)
            this.objectDelegate = (OnObject) delegate;
        if (delegate instanceof OnOpen)
            this.openDelegate = (OnOpen) delegate;
        if (delegate instanceof OnSleep)
            this.sleepDelegate = (OnSleep) delegate;
        if (delegate instanceof OnStateChange)
            this.statechangeDelegate = (OnStateChange) delegate;
        if (delegate instanceof OnWakeup)
            this.wakeupDelegate = (OnWakeup) delegate;
    }
    
    public void setDelegateContext(Object delegateContext)
    {
        this.delegateContext = delegateContext;
    }
    
    public void setAuthentication(TrapAuthentication authentication) throws TrapException
    {
        this.authentication = authentication;
        for (int i = 0; i < this.transports.size(); i++)
            this.transports.get(i).setAuthentication(authentication);
    }
    
    public TrapAuthentication getAuthentication()
    {
        return this.authentication;
    }
    
    /*
     * Adds a transport to the current endpoint. The transport will supply an open message,
     * though it is usually discardeable. If the transport needs authentication for, e.g. open() calls,
     * it will be automatically enforced by the transport once an appropriate authentication is set.
     * Consequently, just setting the transport configuration is enough to have a secure connection.
     */
    public void addTransport(TrapTransport t, TrapMessage message)
    {
        
        try
        {
            t.setTransportDelegate(this, null);
            t.setConfiguration(this.config);
            t.setAuthentication(this.authentication);
            
            // Don't continue adding a transport if the main add function chose not to do so.
            if (!this.addTransport(t))
            {
                t.setTransportDelegate(null, null);
                return;
            }
            
            if (t.getState() == TrapTransportState.AVAILABLE)
            {
                
                // This, in general, means we get a second transport on an existing session. We should re-check the liveness of the existing transports, in case
                // this is a disconnect
                
                // We should temporarily clear the available transports.
                synchronized (this)
                {
                    
                    final LinkedList<TrapTransport> l = new LinkedList<TrapTransport>(this.availableTransports);
                    //this.availableTransports.clear();
                    ThreadPool.executeCached(new Runnable() {
                        
                        public void run()
                        {
                            for (int i = 0; i < l.size(); i++)
                            {
                                final TrapTransport t = l.get(i);
                                
                                if (t.getState() == TrapTransportState.DISCONNECTED)
                                    continue; // This has been cleaned up on its own; we shouldn't meddle.
                                    
                                t.isAlive(0, true, 15000).setCallback(new SingleArgumentCallback<Boolean>() {
                                    
                                    public void receiveSingleArgumentCallback(Boolean result)
                                    {
                                        if (!result.booleanValue())
                                            t.forceError(); // OOps. Dead, jim!
                                    }
                                });
                            }
                        }
                    });
                    
                    this.addTransportToAvailable(t);
                }
            }
            else
            {
                // The second case is trickier. We get a new unavailable transport (=sporadic availability). We can't make any assumptions
                // but it is nevertheless wise to check the available transports.
                synchronized (this)
                {
                    
                    final LinkedList<TrapTransport> l = new LinkedList<TrapTransport>(this.availableTransports);
                    ThreadPool.executeCached(new Runnable() {
                        
                        public void run()
                        {
                            for (int i = 0; i < l.size(); i++)
                            {
                                final TrapTransport t = l.get(i);
                                t.isAlive(0, true, 15000).setCallback(new SingleArgumentCallback<Boolean>() {
                                    
                                    public void receiveSingleArgumentCallback(Boolean result)
                                    {
                                        if (!result.booleanValue())
                                            t.forceError();
                                    }
                                });
                            }
                        }
                    });
                }
            }
            
            // Trigger incoming message (=OPEN) in order to reply properly.
            this.ttMessageReceived(message, t, null);
        }
        catch (TrapException e)
        {
            this.logger.warn(e.getMessage(), e);
        }
    }
    
    protected void addTransportToAvailable(TrapTransport t)
    {
        // We must perform a sorted insertion
        synchronized (this)
        {
            
            if (!t.isAvailable())
            {
                this.logger.debug("Tried to add non-available transport to available.");
                return;
            }
            
            boolean added = false;
            
            for (int i = 0; i < this.availableTransports.size(); i++)
            {
                TrapTransport c = this.availableTransports.get(i);
                
                // Priority goes from negative to positive (most to least preferred)
                // When two transports have the SAME priority, add the newest one last
                
                if (c.getTransportPriority() >= t.getTransportPriority())
                {
                    this.availableTransports.add(i, t);
                    added = true;
                    break;
                }
            }
            
            if (!added)
                this.availableTransports.addLast(t);
            
            ThreadPool.executeCached(new Runnable() {
                
                public void run()
                {
                    TrapTransport last = null;
                    
                    synchronized (TrapEndpointImpl.this)
                    {
                        if (TrapEndpointImpl.this.availableTransports.size() > TrapEndpointImpl.this.maxActiveTransports)
                        {
                            last = TrapEndpointImpl.this.availableTransports.removeLast();
                            
                            TrapEndpointImpl.this.logger.trace("Disconnecting [{}] as max active transports was exceeded", last);
                        }
                    }
                    
                    if (last != null)
                        last.disconnect();
                }
            });
            
            if (this.getState() == TrapState.SLEEPING)
                this.setState(TrapState.OPEN);
        }
        
        this.kickSendingThread();
        
    }
    
    public void sort()
    {
        synchronized (this)
        {
            Collections.sort(this.availableTransports, new Comparator<TrapTransport>() {
                
                public int compare(TrapTransport o1, TrapTransport o2)
                {
                    int o1Prio = o1.getTransportPriority();
                    int o2Prio = o2.getTransportPriority();
                    
                    if (o1Prio < o2Prio)
                        return -1;
                    if (o1Prio == o2Prio)
                        return 0;
                    else
                        return 1;
                }
            });
        }
    }
    
    public String getTrapID()
    {
        return this.trapID;
    }
    
    public void setTrapID(String trapID)
    {
        this.trapID = trapID;
    }
    
    public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
    {
        
        if (this.async && (message.getMessageId() != 0))
        {
            this.getChannel(message.getChannel()).receiveMessage(message, transport);
        }
        else
        {
            this.executeMessageReceived(message, transport);
        }
    }
    
    protected void executeMessageReceived(TrapMessage message, TrapTransport transport)
    {
        if (this.logger.isTraceEnabled())
            this.logger.trace("Dispatched: {}/{} on {} for {}", new Object[] { message.getOp(), message.getMessageId(), transport, this });
        //this.logger.trace("{} dispatched {} message with ID {} on transport {}", new Object[] { this, message.getOp(), new Integer(message.getMessageId()), transport });
        
        switch (message.getOp().getOp())
        {
            case Operation.Value.OPEN:
                this.onOpen(message, transport);
                break;
            
            case Operation.Value.OPENED:
                this.onOpened(message, transport);
                break;
            
            case Operation.Value.CLOSE:
                this.onClose(message, transport);
                break;
            
            case Operation.Value.END:
                this.onEnd(message, transport);
                break;
            
            case Operation.Value.CHALLENGE:
                this.onChallenge(message, transport);
                break;
            
            case Operation.Value.ERROR:
                this.onError(message, transport);
                break;
            
            // We have to handle both MESSAGE and FRAGMENTs as equal. This will be determined by streaming mode.
            case Operation.Value.MESSAGE:
            case Operation.Value.FRAGMENT_END:
            case Operation.Value.FRAGMENT_START:
                this.onMessage(message, transport);
                break;
            
            case Operation.Value.OK:
                this.onOK(message, transport);
                break;
            
            case Operation.Value.PING:
                this.onPing(message, transport);
                break;
            
            case Operation.Value.PONG:
                this.onPong(message, transport);
                break;
            
            case Operation.Value.TRANSPORT:
                this.onTransport(message, transport);
                break;
            
            default:
                return;
                
        }
        
    }
    
    protected void onTransport(TrapMessage message, TrapTransport transport)
    {
        // Transport specific messages. May require us to reconfigure a different transport.
        // This is our hook for future extensions.
    }
    
    /*
     * Ping/Pong events are generally a transport-specific concern.
     * The events will be received by the TrapEndpoint, but handled by the transports.
     */
    protected void onPong(TrapMessage message, TrapTransport transport)
    {
    }
    
    protected void onPing(TrapMessage message, TrapTransport transport)
    {
    }
    
    /*
     * An OK will acknowledge a successful operation. This should be a TODO...
     */
    protected void onOK(TrapMessage message, TrapTransport transport)
    {
    }
    
    protected void onMessage(TrapMessage message, TrapTransport transport)
    {
        if (message instanceof TrapObjectMessage)
        {
            this.objectDelegate.trapObject(((TrapObjectMessage) message).getObject(), message.getChannel(), this, this.delegateContext);
        }
        else
        {
            this.dataDelegate.trapData(message.getData(), message.getChannel(), this, this.delegateContext);
        }
    }
    
    /*
     * Errors should be handled. Onerror will most likely mean that the connection
     * has reached an unrecoverable state and must be discarded. The application MUST be
     * notified of this state.
     */
    protected void onError(TrapMessage message, TrapTransport transport)
    {
        this.setState(TrapState.ERROR);
    }
    
    protected void onChallenge(TrapMessage message, TrapTransport transport)
    {
    }
    
    protected void onEnd(TrapMessage message, TrapTransport transport)
    {
        
        if (this.getState() == TrapState.CLOSING)
        {
            
            if (this.keepaliveTask != null)
                this.keepaliveTask.cancel();
            Iterator<TrapTransport> it = this.transports.iterator();
            
            while (it.hasNext())
                it.next().disconnect();
            
            this.setState(TrapState.CLOSED);
            
            // TODO: Should this do some more cleanup here? Can we reopen this object? If we can't reopen, should we note it in the state somehow?
        }
        else
        {
            if (this.keepaliveTask != null)
                this.keepaliveTask.cancel();
            this.setState(TrapState.CLOSING);
            try
            {
                this.send(this.createMessage().setOp(Operation.END));
            }
            catch (TrapException e)
            {
            	logger.trace("Failed to send END message response. Remote side closed during ending handshake", e);
                
                Iterator<TrapTransport> it = this.transports.iterator();
                
                while (it.hasNext())
                    it.next().disconnect();
            }
        }
        
    }
    
    protected void onClose(TrapMessage message, TrapTransport transport)
    {
    }
    
    protected void onOpened(TrapMessage message, TrapTransport transport)
    {
        
        if (this.getState() == TrapState.CLOSED || this.getState() == TrapState.CLOSING || this.getState() == TrapState.ERROR)
        {
            return;
        }
        
        if (TrapConstants.ENDPOINT_ID_CLIENT.equals(this.trapID))
        {
            TrapConfigurationImpl cfg = new TrapConfigurationImpl(StringUtil.toUtfString(message.getData()));
            String trapId = cfg.getOption(TrapConstants.ENDPOINT_ID);
            this.setTrapID(trapId);
        }
        
        this.setState(TrapState.OPEN);
        
    }
    
    protected void setState(TrapState newState)
    {
        if (newState == this.state)
            return; // Department of redundancy department.
            
        TrapState oldState = this.getState();
        this.state = newState;
        
        if (this.logger.isTraceEnabled())
        {
            this.logger.trace("{} changing state from {} to {} with transports {}", new Object[] { this, oldState, newState, this.transports });
        }
        
        // Unlock anyone waiting on us
        synchronized (this)
        {
            this.notifyAll();
        }
        
        // Now sort into the appropriate delegates.
        if (newState == TrapState.OPEN && oldState == TrapState.OPENING)
            this.openDelegate.trapOpen(this, this.delegateContext);
        else if (newState == TrapState.CLOSED && oldState == TrapState.CLOSING)
            this.closeDelegate.trapClose(this, this.delegateContext);
        else if (newState == TrapState.SLEEPING && oldState == TrapState.OPEN)
            this.sleepDelegate.trapSleep(this, this.delegateContext);
        else if (newState == TrapState.OPEN && oldState == TrapState.SLEEPING)
            this.wakeupDelegate.trapWakeup(this, this.delegateContext);
        
        this.statechangeDelegate.trapStateChange(newState, oldState, this, this.delegateContext);
        
        // Perform cleanup
        if (newState == TrapState.ERROR)
        {
            
            Iterator<TrapTransport> it = this.transports.iterator();
            
            while (it.hasNext())
                it.next().disconnect();
            
            if (this.keepaliveTask != null)
                this.keepaliveTask.cancel();
            
            this.errorDelegate.trapError(this, this.delegateContext);
        }
        
        // Notify messages that have not sent
        if ((newState == TrapState.ERROR) || (newState == TrapState.CLOSED))
        {
            LinkedList<Object> faileds = new LinkedList<Object>();
            
            this.messageQueue.rewind();
            while (this.messageQueue.peek() != null)
            {
                TrapMessage msg = this.messageQueue.pop();
                
                if (msg == null)
                    break;
                
                if (msg instanceof TrapObjectMessage)
                {
                    faileds.add(((TrapObjectMessage) msg).getObject());
                }
                else
                {
                    faileds.add(msg.getData());
                }
                
            }
            
            if (faileds.size() > 0)
                this.failedSendingDelegate.trapFailedSending(faileds, this, this.delegateContext);
        }
    }
    
    protected void onOpen(TrapMessage message, final TrapTransport transport)
    {
        
        if (this.getState() == TrapState.CLOSED || this.getState() == TrapState.CLOSING || this.getState() == TrapState.ERROR)
        {
            this.logger.debug("Connection Error: Received OPEN message on {}. Returning with END", this);
            transport.sendTransportSpecific(this.createMessage().setOp(Operation.END));
            
            // Ensure the transport is disconnected.
            ThreadPool.executeAfter(new Runnable() {
                
                public void run()
                {
                    if (transport.getState() != TrapTransportState.DISCONNECTED && transport.getState() != TrapTransportState.ERROR)
                    {
                        TrapEndpointImpl.this.logger.debug("Disconnect Error: {} failed to disconnect, despite ending the session on {}", transport, TrapEndpointImpl.this);
                        transport.forceError();
                    }
                }
            }, 5000);
            return;
        }
        
        transport.sendTransportSpecific(this.createOnOpenedMessage(message));
        this.setState(TrapState.OPEN);
    }
    
    protected TrapMessage createOnOpenedMessage(TrapMessage message)
    {
        // Send new OPENED message
        return this.createMessage().setOp(Operation.OPENED);
    }
    
    public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
    {
        
        if (this.logger.isDebugEnabled())
            this.logger.debug("Failed sending {} messages on transport {}, Messages were {}", new Object[] { messages.size(), transport, messages });
        
        synchronized (this.sendingLock)
        {
            this.abortSending = true;
            
            try
            {
                
                while (this.isSending())
                    this.sendingLock.wait(1);
                
                Iterator<TrapMessage> it = messages.iterator();
                
                while (it.hasNext())
                {
                    TrapMessage m = it.next();
                    this.getChannel(m.getChannel()).addFailedMessage(m);
                }
                
                for (int i = 0; i < this.channels.length; i++)
                    if (this.channels[i] != null)
                        this.channels[i].rebuildMessageQueue();
            }
            catch (Exception e)
            {
				logger.error("Unhandled exception while trying to recover failed messages; {}", e, e);
                this.close();
                this.setState(TrapState.ERROR);
            }
            finally
            {
                if (this.abortSending)
                {
                    this.abortSending = false;
                    this.sendingLock.notifyAll();
                }
            }
        }
        
        // Restart the sending thread
        this.kickSendingThread();
        
    }
    
    public long lastAlive()
    {
        // Go through all transports and fetch lastAlive
        
        for (int i = 0; i < this.transports.size(); i++)
        {
            TrapTransport t = this.transports.get(i);
            long tLastAlive = t.lastAlive();
            
            if (this.lastAlive < tLastAlive)
                this.lastAlive = tLastAlive;
        }
        
        return this.lastAlive;
    }
    
    public Callback<Boolean> isAlive(long timeout)
    {
        return this.isAlive(timeout, true, true, timeout);
    }
    
    public Callback<Boolean> isAlive(long within, boolean check, final boolean reconnect, final long timeout)
    {
        
        final SingleCallback<Boolean> cb = new SingleCallback<Boolean>();
        
        // Ensure lastAlive is up to date.
        this.lastAlive();
        
        // Define within
        long mustBeAliveAfter = System.currentTimeMillis() - within;
        
        // We're within the allotted time window.
        if (this.lastAlive > mustBeAliveAfter)
            return cb.callback(true);
        
        // We're not allowed to perform the liveness check...
        if (!check)
            return cb.callback(false);
        
        final Runnable recovery = new Runnable() {
            
            public void run()
            {
                if (!reconnect)
                {
                    cb.callback(false);
                    return;
                }
                
                ThreadPool.executeCached(new Runnable() {
                    
                    public void run()
                    {
                        try
                        {
                            TrapEndpointImpl.this.setState(TrapState.SLEEPING);
                            TrapEndpointImpl.this.canReconnectUntil = System.currentTimeMillis() + TrapEndpointImpl.this.reconnectTimeout;
                            
                            TrapEndpointImpl.this.reconnect(timeout);
                            
                            // Reconnect will actually lock for us, so we don't need to lock.
                            cb.callback(TrapEndpointImpl.this.getState() == TrapState.OPEN);
                            return;
                        }
                        catch (Exception e)
                        {
                            TrapEndpointImpl.this.logger.error("Setting TrapEndpoint to state ERROR because reconnect failed. We don't know currently how to recover from this state, so the connection is dropped");
                            TrapEndpointImpl.this.setState(TrapState.ERROR);
                        }
                        
                        cb.callback(false);
                    }
                });
            }
        };
        
        // TODO: The liveness check should (theoretically) be multithreaded. It also MUST be synchronized so we don't accidentally drop a transport.
        synchronized (this)
        {
            if (this.availableTransports.size() <= 0)
                recovery.run();
            else
                for (int i = 0; i < this.availableTransports.size(); i++)
                {
                    this.availableTransports.get(i).isAlive(within, check, timeout).setCallback(new SingleArgumentCallback<Boolean>() {
                        
                        public void receiveSingleArgumentCallback(Boolean result)
                        {
                            if (result.booleanValue())
                            {
                                cb.callback(true);
                                return;
                            }
                            
                            try
                            {
                                // Check if someone else has done a callback
                                if (cb.get(0) != null)
                                    return;
                            }
                            catch (InterruptedException e1)
                            {
                            }
                            
                            recovery.run();
                            
                        }
                    });
                }
        }
        
        return cb;
    }
    
    protected abstract void reconnect(long timeout) throws TrapException;
    
    public TrapState getState()
    {
        return this.state;
    }
    
    protected TrapMessage createMessage()
    {
        return new TrapMessageImpl().setFormat(this.trapFormat);
    }
    
    public int compareTo(TrapEndpointImpl o)
    {
        return o.trapID.compareTo(this.trapID);
    }
    
    public boolean isAsync()
    {
        return this.async;
    }
    
    public void setAsync(boolean a)
    {
        this.async = a;
    }
    
    public int getMaxActiveTransports()
    {
        return this.maxActiveTransports;
    }
    
    public void setMaxActiveTransports(int newMax)
    {
        this.maxActiveTransports = newMax;
    }
    
    public int getKeepaliveInterval()
    {
        return this.keepaliveInterval;
    }
    
    public void setKeepaliveInterval(int newInterval)
    {
        this.keepaliveInterval = newInterval;
        
        // Forward apply on all transports
        for (int i = 0; i < this.transports.size(); i++)
            this.transports.get(i).setKeepaliveInterval(newInterval);
        
        long mTimer = TrapEndpointImpl.this.keepaliveInterval;
        
        if ((mTimer == TrapKeepalivePolicy.DEFAULT) || (mTimer == TrapKeepalivePolicy.DISABLED))
            return;
        
        if (this.keepaliveTask != null)
            this.keepaliveTask.cancel();
        
        TrapEndpointImpl.this.keepaliveTask = ThreadPool.executeAfter(new EndpointKeepaliveTask(this), mTimer * 1000);
    }
    
    public void setKeepaliveExpiry(long newExpiry)
    {
        this.keepaliveExpiry = newExpiry;
        for (int i = 0; i < this.transports.size(); i++)
            this.transports.get(i).setKeepaliveExpiry(newExpiry);
    }
    
    protected boolean isSending()
    {
        return this.sending;
    }
    
    protected void setSending(boolean sending)
    {
        this.sending = sending;
    }
    
    public void setOption(String optionName, String value)
    {
        this.config.setOption(optionName, value);
        this.configure(this.config.toString());
    }
    
    public Format getTrapFormat()
    {
        return this.trapFormat;
    }
    
    public void setTrapFormat(Format trapFormat)
    {
        this.trapFormat = trapFormat;
        
        Iterator<TrapTransport> it = this.transports.iterator();
        
        while (it.hasNext())
            it.next().setFormat(trapFormat);
    }
    
    public long getReconnectTimeout()
    {
        return this.reconnectTimeout;
    }
    
    public void setReconnectTimeout(long reconnectTimeout)
    {
        this.reconnectTimeout = reconnectTimeout;
    }
    
    protected void finalize() throws Throwable
    {
        
        if ((this.state != TrapState.ERROR) && (this.state != TrapState.CLOSED))
            this.logger.trace("Endpoint {} has been garbage collected.", new Object[] { this });
        
        super.finalize();
    }
    
    public String toString()
    {
        String name = this.getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        return name + "/" + this.getTrapID() + "/" + this.getState() + "/" + Integer.toHexString(this.hashCode());
    }
    
    public TrapChannelImpl getChannel(int channelID)
    {
        
        if (channelID > this.channels.length || channelID < TrapChannel.ID_MIN)
        {
            if (channelID > TrapChannel.ID_MAX || channelID < TrapChannel.ID_MIN)
                throw new IllegalArgumentException("Channel ID " + channelID + " is outside the supported range.");
        }
        
        if (channelID >= this.channels.length)
        {
            synchronized (this.channelsLock)
            {
                if (channelID >= this.channels.length)
                {
                    int newChannels = Math.min(TrapChannel.ID_MAX, channelID + 8) + 1;
                    this.channels = Arrays.copyOf(this.channels, newChannels);
                }
            }
        }
        
        TrapChannelImpl c = this.channels[channelID];
        
        if (c == null)
        {
            synchronized (this.channels)
            {
                c = this.channels[channelID];
                
                if (c == null)
                {
                    c = new TrapChannelImpl(this, channelID);
                    
                    int chunkSize = this.getMaxChunkSize();
                    chunkSize = Math.min(chunkSize, c.getChunkSize());
                    if (chunkSize <= 0)
                        chunkSize = Integer.MAX_VALUE;
                    c.setChunkSize(chunkSize);
                    
                    this.channels[channelID] = c;
                    this.messageQueueRebuild = true;
                }
            }
        }
        
        return c;
    }
    
    protected int getMaxChunkSize()
    {
        return this.config.getIntOption("trap." + OPTION_MAX_CHUNK_SIZE, TrapConstants.DEFAULT_CHUNK_SIZE);
    }
    
    public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
    {
        this.getChannel(message.getChannel()).messageSent(message);
    }
    
    public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
    {
        // Stubbed implementation will simply discard these messages.
        // We will not support peer-to-peer in the generic case for the time being.
        // This way, we will stop p2p messages from interfering with non-p2p.
        
    }
    
    public TrapHostingTransport getHostingTransport(String protocol)
    {
        for (TrapHostingTransport ht : this.getHostingTransports())
            if (ht.getProtocolName().equals(protocol))
                return ht;
        return null;
    }
    
    public Collection<TrapHostingTransport> getHostingTransports()
    {
        LinkedList<TrapHostingTransport> ts = new LinkedList<TrapHostingTransport>();
        for (TrapTransport t : this.transports)
            if (t instanceof TrapHostingTransport)
                ts.add((TrapHostingTransport) t);
        return ts;
    }
    
    public Map<String, Map<String, Object>> getTransportAuthenticationContexts()
    {
        
        Collection<TrapTransport> ts = this.getTransports();
        Map<String, Map<String, Object>> rv = new HashMap<String, Map<String, Object>>();
        
        for (TrapTransport t : ts)
        {
            rv.put(t.getTransportName(), t.getAuthenticationContext());
        }
        
        return rv;
    }
    
}

class EndpointKeepaliveTask implements Runnable
{
    
    private final WeakReference<TrapEndpoint> e;
    
    public EndpointKeepaliveTask(TrapEndpoint e)
    {
        this.e = new WeakReference<TrapEndpoint>(e);
    }
    
    public void run()
    {
        
        final TrapEndpointImpl ep = (TrapEndpointImpl) this.e.get();
        
        if (ep == null)
            return;
        
        // Conditions that should cause this task to exit.
        if ((ep.getState() == TrapState.CLOSING) || (ep.getState() == TrapState.CLOSED) || (ep.getState() == TrapState.ERROR))
            return;
        
        if ((ep.getKeepaliveInterval() == TrapKeepalivePolicy.DISABLED) || (ep.getKeepaliveInterval() == TrapKeepalivePolicy.DEFAULT))
            return;
        
        // Calculate the expected time we would need for keepalives to be working
        long expectedTime = System.currentTimeMillis() - (ep.keepaliveInterval * 1000) - ep.keepaliveExpiry;
        
        try
        {
            // Now verify all transports are within that time.
            Iterator<TrapTransport> it = ep.transports.iterator();
            
            while (it.hasNext())
            {
                final TrapTransport t = it.next();
                
                // Check that the transport is active
                if (!t.isConnected())
                {
                    // Inactive transports are excused from keepalives
                    continue;
                }
                
                if (t.lastAlive() < (expectedTime - 10 /* Allow slight shift in expected time to account for clock drift. */))
                {
                    // This transport is not conforming.
                    ep.logger.debug("Transport {} is not compliant with the keepalive timers. Last alive reported was {}, but expected {}", new Object[] { t.getTransportName(), t.lastAlive(), expectedTime });
                    
                    try
                    {
                        // Perform a manual check
                        t.isAlive(ep.keepaliveExpiry, true, ep.keepaliveExpiry).setCallback(new SingleArgumentCallback<Boolean>() {
                            
                            public void receiveSingleArgumentCallback(Boolean result)
                            {
                                if (result.booleanValue())
                                {
                                    ep.logger.info("Disconnecting transport {} because it had timed out while not performing its own checks", t.getTransportName());
                                    t.disconnect();
                                }
                                
                            }
                        });
                    }
                    catch (Exception e)
                    {
                        ep.logger.error("Exception while checking non-conforming transport", e);
                    }
                }
            }
            
        }
        catch (ConcurrentModificationException e)
        {
            // Recurse
            this.run();
        }
        
        // Now reschedule ourselves
        // Performing this jump will prevent a race condition from making us spiral out of control
        long mTimer = ep.keepaliveInterval;
        
        if ((mTimer == TrapKeepalivePolicy.DEFAULT) || (mTimer == TrapKeepalivePolicy.DISABLED))
            return;
        
        ep.keepaliveTask = ThreadPool.executeAfter(this, mTimer * 1000);
    }
    
}
