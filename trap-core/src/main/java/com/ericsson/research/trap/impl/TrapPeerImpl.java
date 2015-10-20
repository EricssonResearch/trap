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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapPeer;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.TrapTransports;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.transports.AbstractTransport;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.UUID;
import com.ericsson.research.trap.utils.spi.ConfigurationImpl;

/*
 * IMPLEMENTATION NOTES!
 * 
 * TrapPeer will override the transport management of TrapEndpoint, implementing the following changes:
 * 
 * - addTransport() / removeTransport() are changed to noops. - Constructor will perform all the additions. They are
 * final, fwiw. - Transports will be added to availableTransports, as with the ClientTrapEndpoint. - Transports will
 * only be reconnected if they are from TrapEndpoint's transports list (and not active)
 * 
 * 
 * TrapPeer coopts the following Operations for its, err, operation over the side channel:
 * 
 * OPEN - Initial open dual handshake. A response is not expected. OPENED - Confirmation of open (NYI) PING - Secondary
 * config sending -- requesting reconnect. PONG - Secondary config response -- new config received (don't send reply).
 */
public class TrapPeerImpl extends TrapEndpointImpl implements TrapPeer, OnAccept
{
    
    TrapListener                   listener               = null;
    private TrapPeerChannel        channel;
    
    /*
     * The keys are used to determine peer priority.
     */
    private String                 remoteId;
    private ConfigurationImpl      localCfg               = new ConfigurationImpl();
    private ConfigurationImpl      remoteCfg              = new ConfigurationImpl();
    
    /*
     * The activeTransports collection serves the same purpose as in the client; 
     * keeping track of which transports are currently in use so as to correctly identify sleeping vs disconnected.
     */
    private HashSet<TrapTransport> activeTransports       = new HashSet<TrapTransport>();
    
    /**
     * The local ID, from the perspective of the current peer. On the other end, this will be read as the remote ID.
     */
    public static final String     KEY_SENDER_ID          = "peer_id_sender";
    
    /**
     * The remote ID, from the perspective of the current peer. On the other end, this will be read as the local ID, if
     * applicable.
     */
    public static final String     KEY_RECEIVER_ID        = "peer_id_receiver";
    
    /**
     * Key that signifies the transport key (to be configured) for transportable messages.
     */
    public static final String     KEY_TRANSPORT          = "transport";
    
    /**
     * The sender's ID conflicts with the receiver's ID. They should be regenerated.
     */
    public static final byte       ERR_DUPLICATE_ID       = 1;
    
    /**
     * No sender ID was specified in the message
     */
    public static final byte       ERR_NO_ID              = 2;
    
    /**
     * The receiver ID, when specified, is invalid. T
     */
    public static final byte       ERR_WRONG_ID           = 3;
    
    /**
     * The connection failed. No transports could successfully connect from this end.
     */
    public static final byte       ERR_TRANSPORTS_FAILED  = 4;
    
    private int                    localControlMessageId  = 0;
    private int                    remoteControlMessageId = 0;
    
    public TrapPeerImpl() throws TrapException
    {
        super();
        this.init();
    }
    
    public void open(TrapPeerChannel channel) throws TrapException
    {
        this.channel = channel;
        this.listener.listen(this);
        this.sendConfig(Operation.OPEN);
    }
    
    private void sendConfig(Operation op)
    {
        
        String cfg = this.listener.getClientConfiguration();
        
        this.localCfg = new ConfigurationImpl(cfg);
        this.localCfg.setOption(KEY_SENDER_ID, "" + this.trapID);
        this.localCfg.initFromString(this.listener.getClientConfiguration());
        
        TrapMessage m = this.createControlMessage();
        m.setData(StringUtil.toUtfBytes(this.localCfg.toString()));
        m.setCompressed(true);
        m.setOp(op);
        
        byte[] bs;
        try
        {
            bs = m.serialize();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        
        this.channel.sendToRemote(bs);
    }
    
    private synchronized TrapMessage createControlMessage()
    {
        return new TrapMessageImpl().setChannel(0).setFormat(Format.REGULAR).setMessageId(++this.localControlMessageId);
    }
    
    public void init() throws TrapException
    {
        this.listener = new ListenerTrapEndpoint() {
            
            @Override
            public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
            {
                TrapPeerImpl.this.logger.debug("Adding new transport [{}] to TrapEndpoint ID {}", transport, TrapPeerImpl.this.getTrapID());
                PeerInfo p = new PeerInfo(transport, false);
                transport.setTransportDelegate(TrapPeerImpl.this, p);
                TrapPeerImpl.this.addTransport(transport, message);
                TrapPeerImpl.this.activeTransports.add(transport);
                
                // Overwrite the delegate context.
                transport.setTransportDelegate(TrapPeerImpl.this, p);
            }
            
        };
        
        // Override maxActiveTransports
        // For peer-to-peer situations, we will derive this value later.
        this.maxActiveTransports = Integer.MAX_VALUE;
        this.trapID = UUID.randomUUID();
        
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
                
                PeerInfo p = new PeerInfo(t, true);
                t.setTransportDelegate(this, p);
                super.addTransport(t);
            }
        }
        catch (Exception e)
        {
            throw new TrapException(e);
        }
        
