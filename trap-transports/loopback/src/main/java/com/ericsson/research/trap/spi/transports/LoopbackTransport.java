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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;

public class LoopbackTransport extends AbstractTransport implements ListenerTrapTransport
{
    
    public static final ConcurrentHashMap<String, WeakReference<LoopbackTransport>> transports           = new ConcurrentHashMap<String, WeakReference<LoopbackTransport>>();
    public static final String                                                      CONFIG_KEY_REMOTE_ID = "loopback-remote-id";
    
    protected String                                                                id;
    protected Object                                                                sContext;
    protected ListenerTrapTransportDelegate                                         sListener;
    protected WeakReference<LoopbackTransport>                                      remote;
    
    public LoopbackTransport()
    {
        super();
        this.id = UUID.randomUUID().toString();
        this.transportPriority = -1000;
        this.enabled = false; // Start with disabled mode. Config can re-enable. Prevents us from disturbing other tests.
    }
    
    public void init()
    {
        super.init();
        this.remote = null;
    }
    
    public String getTransportName()
    {
        return "loopback";
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.FUNCTION;
    }
    
    @Override
    public synchronized void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        // We can just forward the message to the proper destination
        
        LoopbackTransport t = this.remote.get();
        
        if (t == null)
        {
            this.setState(TrapTransportState.ERROR);
            throw new TrapTransportException(message, this.state);
        }
        
        t._receive(message);
    }
    
    @Override
    public boolean canConnect()
    {
        return true;
    }
    
    @Override
    protected boolean onMessage(TrapMessage message)
    {
        // Again shortcut authentication checking, earning us a lot of CPU time.
        return true;
    }
    
    @Override
    public boolean canListen()
    {
        return true;
    }
    
    @Override
    protected boolean isServerConfigured()
    {
        return true;
    }
    
    @Override
    protected boolean isClientConfigured()
    {
        return this.getOption(CONFIG_KEY_REMOTE_ID) != null;
    }
    
    @Override
    public synchronized void internalConnect() throws TrapException
    {
        // Find the remote id.
        String remoteId = this.getOption(CONFIG_KEY_REMOTE_ID);
        
        WeakReference<LoopbackTransport> tRef = transports.get(remoteId);
        
        if (tRef == null)
        {
            this.logger.trace("Remote endpoint invalid (probably not the same machine)");
            this.configure(OPTION_ENABLED, "false");
            this.setState(TrapTransportState.DISCONNECTED);
            return;
        }
        
        LoopbackTransport t = tRef.get();
        
        if (t == null)
        {
            this.setState(TrapTransportState.ERROR);
            return;
        }
        
        this.remote = new WeakReference<LoopbackTransport>(t._connect(this));
        this.setState(TrapTransportState.CONNECTED);
    }
    
    public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
    {
        if (!this.enabled)
            throw new TrapException("Not Enabled!");
        
        this.sListener = listener;
        this.sContext = context;
        transports.put(this.id, new WeakReference<LoopbackTransport>(this));
    }
    
    public void getClientConfiguration(TrapConfiguration destination, String hostname)
    {
        destination.setOption(this.prefix + "." + CONFIG_KEY_REMOTE_ID, this.id);
    }
    
    /**
     * Connects a "remote" transport to this one. Called by the remote transport's connect method. This one is
     * (presumably) a listener. It spawns a new LoopbackTransport, notifies its listener, then returns the new transport
     * to the remote side. They form a double link and can send messages back and forth.
     * 
     * @param remote
     * @return
     */
    protected LoopbackTransport _connect(LoopbackTransport remote)
    {
        LoopbackTransport local = new LoopbackTransport();
        local.remote = new WeakReference<LoopbackTransport>(remote);
        
        // Optional: configure
        
        // Now notify
        this.sListener.ttsIncomingConnection(local, this, this.sContext);
        local.setState(TrapTransportState.CONNECTED);
        
        // Now allow the remote party to benefit.
        return local;
    }
    
    /**
     * Called from the remote transport when we need to receive a message.
     * 
     * @param message
     */
    protected void _receive(TrapMessage message)
    {
        this.receiveMessage(message);
    }
    
    @Override
    protected void internalDisconnect()
    {
        if (this.remote != null)
        {
            LoopbackTransport t = this.remote.get();
            
            if (t != null)
                t._close();
            
        }
        
        transports.remove(this.id);
        this.setState(TrapTransportState.DISCONNECTED);
    }
    
    protected void _close()
    {
        transports.remove(this.id);
        this.setState(TrapTransportState.DISCONNECTED);
    }
    
    public void fillAuthenticationKeys(@SuppressWarnings("rawtypes") HashSet keys)
    {
    }
    
    public void fillContext()
    {
    }
    
    //Override parent's send and stop sending authdata... This prevents useless serialization of messages.
    public void send(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        if ((this.state != TrapTransportState.AVAILABLE) && (this.state != TrapTransportState.CONNECTED))
            throw new TrapTransportException(message, this.state);
        
        if (this.logger.isTraceEnabled())
            this.logger.trace("Sending {}/{} on transport {} for {}.", new Object[] { message.getOp(), message.getMessageId(), this, this.delegate });
        
        this.internalSend(message, expectMore);
    }
    
    /*
     * Disable transit messages
     *
     * (non-Javadoc)
     * @see com.ericsson.research.trap.spi.transports.AbstractTransport#addTransitMessage(com.ericsson.research.trap.spi.TrapMessage)
     */
    protected void addTransitMessage(TrapMessage m)
    {
    }
    
    protected void acknowledgeTransitMessage(TrapMessage message)
    {
    }
    
    public boolean isObjectTransport()
    {
        return true;
    }
    
    public void flushTransport()
    {
        // Loopback has no need to flush.
    }
    
    @Override
    protected void setState(TrapTransportState newState)
    {
        
        //if (newState == TrapTransportState.ERROR)
        //    System.out.println("Debugging");
        
        super.setState(newState);
    }
    
}
