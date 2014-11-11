package com.ericsson.research.trap.spi.transports;

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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.auth.TrapAuthenticationException;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.impl.AutoconfigurationDisabledException;
import com.ericsson.research.trap.impl.NullAuthentication;
import com.ericsson.research.trap.impl.TrapConfigurationImpl;
import com.ericsson.research.trap.impl.TrapMessageImpl;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapKeepaliveDelegate;
import com.ericsson.research.trap.spi.TrapKeepalivePredictor;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.ByteConverter;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.Future;
import com.ericsson.research.trap.utils.IPUtil;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public abstract class AbstractTransport implements TrapTransport, TrapKeepaliveDelegate
{
    
    protected Map<String, String>        headersMap             = new HashMap<String, String>();
    protected boolean                    enabled                = TrapConstants.TRANSPORT_ENABLED_DEFAULT;
    protected TrapConfiguration          configuration          = new TrapConfigurationImpl();
    protected String                     prefix                 = "transport.undefined";
    protected TrapTransportState         state                  = TrapTransportState.DISCONNECTED;
    protected TrapAuthentication         authentication         = new NullAuthentication();
    
    protected TrapTransportDelegate      delegate               = null;
    protected Object                     delegateContext        = null;
    
    protected HashSet<String>            availableKeys          = new HashSet<String>();
    protected Collection<String>         contextKeys            = new HashSet<String>();
    protected HashMap<String, Object>    contextMap             = new HashMap<String, Object>();
    
    protected long                       lastAlive              = 0;
    Map<String, SingleCallback<Boolean>> livenessMap            = Collections.synchronizedMap(new HashMap<String, SingleCallback<Boolean>>());
    
    protected TrapKeepalivePredictor     keepalivePredictor;
    
    protected int                        transportPriority      = 0;
    
    protected Logger                     logger;
    
    // Linked list... Transports are in-order, so in most cases, non-dropped
    // messages will arrive on FCFS basis
    protected LinkedList<TrapMessage>    messagesInTransit      = new LinkedList<TrapMessage>();
    
    // Internal buffer used to queue messages that must be sent on this transport
    protected LinkedList<TrapMessage>    transportMessageBuffer = new LinkedList<TrapMessage>();
    
    protected Format                     format                 = TrapConstants.MESSAGE_FORMAT_DEFAULT;
    private Future                       connectionTimeoutTask  = null;
    private Future                       disconnectExpiry       = null;
    
    public AbstractTransport()
    {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.keepalivePredictor = new StaticKeepalivePredictor();
        this.prefix = "trap.transport." + this.getTransportName().toLowerCase();
        this.availableKeys.add(TrapContextKeys.Transport);
        this.availableKeys.add(TrapContextKeys.Protocol);
        this.fillAuthenticationKeys(this.availableKeys);
        this.init();
    }
    
    public void init()
    {
        this.state = TrapTransportState.DISCONNECTED;
        this.messagesInTransit = new LinkedList<TrapMessage>();
        this.lastAlive = 0;
        this.keepalivePredictor.stop();
        this.keepalivePredictor.setDelegate(this);
        
        if (this.connectionTimeoutTask != null)
            this.connectionTimeoutTask.cancel(true);
        this.connectionTimeoutTask = null;
        
        if (this.disconnectExpiry != null)
            this.disconnectExpiry.cancel();
        this.disconnectExpiry = null;
    }
    
    public boolean isEnabled()
    {
        return this.enabled;
    }
    
    public boolean isConnected()
    {
        return (this.getState() == TrapTransportState.CONNECTED) || (this.getState() == TrapTransportState.AVAILABLE) || this.getState() == TrapTransportState.UNAVAILABLE;
    }
    
    public int getTransportPriority()
    {
        return this.transportPriority;
    }
    
    public void setTransportPriority(int priority)
    {
        this.transportPriority = priority;
    }
    
    public void setConfiguration(TrapConfiguration configuration)
    {
        this.configuration = configuration;
        this.updateConfig();
    }
    
    public void configure(String configurationKey, String configurationValue)
    {
        if (!configurationKey.startsWith(this.prefix))
            configurationKey = this.prefix + "." + configurationKey;
        this.configuration.setOption(configurationKey, configurationValue);
        this.updateConfig();
    }
    
    protected void updateConfig()
    {
        String eString = this.getOption(OPTION_ENABLED);
        if (eString != null)
        {
            try
            {
                this.enabled = Boolean.valueOf(eString);
            }
            catch (Exception e)
            {
                this.logger.warn("Failed to parse transport {} enabled flag", this.getTransportName(), e);
            }
        }
        
        if (!this.isEnabled())
            return;
        
        String prioString = this.getOption(OPTION_PRIORITY);
        if (prioString != null)
        {
            try
            {
                this.transportPriority = Integer.parseInt(prioString);
            }
            catch (Exception e)
            {
                this.logger.warn("Failed to parse transport {} priority", this.getTransportName(), e);
            }
        }
        
        int option = this.configuration.getIntOption("trap.keepalive.interval", this.keepalivePredictor.getKeepaliveInterval());
        this.setKeepaliveInterval(option);
        
        option = this.configuration.getIntOption("trap.keepalive.expiry", (int) this.keepalivePredictor.getKeepaliveExpiry());
        this.setKeepaliveExpiry(option);
        
        // Note: Loggerprefix is a global setting; inherit from parent.
        String loggerString = this.configuration.getOption(OPTION_LOGGERPREFIX);
        if (loggerString != null)
        {
            this.logger = LoggerFactory.getLogger(loggerString + StringUtil.getLoggerComponent(this));
            
            if (this.keepalivePredictor instanceof StaticKeepalivePredictor)
                ((StaticKeepalivePredictor) this.keepalivePredictor).logger = this.logger;
        }
        
    }
    
    public void configure(String configurationKey, int configurationValue) throws TrapException
    {
        this.configure(configurationKey, Integer.toString(configurationValue));
    }
    
    public String getConfiguration()
    {
        return this.configuration.toString();
    }
    
    public boolean canConnect()
    {
        return false;
    }
    
    public boolean canListen()
    {
        return false;
    }
    
    public void setTransportDelegate(TrapTransportDelegate delegate, Object context)
    {
        this.delegate = delegate;
        this.delegateContext = context;
    }
    
    public void setAuthentication(TrapAuthentication authentication) throws TrapException
    {
        this.authentication = authentication;
        this.contextKeys = authentication.getContextKeys(this.availableKeys);
        this.contextMap = new HashMap<String, Object>();
        
        this.fillContext(this.contextMap, this.contextKeys);
    }
    
    /**
     * To be implemented by children.
     * 
     * @see TrapTransportProtocol
     * @return The protocol name of the protocol used by the transport.
     */
    protected abstract String getProtocolName();
    
    public boolean isAvailable()
    {
        return this.state == TrapTransportState.AVAILABLE;
    }
    
    /**
     * Asks the transport to fill the set with the available context keys it can provide for authentication. These keys
     * will be offered to the authentication provider, and can not be changed after the call to this function. The keys
     * are set on a per-transport basis.
     * <p>
     * This function MUST NOT throw.
     * 
     * @param keys
     *            The keys to fill in. The transport should only add keys to this set.
     */
    public void fillAuthenticationKeys(HashSet<String> keys)
    {
        keys.add(TrapContextKeys.Transport);
        keys.add(TrapContextKeys.Protocol);
        keys.add(TrapContextKeys.Configuration);
        keys.add(TrapContextKeys.State);
        keys.add(TrapContextKeys.LastAlive);
        keys.add(TrapContextKeys.Priority);
        keys.add(TrapContextKeys.Format);
    };
    
    /**
     * Asks the subclass to update the context map, filling in the keys. This can be called, for example, when a new
     * authentication method is set that may have modified contextKeys.
     */
    public void fillContext(Map<String, Object> context, Collection<String> filter)
    {
        
        if (filter.contains(TrapContextKeys.Transport))
            context.put(TrapContextKeys.Transport, this);
        
        if (filter.contains(TrapContextKeys.Protocol))
            context.put(TrapContextKeys.Protocol, this.getProtocolName());
        
        if (filter.contains(TrapContextKeys.State))
            context.put(TrapContextKeys.State, this.getState());
        
        if (filter.contains(TrapContextKeys.Configuration))
            context.put(TrapContextKeys.Configuration, this.getConfiguration().toString());
        
        if (filter.contains(TrapContextKeys.LastAlive))
            context.put(TrapContextKeys.LastAlive, this.lastAlive);
        
        if (filter.contains(TrapContextKeys.Priority))
            context.put(TrapContextKeys.Priority, this.getTransportPriority());
        
        if (filter.contains(TrapContextKeys.Format))
            context.put(TrapContextKeys.Format, this.getFormat());
    }
    
    public Collection<String> getAuthenticationKeys()
    {
        HashSet<String> rv = new HashSet<String>();
        this.fillAuthenticationKeys(rv);
        return rv;
    }
    
    public Map<String, Object> getAuthenticationContext()
    {
        return this.getAuthenticationContext(this.getAuthenticationKeys());
    }
    
    public Map<String, Object> getAuthenticationContext(Collection<String> authenticationKeys)
    {
        Map<String, Object> rv = new HashMap<String, Object>();
        this.fillContext(rv, authenticationKeys);
        return rv;
    }
    
    /**
     * Performs the actual sending of a TrapMessage. This method MUST NOT perform any checks on the outgoing messages.
     * It may still perform checks on the transport, and throw appropriately.
     * 
     * @param message
     *            The message to send.
     * @throws TrapTransportException
     *             If an error occurred while trying to send the message. Before this exception is thrown, the transport
     *             MUST change its state to ERROR, as it means this transport can no longer be used.
     */
    public abstract void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException;
    
    /**
     * Changes the state of the transport, and notifies the listener.
     * 
     * @param newState
     *            The state to change to.
     */
    protected void setState(TrapTransportState newState)
    {
        if (newState == this.state)
            return;
        
        TrapTransportState oldState = this.state;
        this.state = newState;
        
        if (this.delegate == null)
            this.logger.trace("Transport {} changed state from {} to {}", new Object[] { this.getTransportName(), oldState, newState });
        
        try
        {
            this.delegate.ttStateChanged(newState, oldState, this, this.delegateContext);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        if ((newState == TrapTransportState.AVAILABLE) && (this.connectionTimeoutTask != null))
        {
            this.connectionTimeoutTask.cancel(true);
            this.connectionTimeoutTask = null;
        }
        
        // Autostart keepalives, if applicable.
        if (newState == TrapTransportState.CONNECTED)
        {
            this.keepalivePredictor.start();
        }
        
        // Autostart keepalives, if applicable.
        if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.DISCONNECTING) || (newState == TrapTransportState.ERROR))
        {
            this.keepalivePredictor.stop();
            
            if (this.disconnectExpiry != null)
                this.disconnectExpiry.cancel();
            this.disconnectExpiry = null;
            
            if (this.messagesInTransit.size() > 0)
            {
                ThreadPool.executeCached(new Runnable() {
                    
                    public void run()
                    {
                        AbstractTransport.this.delegate.ttMessagesFailedSending(AbstractTransport.this.messagesInTransit, AbstractTransport.this, AbstractTransport.this.delegateContext);
                        
                    }
                });
            }
        }
        
        // Flush transport specific messages (again, if applicable)
        if ((newState == TrapTransportState.AVAILABLE) && (oldState == TrapTransportState.UNAVAILABLE) || newState == TrapTransportState.CONNECTED)
        {
            ThreadPool.executeAfter(new Runnable() {
                
                public void run()
                {
                    AbstractTransport.this.flushTransportMessages(false);
                }
            }, 5);
        }
    }
    
    public void enable()
    {
        this.configure(OPTION_ENABLED, "true");
    }
    
    public void disable()
    {
        this.configure(OPTION_ENABLED, "false");
        this.disconnect();
    }
    
    public void connect() throws TrapException
    {
        if (!this.isEnabled())
            throw new TrapException("Transport " + this.getTransportName() + " is unavailable...");
        
        if (!this.canConnect())
            throw new TrapException("Transport " + this.getTransportName() + " cannot act as a client");
        
        if (this.getState() != TrapTransportState.DISCONNECTED)
            throw new TrapException("Cannot connect from state that is not DISCONNECTED");
        
        if (!this.isClientConfigured())
        {
            this.logger.debug("Configuration Error. {} not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.", this);
            this.setState(TrapTransportState.ERROR);
            return;
        }
        
        this.setState(TrapTransportState.CONNECTING);
        
        // Give the connection 15 seconds to move into a OPEN state before we purge it.
        this.connectionTimeoutTask = ThreadPool.weakExecuteAfter(new Runnable() {
            
            public void run()
            {
                if ((AbstractTransport.this.getState() == TrapTransportState.CONNECTING) || (AbstractTransport.this.getState() == TrapTransportState.CONNECTED))
                {
                    AbstractTransport.this.logger.debug("Connection Error. {} failed to move to state OPEN after 15 seconds... purging it", AbstractTransport.this);
                    AbstractTransport.this.disconnect();
                    AbstractTransport.this.connectionTimeoutTask = null;
                }
            }
        }, 15000);
        
        this.internalConnect();
    }
    
    protected abstract void internalConnect() throws TrapException;
    
    public void disconnect()
    {
        if ((this.state == TrapTransportState.DISCONNECTING) || (this.state == TrapTransportState.DISCONNECTED) || (this.state == TrapTransportState.ERROR))
            return; // Cannot re-disconnect
            
        if (this.getState() == TrapTransportState.CONNECTING)
        {
            this.internalDisconnect();
            return;
        }
        
        try
        {
            
            this.flushTransport();
            
            // Any outstanding messages will be considered FAILED.
            // They will be resent by the main thread.
            this.setState(TrapTransportState.DISCONNECTING);
            LinkedList<TrapMessage> failedMessages = new LinkedList<TrapMessage>();
            synchronized (this.messagesInTransit)
            {
                failedMessages.addAll(this.messagesInTransit);
                this.messagesInTransit.clear();
            }
            if (failedMessages.size() > 0)
                this.delegate.ttMessagesFailedSending(failedMessages, this, this.delegateContext);
            
            this.sendTransportSpecific(this.createMessage().setOp(Operation.CLOSE));
            //this.internalSend(, false);
            this.keepalivePredictor.dataSent();
            this.disconnectExpiry = ThreadPool.weakExecuteAfter(new Runnable() {
                
                public void run()
                {
                    TrapTransportState cs = AbstractTransport.this.getState();
                    if (cs == TrapTransportState.DISCONNECTED)
                        return;
                    AbstractTransport.this.internalDisconnect();
                    AbstractTransport.this.logger.debug("Disconnection Error. {} moving to state ERROR as failed to disconnect in time. Triggering state was {}", this, cs);
                    AbstractTransport.this.setState(TrapTransportState.ERROR);
                }
                
            }, this.keepalivePredictor.getKeepaliveExpiry());
        }
        catch (Exception e)
        {
            this.internalDisconnect();
            this.logger.debug("Disconnection Error. {} moving to state ERROR due to exception", this, e);
            this.setState(TrapTransportState.ERROR);
        }
    }
    
    protected abstract void internalDisconnect();
    
    /* Transport (Abstract) logic follows! This logic will refer to the MOST PARANOID TRANSPORT and MUST be overridden by LESS PARANOID transports */
    
    /**
     * Send checks if the transport is in the correct state, if the message is authenticated (otherwise adds
     * authentication) and performs additional checks when needed.
     */
    public void send(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        if ((this.state != TrapTransportState.AVAILABLE) && (this.state != TrapTransportState.CONNECTED))
            throw new TrapTransportException(message, this.state);
        
        message.setAuthData(this.authentication.createAuthenticationResponse(null, message, this.headersMap, this.contextMap));
        
        if (this.logger.isTraceEnabled())
            this.logger.trace("Sending {} on transport {} for {}.", new Object[] { message, this, this.delegate });
        
        this.internalSend(message, expectMore);
        
        // Only add to transit message if internalSend exits successfully.
        if (message.getMessageId() != 0)
            this.addTransitMessage(message);
        
        this.keepalivePredictor.dataSent();
    }
    
    public void sendTransportSpecific(TrapMessage message)
    {
        message.setAuthData(this.authentication.createAuthenticationResponse(null, message, this.headersMap, this.contextMap));
        
        synchronized (this.transportMessageBuffer)
        {
            this.transportMessageBuffer.add(message);
        }
        
        //System.out.println("Flush Queued; " + this + " size:" + this.transportMessageBuffer.size());
        ThreadPool.executeCached(new Runnable() {
            
            public void run()
            {
                AbstractTransport.this.flushTransportMessages(false);
            }
        });
    }
    
    protected Object transportMessageFlushLock = new Object();
    
    protected void flushTransportMessages(boolean expectMoreAtEnd)
    {
        synchronized (this.transportMessageFlushLock)
        {
            //System.out.println("Flush beginning; " + this + " size:" + this.transportMessageBuffer.size());
            
            synchronized (this.transportMessageBuffer)
            {
                if (this.transportMessageBuffer.isEmpty())
                    return;
            }
            
            while ((this.getState() == TrapTransportState.AVAILABLE || this.getState() == TrapTransportState.CONNECTED || this.getState() == TrapTransportState.CONNECTING || this.getState() == TrapTransportState.DISCONNECTING) && (this.transportMessageBuffer.size() > 0))
            {
                TrapMessage message = null;
                try
                {
                    synchronized (this.transportMessageBuffer)
                    {
                        message = this.transportMessageBuffer.remove();
                    }
                    
                    if (this.logger.isTraceEnabled())
                        this.logger.trace("Sending {}/{} on transport {} for {}.", new Object[] { message.getOp(), message.getMessageId(), this, this.delegate });
                    this.internalSend(message, true);
                    this.keepalivePredictor.dataSent();
                }
                catch (NoSuchElementException e)
                {
                }
                catch (TrapTransportException e)
                {
                    synchronized (this.transportMessageBuffer)
                    {
                        this.transportMessageBuffer.addFirst(message);
                    }
                    if (!expectMoreAtEnd)
                        this.flushTransport();
                    return;
                }
            }
            
            //System.out.println("Flush ending; " + this + " size:" + this.transportMessageBuffer.size());
            if (!expectMoreAtEnd)
                this.flushTransport();
        }
    }
    
    ByteArrayOutputStream bos         = new ByteArrayOutputStream();
    
    /**
     * Call this when data is received.
     * 
     * @param data
     */
    int                   i           = 0;
    
    // There is no reason for receive to be publicly synchronized other than prevent it from
    // being called concurrently. We can order the requests using a lock better
    private Object        receiveLock = new Object();
    
    public void receive(byte[] data, int offset, int length)
    {
        
        synchronized (this.receiveLock)
        {
            int consumed = 0;
            
            //if (new String(data).contains("HTTP"))
            //	System.out.println("Lolwut?");
            
            try
            {
                // We need to handle the case where message data is spread out over two or more incoming data blobs (e.g. socket, udp, etc)...
                // Therefore, we'll need to do some buffer shuffling.
                
                this.bos.write(data, offset, length);
                byte[] dataArray = this.bos.toByteArray();
                
                do
                {
                    
                    // Saving these for posterity. Essentially, this was an attempt to check the integrity of the buffers.
                    // There may be a case to be made for a Trap Checksum, but I'm not going all the way there yet.
                    //					if (dataArray[consumed] > 32)
                    //						System.out.println("Potential break in the system here");
                    
                    TrapMessage m = new TrapMessageImpl();
                    int thisLoop = m.deserialize(dataArray, consumed, dataArray.length - consumed);
                    
                    if (thisLoop == -1)
                    {
                        
                        //						if (dataArray[consumed] > 32 || dataArray[consumed] == 5)
                        //							System.out.println("Potential break in the system here");
                        
                        break;
                    }
                    
                    this.receiveMessage(m);
                    
                    consumed += thisLoop;
                } while (consumed < dataArray.length);
                
                if (consumed > 0)
                {
                    this.bos = new ByteArrayOutputStream();
                    try
                    {
                        this.bos.write(dataArray, consumed, dataArray.length - consumed);
                    }
                    catch (Throwable t)
                    {
                        System.out.println(t);
                    }
                }
            }
            catch (UnsupportedEncodingException e)
            {
                this.sendTransportSpecific(this.createMessage().setOp(Operation.END));
                
                // Close the transport, since it's invalid
                // It's illegal to raise an UnsupportedEncodingException at this point in time.
                this.internalDisconnect();
            }
            catch (UnsupportedOperationException e)
            {
                // The transport has an erroneous buffer. Clear our local buffer, then disconnect.
                this.logger.warn("Transport Error: {} received a Trap message with an unsupported operation. This means one of two things: either you are connecting to a newer version of Trap, or the data on this transport is corrupted.", this);
                this.logger.warn("Transport Error: Dumping the data that caused this error in level DEBUG...");
                
                byte[] dataArray = this.bos.toByteArray();
                
                int start = consumed;
                int end = Math.min(dataArray.length, consumed + 400);
                
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                
                for (int i = start; i < end; i++)
                {
                    sb.append(dataArray[i]);
                    sb.append(", ");
                }
                
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);
                
                sb.append("]");
                
                this.logger.debug(sb.toString());
                
                this.logger.warn("Transport Error: {} terminating forcefully to prevent further application corruption", this);
                
                this.sendTransportSpecific(this.createMessage().setOp(Operation.END));
                
                this.internalDisconnect();
            }
        }
    }
    
    public String toString()
    {
        return this.getTransportName() + "/" + this.getState() + "/" + Integer.toHexString(this.hashCode());
    }
    
    /**
     * Called when a message is received, in the most general case.
     * 
     * @param message
     */
    public void receiveMessage(TrapMessage message)
    {
        
        if (this.logger.isTraceEnabled())
            this.logger.trace("Received: {}/{}/{} on {} for {}", new Object[] { message.getOp(), message.getMessageId(), message.getChannel(), this, this.delegate });
        
        this.lastAlive = System.currentTimeMillis();
        this.keepalivePredictor.dataReceived();
        
        // Authenticated message.
        boolean propagate = true;
        
        switch (message.getOp().getOp())
        {
            case Operation.Value.OPEN:
                propagate = this.onOpen(message);
                break;
            
            case Operation.Value.OPENED:
                propagate = this.onOpened(message);
                break;
            
            case Operation.Value.CLOSE:
                propagate = this.onClose(message);
                break;
            
            case Operation.Value.END:
                propagate = this.onEnd(message);
                break;
            
            case Operation.Value.CHALLENGE:
                propagate = this.onChallenge(message);
                break;
            
            case Operation.Value.ERROR:
                propagate = this.onError(message);
                break;
            
            case Operation.Value.FRAGMENT_START:
            case Operation.Value.FRAGMENT_END:
            case Operation.Value.MESSAGE:
                propagate = this.onMessage(message);
                break;
            
            case Operation.Value.ACK:
                propagate = false;
                this.onAck(message);
                break;
            
            case Operation.Value.OK:
                propagate = this.onOK(message);
                break;
            
            case Operation.Value.PING:
                propagate = this.onPing(message);
                propagate = false;
                break;
            
            case Operation.Value.PONG:
                propagate = this.onPong(message);
                propagate = false;
                break;
            
            case Operation.Value.TRANSPORT:
                propagate = this.onTransport(message);
                break;
            
            default:
                return;
                
        }
        
        if (propagate)
        {
            this.delegate.ttMessageReceived(message, this, this.delegateContext);
            this.acknowledgeTransitMessage(message);
        }
    }
    
    /**
     * Transport messages are most often handled by the Trap layer, then repropagated down. The transport CAN attempt to
     * intercept some but it is NOT recommended.
     * 
     * @param message
     * @return
     */
    protected boolean onTransport(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            
        }
        
        return authed;
    }
    
    /**
     * Ping/Pong should be left to Trap.
     * 
     * @param message
     * @return
     */
    protected boolean onPong(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            byte[] bs = message.getData();
            char type = (char) bs[0];
            
            byte[] data = new byte[bs.length - 7];
            System.arraycopy(bs, 7, data, 0, bs.length - 7);
            
            int time = 30;
            try
            {
                time = Integer.parseInt(new String(bs, 1, 6, "UTF-8"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }       
            
            if (this.logger.isTraceEnabled())
                this.logger.trace("Handling PONG. Type is {} and data is {}", type, new String(bs, 1, 6, Charset.forName("UTF-8")));
            
            
            if (type != '3')
                this.keepalivePredictor.keepaliveReceived(false, type, time, data);
            else
            {
                String payload = StringUtil.toUtfString(data);
                SingleCallback<Boolean> mcb = this.livenessMap.get(payload);
                if (mcb != null)
                    mcb.callback(true);
                this.livenessMap.remove(payload);
            }
            
        }
        
        return authed;
    }
    
    /**
     * Ping/Pong should be left to Trap.
     * 
     * @param message
     * @return
     */
    protected boolean onPing(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        //System.out.println("PING RECEIVED " + authed + " on " + this);
        
        if (authed)
        {
            // Prevent a silly issue where a disconnecting transport would try and send a pong and error on itself.
            if ((this.getState() == TrapTransportState.DISCONNECTING) || (this.getState() == TrapTransportState.DISCONNECTED))
                return authed;
            
            // Parse message data
            byte[] bs = message.getData();
            char type = (char) bs[0];
            byte[] data = new byte[bs.length - 7];
            System.arraycopy(bs, 7, data, 0, bs.length - 7);
            
            int timer = 30;
            try
            {
                timer = Integer.parseInt(new String(bs, 1, 6, "UTF-8"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            
            // We don't need to send anything; the validator will create a PONG message if needed.
            
            if (type != '3')
                this.keepalivePredictor.keepaliveReceived(true, type, timer, data);
            else
                // isAlive() call
                this.sendKeepalive(false, type, timer, data);
        }
        
        return authed;
    }
    
    private String padTimerStr(String timerStr)
    {
        while (timerStr.length() < 6)
        {
            if (timerStr.startsWith("-"))
                timerStr = "-0" + timerStr.substring(1);
            else
                timerStr = "0" + timerStr;
        }
        return timerStr;
    }
    
    /**
     * General ack. Used by Trap; the transport need not apply.
     * 
     * @param message
     * @return
     */
    protected boolean onOK(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            
        }
        
        return authed;
    }
    
    /**
     * Transport should not care for these.
     * 
     * @param message
     * @return
     */
    protected boolean onMessage(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            
        }
        
        return authed;
    }
    
    /**
     * Transport MAY inspect these.
     * 
     * @param message
     * @return
     */
    protected boolean onError(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            
        }
        
        return authed;
    }
    
    /**
     * Transport MUST intercept these.
     * 
     * @param message
     * @return
     */
    protected boolean onChallenge(TrapMessage message)
    {
        // We received a challenge.
        
        try
        {
            TrapMessageImpl original = new TrapMessageImpl(message.getData());
            String response = this.authentication.createAuthenticationResponse(message, original, this.headersMap, this.contextMap);
            original.setAuthData(response);
            this.sendTransportSpecific(original);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Transport MUST NOT intercept these
     * 
     * @param message
     * @return
     */
    protected boolean onEnd(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            
        }
        
        return authed;
    }
    
    /**
     * Transport MUST intercept these.
     * 
     * @param message
     * @return
     */
    protected boolean onClose(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            this.disconnect();
            this.internalDisconnect();
            this.setState(TrapTransportState.DISCONNECTED);
        }
        
        return false;
    }
    
    /**
     * Transport MUST intercept these.
     * 
     * @param message
     * @return
     */
    protected boolean onOpened(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            if (this.getState() == TrapTransportState.UNAVAILABLE || this.getState() == TrapTransportState.CONNECTED)
                this.setState(TrapTransportState.AVAILABLE);
            else
                this.logger.debug("Potential race: Transport received onOpen while not connecting");
        }
        
        return authed;
    }
    
    /**
     * Transport MUST intercept these.
     * 
     * @param message
     * @return
     */
    protected boolean onOpen(TrapMessage message)
    {
        boolean authed = this.checkAuthentication(message);
        
        if (authed)
        {
            if (this.getState() == TrapTransportState.UNAVAILABLE || this.getState() == TrapTransportState.CONNECTED)
                this.setState(TrapTransportState.AVAILABLE);
            else
                this.logger.debug("Potential race: Transport received onOpen while not connecting");
        }
        else
        {
            // The challenge will have been sent by checkAuth
            // We don't really need to do anything; we'll receive a new
            // OPEN event...
        }
        
        return authed;
    }
    
    protected boolean checkAuthentication(TrapMessage message)
    {
        
        boolean authed;
        try
        {
            authed = this.authentication.verifyAuthentication(message, this.headersMap, this.contextMap);
        }
        catch (TrapAuthenticationException e1)
        {
            this.logger.debug("Disconnecting transport [{}] due to authentication failure", this.getTransportName());
            this.disconnect();
            return false;
        }
        
        if (!authed)
        {
            // Challenge
            String authChallenge = this.authentication.createAuthenticationChallenge(message, this.contextMap);
            TrapMessage challenge = this.createMessage();
            challenge.setOp(Operation.CHALLENGE);
            challenge.setAuthData(authChallenge);
            
            try
            {
                challenge.setData(message.serialize());
                this.sendTransportSpecific(challenge);
            }
            catch (IOException e)
            {
                this.logger.warn("Something happened", e);
            }
        }
        
        return authed;
    }
    
    public String getOption(String option)
    {
        if (!option.startsWith(this.prefix))
            option = this.prefix + "." + option;
        
        // Try prefixed
        String rv = this.configuration.getOption(option);
        
        // Get unprefixed (=parent)
        if (rv == null)
            rv = this.configuration.getOption(option.substring(this.prefix.length() + 1));
        
        return rv;
    }
    
    public int getIntOption(String option, int defaultValue)
    {
        String opt = this.getOption(option);
        try
        {
            return Integer.parseInt(opt);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
    public boolean getBooleanOption(String option, boolean defaultValue)
    {
        String opt = this.getOption(option);
        try
        {
            return Boolean.parseBoolean(opt);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
    public TrapTransportState getState()
    {
        return this.state;
    }
    
    public long lastAlive()
    {
        return this.lastAlive;
    }
    
    public Callback<Boolean> isAlive(final long within, final boolean check, final long timeout)
    {
        
        final SingleCallback<Boolean> cb = new SingleCallback<Boolean>();
        
        if ((System.currentTimeMillis() - within) < AbstractTransport.this.lastAlive)
            return cb.callback(true);
        
        if (!check)
            return cb.callback(false);
        
        ThreadPool.executeCached(new Runnable() {
            
            public void run()
            {
                
                final String payload = (UID.randomUID() + System.currentTimeMillis());
                AbstractTransport.this.livenessMap.put(payload, cb);
                
                ThreadPool.executeAfter(new Runnable() {
                    
                    public void run()
                    {
                        cb.callback(false);
                        AbstractTransport.this.livenessMap.remove(payload);
                    }
                }, timeout);
                
                // Now perform the blaady check
                AbstractTransport.this.sendKeepalive(true, '3', 0, StringUtil.toUtfBytes(payload));
            }
        });
        
        return cb;
    }
    
    /**
     * @param type
     *            The type of ping message (0 - disable, 1 - automatic, 2 - manual timer, 3 - manual triggered)
     * @param timer
     *            The time until the next automated keepalive
     * @param data
     *            Application specific data
     * @throws TrapException
     */
    protected void sendKeepalive(boolean ping, char type, int timer, byte[] data)
    {
        
        try
        {
            
            TrapMessage m = this.createMessage();
            
            if (ping)
                m.setOp(Operation.PING);
            else
                m.setOp(Operation.PONG);
            
            // Set up the data
            byte[] bs = new byte[data.length + 7];
            bs[0] = (byte) type;
            
            String timerStr = Integer.toString(timer);
            timerStr = this.padTimerStr(timerStr);
            
            byte[] timerBs = StringUtil.toUtfBytes(timerStr);
            
            System.arraycopy(timerBs, 0, bs, 1, 6);
            System.arraycopy(data, 0, bs, 7, data.length);
            
            m.setData(bs);
            m.setMessageId(0);
            
            this.sendTransportSpecific(m);
            
        }
        catch (Exception e)
        {
            
            if ((this.getState() != TrapTransportState.AVAILABLE) && (this.getState() != TrapTransportState.CONNECTED))
            {
                this.logger.debug("Keepalive error: {} terminating keepalive sending due to {} ", this, e.toString());
            }
            else
            {
                this.logger.warn("Exception while trying to send keepalive on {}", this);
                e.printStackTrace();
            }
            return;
        }
        
    }
    
    public int getKeepaliveInterval()
    {
        return this.keepalivePredictor.getKeepaliveInterval();
    }
    
    public void setKeepaliveInterval(int newInterval)
    {
        this.keepalivePredictor.setKeepaliveInterval(newInterval);
        
        if ((this.state == TrapTransportState.AVAILABLE) || (this.state == TrapTransportState.CONNECTED))
            this.keepalivePredictor.start();
    }
    
    public void predictedKeepaliveExpired(TrapKeepalivePredictor predictor, long msec)
    {
        this.logger.debug("Keepalive timer for {} expired. Moving to DISCONNECTED.", this.getTransportName());
        this.setState(TrapTransportState.DISCONNECTED);
    }
    
    public void shouldSendKeepalive(boolean isPing, char type, int timer, byte[] data)
    {
        this.sendKeepalive(isPing, type, timer, data);
    }
    
    public TrapKeepalivePredictor getKeepalivePredictor()
    {
        return this.keepalivePredictor;
    }
    
    /**
     * Assigns a new predictor. See {@link TrapKeepalivePredictor}. Before assigning a predictor to a transport, ensure
     * the predictor supports the transport. Some transports, e.g. http, do not support or require keepalive prediction
     * and thus disable it.
     * 
     * @param newPredictor
     *            the new predictor.
     */
    public void setKeepalivePredictor(TrapKeepalivePredictor newPredictor)
    {
        this.keepalivePredictor = newPredictor;
    }
    
    /**
     * @see TrapKeepalivePredictor#setKeepaliveExpiry(long)
     * @param newExpiry
     */
    public void setKeepaliveExpiry(long newExpiry)
    {
        this.keepalivePredictor.setKeepaliveExpiry(newExpiry);
    }
    
    boolean warnAddressConfigurationPerformed = false;
    
    protected void warnAddressConfiguration()
    {
        if (this.warnAddressConfigurationPerformed)
            return;
        
        if (!this.configuration.getBooleanOption("warnAddressConfiguration", true))
            return;
        
        this.warnAddressConfigurationPerformed = true;
        
        this.logger.warn("Configuration Error: {} could not detect a single public address; may need configuration!", this);
    }
    
    protected String getHostName(InetAddress address, boolean defaultConfig, boolean failOnUnreachable)
    {
        String hostName;
        
        if (IPUtil.isLocal(address) && defaultConfig)
        {
            InetAddress[] addresses = IPUtil.getPublicAddresses();
            
            if (addresses.length != 1)
                this.warnAddressConfiguration();
            
            if (addresses.length == 0)
            {
                hostName = "127.0.0.1";
            }
            else
            {
                InetAddress a = addresses[0];
                hostName = IPUtil.getAddressForURI(a);
            }
            
        }
        else
        {
            hostName = IPUtil.getAddressForURI(address);
            
            // Check for zero-address
            byte[] bs = address.getAddress();
            
            boolean zeroAddress = true;
            
            for (int i = 0; i < bs.length; i++)
                if (bs[i] != 0)
                    zeroAddress = false;
            
            if (zeroAddress && failOnUnreachable)
                throw new AutoconfigurationDisabledException("Transport " + this.getTransportName() + " configured with zero address: " + hostName);
            
        }
        
        return hostName;
    }
    
    public boolean isConfigured(boolean client, boolean server)
    {
        boolean rv = true;
        
        if (client)
            rv = rv && this.isClientConfigured();
        
        if (server)
            rv = rv && this.isServerConfigured();
        
        return rv;
    }
    
    /**
     * Asks whether the transport has the proper configuration for its server role. Must return false if the transport
     * cannot be a server.
     * 
     * @return
     */
    protected boolean isServerConfigured()
    {
        // Most servers are configured by default, so we'll adjust the default accordingly
        return this.canListen();
    }
    
    /**
     * Asks whether the transport has the proper configuration for its client role. Must return false if the transport
     * cannot be a client.
     * 
     * @return
     */
    protected boolean isClientConfigured()
    {
        return false;
    }
    
    public void forceError()
    {
        if (this.logger.isTraceEnabled())
        {
            this.logger.trace("Error was forced", new Exception());
        }
        this.setState(TrapTransportState.ERROR);
    }
    
    /*
     * The following methods deal with messages-in-transit
     */
    
    protected void onAck(TrapMessage message)
    {
        
        boolean authed = this.checkAuthentication(message);
        
        if (!authed)
            return;
        
        byte[] data = message.getData();
        
        ByteBuffer buf = ByteBuffer.wrap(data);
        
        if (message.getFormat() == Format.REGULAR)
        {
            int messageID, channelID;
            while (buf.remaining() > 0)
            {
                channelID = buf.get();
                messageID = buf.getInt();
                this.removeTransitMessageById(messageID, channelID);
            }
            
        }
        else
        {
            for (int i = 0; i < data.length; i += 4)
            {
                
                // The data will be encoded as an integer noting what message to ack
                int messageId = -1;
                messageId = ByteConverter.fromBigEndian7(data, i);
                this.removeTransitMessageById(messageId, 0);
            }
            
        }
        
    }
    
    /**
     * Notes to the system that a message is in transit. Some transports (e.g. loopback) are unconcerned about this.
     * HTTP can also reasonably deduce when a transit message has arrived. However, some transports (e.g. socket) have
     * no built-in acknowledgement sequence for a complete message, and may be broken during the course of a transfer.
     * <p>
     * The transit message features allow transports to use built-in methods to detect these failures and trigger
     * ttMessagesFailedSending. This will allow Trap to recover these messages.
     * <p>
     * Override this method to disable transit checking. For performance reasons,
     * {@link #acknowledgeTransitMessage(TrapMessage)} should be overridden as well.
     * 
     * @param m
     * @throws TrapTransportException
     */
    protected void addTransitMessage(TrapMessage m) throws TrapTransportException
    {
        
        if (m.getMessageId() == 0)
            return;
        
        this.logger.trace("Transport {} adding transit message {}", new Object[] { this.getTransportName(), m });
        
        synchronized (this.messagesInTransit)
        {
            if ((this.state != TrapTransportState.AVAILABLE) && (this.state != TrapTransportState.CONNECTED))
                throw new TrapTransportException(m, this.state);
            
            this.messagesInTransit.add(m);
            //if (this.messagesInTransit.size() > 1000)
            //this.setState(TrapTransportState.UNAVAILABLE);
        }
    }
    
    protected void removeTransitMessageById(long messageID, int channelID)
    {
        if (messageID == 0)
            return;
        
        try
        {
            synchronized (this.messagesInTransit)
            {
                this.logger.trace("Transport {} received ack for message id C{}/{}. Got transit list: {}", new Object[] { this.getTransportName(), channelID, messageID, this.messagesInTransit });
                Iterator<TrapMessage> it = this.messagesInTransit.iterator();
                
                boolean first = true;
                
                while (it.hasNext())
                {
                    TrapMessage m = it.next();
                    
                    if (m.getMessageId() == messageID && m.getChannel() == channelID)
                    {
                        it.remove();
                        this.logger.trace("Removed transit message {}; new list is {}", m, this.messagesInTransit);
                        
                        if (!first)
                        {
                            // This implies dropped messages!!!
                            //this.logger.error("It appears we have dropped some messages on an otherwise working transport. Most likely, this transport is bugged; please report this. Transport was {}", this);
                            // Solution: Resend.
                            this.sendTransportSpecific(this.messagesInTransit.peek());
                        }
                        
                        this.delegate.ttMessageSent(m, this, this.delegateContext);
                        
                        return;
                    }
                    
                    first = false;
                }
            }
        }
        finally
        {
            //if (this.messagesInTransit.size() < 500)
            //this.setState(TrapTransportState.AVAILABLE);
        }
    }
    
    long       lastAckTimestamp = 0;
    ByteBuffer acks             = ByteBuffer.allocate(640);
    Future     ackTask          = null;
    
    protected void acknowledgeTransitMessage(TrapMessage message)
    {
        if (message.getMessageId() == 0)
            return;
        
        /*byte[] ackData = null;
        
        if (this.format == Format.REGULAR)
        	ackData = ByteConverter.toBigEndian(message.getMessageId());
        else
        	ackData = ByteConverter.toBigEndian7(message.getMessageId());
        */
        
        synchronized (this.acks)
        {
            
            //System.out.println("Adding ack " + ackData[0] + ":" + ackData[1] + ":"+ ackData[2] + ":" + ackData[3]);
            
            if (this.acks.remaining() < 5)
            {
                this.flushAcks();
            }
            
            if (this.format == Format.REGULAR)
            {
                this.acks.put((byte) message.getChannel());
                this.acks.putInt(message.getMessageId());
            }
            else
            {
                this.acks.put(ByteConverter.toBigEndian7(message.getMessageId()));
            }
            
            long ctm = System.currentTimeMillis();
            
            if (this.lastAckTimestamp >= (ctm - 5))
            {
                // We have to schedule a task, in case nobody ever comes knocking...
                if (this.ackTask == null)
                {
                    this.ackTask = ThreadPool.weakExecuteAfter(new Runnable() {
                        
                        public void run()
                        {
                            synchronized (AbstractTransport.this.acks)
                            {
                                AbstractTransport.this.ackTask = null;
                                AbstractTransport.this.flushAcks();
                            }
                        }
                    }, 6);
                }
                
                return;
            }
            else
            {
                this.flushAcks();
            }
            
        }
    }
    
    /**
     * CALLER MUST SYNCHRONIZE ON ACKS
     */
    private void flushAcks()
    {
        this.lastAckTimestamp = System.currentTimeMillis();
        
        this.acks.flip();
        byte[] arr = new byte[this.acks.remaining()];
        this.acks.get(arr);
        this.acks.clear();
        
        /*byte[] arr = new byte[this.acks.size() * 4];
        Iterator it = this.acks.iterator();
        int i = 0;
        while (it.hasNext())
        {
        	byte[] bs = (byte[]) it.next();
        	System.arraycopy(bs, 0, arr, i, bs.length);
        	i += 4;
        }
        
        //System.out.println("Clearing ACK array and flushing");
        this.acks.clear();
        */
        
        TrapMessageImpl ack = (TrapMessageImpl) this.createMessage();
        
        ack.setOp(Operation.ACK);
        ack.setData(arr);
        this.sendTransportSpecific(ack);
    }
    
    public void setFormat(Format format)
    {
        this.format = format;
    }
    
    public Format getFormat()
    {
        return this.format;
    }
    
    protected TrapMessage createMessage()
    {
        return new TrapMessageImpl().setFormat(this.format);
    }
    
    public boolean isObjectTransport()
    {
        return false;
    }
    
    public Object getContext()
    {
        return this.delegateContext;
    }
    
    public void receiveTransportedMessage(TrapMessage msg)
    {
        this.logger.warn("Transported message lost. Check this transport's implementation. [{}]", msg);
    }
}