        this.setState(TrapState.OPENING);
        
    }
    
    @Override
    protected void reconnect(long timeout) throws TrapException
    {
        this.sendConfig(Operation.PING);
        
        long endTime = System.currentTimeMillis() + (timeout * this.transports.size());
        
        try
        {
            while ((this.getState() != TrapState.OPEN))
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
    
    private void sendError(byte error)
    {
        TrapMessage m = this.createControlMessage();
        m.setCompressed(false); // Dictionary would actually waste more space.
        m.setOp(Operation.ERROR);
        m.setData(new byte[] { error });
        try
        {
            this.channel.sendToRemote(m.serialize());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void receive(byte[] data)
    {
        try
        {
            TrapMessageImpl msg = new TrapMessageImpl(data);
            
            // Discard any old messages. Out of order counts too. We will only use the LATEST message
            if (msg.getMessageId() <= this.remoteControlMessageId)
                return;
            
            this.remoteControlMessageId = msg.getMessageId();
            
            if (msg.getOp() == Operation.OPEN || msg.getOp() == Operation.PING || msg.getOp() == Operation.PONG)
            {
                // Repurposed operation. This message will carry config for us to connect to.
                
                String string = StringUtil.toUtfString(msg.getData());
                
                if (this.remoteCfg != null && !this.remoteCfg.toString().equals(string))
                {
                    this.remoteCfg = new ConfigurationImpl(string);
                    
                    this.logger.trace("Got remote configuration: [{}]", this.remoteCfg);
                    
                    String cRemoteId = this.remoteCfg.getOption(KEY_SENDER_ID);
                    if (cRemoteId == null)
                    {
                        this.sendError(ERR_NO_ID);
                        return;
                    }
                    
                    if (this.trapID.equals(cRemoteId))
                    {
                        // Reject the peer
                        this.sendError(ERR_DUPLICATE_ID);
                        return;
                    }
                    
                    if (this.remoteId == null)
                        this.remoteId = cRemoteId;
                    
                    if (!cRemoteId.equals(this.remoteId))
                    {
                        // Reject the peer
                        this.sendError(ERR_WRONG_ID);
                        return;
                    }
                    
                    this.configure(string);
                    this.reconnectTransportsAbovePriority(Long.MIN_VALUE);
                    
                    // This reinstates max active transports once any peer to peer transport has been established.
                    // TODO: Make this setting take effect immediately.
                    //if (this.remoteId.compareTo(this.trapID) > 0)
                    //    this.setMaxActiveTransports(1);
                    
                }
                
                if (msg.getOp() == Operation.PING)
                    this.sendConfig(Operation.PONG);
            }
            
            if (msg.getOp() == Operation.TRANSPORT)
            {
                String transportName = msg.getAuthData();
                this.getTransport(transportName).receiveTransportedMessage(new TrapMessageImpl(msg.getData()));
            }
            
        }
        catch (IOException e)
        {
        	logger.error("IOException while receiving data", e);
        }
        catch (TrapException e)
        {
        	logger.error("Trap Exception while receiving data", e);
        }
        
    }
    
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Never used; only present to reuse code.
    }
    
    static class PeerInfo
    {
        public WeakReference<TrapTransport> transport;
        boolean                             locallyInitiated = false;
        
        public PeerInfo(TrapTransport t, boolean locallyInitiated)
        {
            this.transport = new WeakReference<TrapTransport>(t);
            this.locallyInitiated = locallyInitiated;
        }
    }
    
    public void reconnectTransportsAbovePriority(long minPriority)
    {
        
        LinkedList<TrapTransport> candidates = new LinkedList<TrapTransport>();
        
        // Iterate over transports; get all transports above target priority that are disconnected
        synchronized (this.transports)
        {
            for (TrapTransport t : this.transports)
            {
                if (t.canConnect() && t.isEnabled())
                {
                    if (t.getTransportPriority() >= minPriority)
                    {
                        if (t.getState() == TrapTransportState.DISCONNECTED || t.getState() == TrapTransportState.ERROR)
                        {
                            candidates.add(t);
                        }
                    }
                }
            }
        }
        
        // Now we have a list of potentials.
        
        for (TrapTransport t : candidates)
        {
            
            PeerInfo pi = (PeerInfo) t.getContext();
            t.init();
            t.setTransportDelegate(this, pi);
            t.setConfiguration(this.config);
            t.setFormat(this.getTrapFormat());
            this.activeTransports.add(t);
            
            try
            {
                t.connect();
                this.activeTransports.add(t);
            }
            catch (TrapException e1)
            {
            }
        }
        
    }
    
    public synchronized void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
    {
        // Super will manage available transports. All we need to consider is what action to take.
        super.ttStateChanged(newState, oldState, transport, context);
        
        // Always remove from active, to clear any confusion... 
        if (newState == TrapTransportState.DISCONNECTING || newState == TrapTransportState.DISCONNECTED || newState == TrapTransportState.ERROR)
        {
            this.availableTransports.remove(transport);
            this.activeTransports.remove(transport);
        }
        
        // Don't trigger any recoveries if we've asked to close.
        if ((this.getState() == TrapState.CLOSED) || (this.getState() == TrapState.CLOSING) || (this.getState() == TrapState.ERROR))
            return;
        
        // Cases we need to cover. A transport was just lost that needs to recover.
        // Alternatively, lost all transports and we need to take action
        
        // What to do if we lose a transport
        if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
        {
            
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
                
                if (this.getState() == TrapState.OPENING)
                {
                    if (this.activeTransports.size() != 0)
                        return; // More transports to go
                        
                    // The only active transport (left) failed when trying to connect.
                    this.sendError(ERR_TRANSPORTS_FAILED);
                    
                    // We will not change our state (yet)
                    // It is up to the other side to communicate back to us that they failed too.
                    // If all else fails, the general connect timer will kill us.
                    return;
                }
                else
                {
                    
                    if (this.activeTransports.size() != 0)
                    {
                        // We can just reconnect transports (if applicable)
                        TrapTransport head = this.availableTransports.iterator().next();
                        long priority = head != null ? head.getTransportPriority() : Long.MIN_VALUE;
                        this.reconnectTransportsAbovePriority(priority);
                        return;
                    }
                    
                    // Okay. We have no transports, and the last one died. What are we doing?
                    this.setState(TrapState.SLEEPING);
                    
                    // Adjust reconnect timeout
                    this.canReconnectUntil = System.currentTimeMillis() + this.reconnectTimeout;
                    
                    // We are sleeping. Ruh-roh.
                    this.sendConfig(Operation.PING);
                    
                }
            }
            else if (oldState == TrapTransportState.CONNECTING)
            {
                if (this.activeTransports.size() != 0)
                    return; // More transports to go
                    
                // The only active transport (left) failed when trying to connect.
                this.sendError(ERR_TRANSPORTS_FAILED);
            }
            else
            {
                // disconnecting, so do nothing
                
                if ((this.getState() == TrapState.OPEN) || (this.getState() == TrapState.SLEEPING))
                {
                    if (this.activeTransports.size() == 0)
                    {
                        this.setState(TrapState.SLEEPING);
                        this.sendConfig(Operation.PING);
                    }
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
                
                TrapMessage m = this.createMessage().setOp(Operation.OPEN);
                TrapConfigurationImpl body = new TrapConfigurationImpl();
                
                body.setOption(TrapConstants.ENDPOINT_ID_CLIENT, this.trapID);
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
                }
            }
            else
            {
                this.logger.error("Reached TrapTransportState.CONNECTED from a non-CONNECTING state. We don't believe in this.");
            }
        }
    }
    
    @Override
    public void enableTransport(String transportName) throws TrapException
    {
        super.enableTransport(transportName);
        this.listener.enableTransport(transportName);
    }
    
    @Override
    public void disableTransport(String transportName)
    {
        super.disableTransport(transportName);
        this.listener.disableTransport(transportName);
    }
    
    @Override
    public void disableAllTransports()
    {
        super.disableAllTransports();
        this.listener.disableAllTransports();
    }
    
    @Override
    public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
    {
        TrapMessage cm = this.createControlMessage();
        cm.setOp(Operation.TRANSPORT);
        cm.setAuthData(transport.getTransportName());
        try
        {
            cm.setData(message.serialize());
            this.channel.sendToRemote(cm.serialize());
        }
        catch (IOException e)
        {
        	logger.error("Exception while trying to communicate with remote peer: {}", e, e);
        }
    }
    
}
