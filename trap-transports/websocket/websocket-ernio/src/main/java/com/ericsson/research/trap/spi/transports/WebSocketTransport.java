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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSURI;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;

public class WebSocketTransport extends AbstractTransport
{
    
    private static final String OPTION_BINARY = "binary";
    private WSInterface         socket;
    private boolean             binary        = true;
    private long                lastSend      = 0;
    private boolean             delayed       = false;
    private boolean             delayQueued   = false;
    private boolean             useDelay      = true;
    private WSListener          mListener     = null;
    
    // Create a new SocketTransport for connecting (=client)
    public WebSocketTransport()
    {
        this.transportPriority = 0;
    }
    
    // Create a new SocketTransport for receiving (=server)
    public WebSocketTransport(WSInterface socket)
    {
        this.socket = socket;
        this.createListener(socket);
        this.transportPriority = 0;
    }
    
    private synchronized void createListener(final WSInterface socket)
    {
        this.mListener = new WSListener() {
            
            @Override
            public void notifyPong(byte[] payload)
            {
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyPong(payload);
            }
            
            @Override
            public void notifyOpen(WSInterface socket)
            {
                
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyOpen(socket);
            }
            
            @Override
            public void notifyMessage(byte[] data)
            {
                
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyMessage(data);
            }
            
            @Override
            public void notifyMessage(String utf8String)
            {
                
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyMessage(utf8String);
            }
            
            @Override
            public void notifyError(Throwable t)
            {
                
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyError(t);
            }
            
            @Override
            public void notifyClose()
            {
                
                if (socket == WebSocketTransport.this.socket)
                    WebSocketTransport.this.notifyClose();
            }
        };
        
        socket.setReadListener(this.mListener);
    }
    
    public boolean canConnect()
    {
        return true;
    }
    
    public String getTransportName()
    {
        return "websocket";
    }
    
    @Override
    public void init()
    {
        super.init();
        if (this.socket != null)
        {
            this.socket.setReadListener(null);
            this.socket.close();
        }
        this.socket = null;
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.WEBSOCKET;
    }
    
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        
        if (this.logger.isTraceEnabled())
            this.logger.trace("Now sending message ({}) with id [{}]", message.getOp().toString(), message.getMessageId());
        
        try
        {
            WSInterface mSock = this.socket;
            
            if (mSock == null)
                throw new TrapTransportException(message, this.getState());
            
            synchronized (mSock)
            {
                
                byte[] raw = message.serialize();
                
                this.delayed |= this.lastSend == System.currentTimeMillis();
                this.delayed &= this.useDelay;
                if (this.delayed && !this.delayQueued)
                {
                    this.delayQueued = true;
                    ThreadPool.executeAfter(new Runnable() {
                        
                        @Override
                        public void run()
                        {
                            WebSocketTransport.this.flushTransport();
                        }
                    }, 1);
                }
                
                if (expectMore || this.delayed)
                {
                    
                    if (this.outBuf == null)
                        this.outBuf = new ByteArrayOutputStream();
                    
                    this.outBuf.write(raw);
                    return;
                }
                
                this.performSend(raw);
            }
        }
        catch (IOException e)
        {
            this.logger.debug(e.toString());
            // Move to state ERROR and clean up
            this.setState(TrapTransportState.ERROR);
            if (this.socket != null)
                this.socket.close();
            throw new TrapTransportException(message, this.state);
        }
        catch (TrapTransportException e)
        {
            // Move to state ERROR (No socket, tried to send?)
            this.setState(TrapTransportState.ERROR);
            throw e;
        }
        catch (NullPointerException e)
        {
            this.logger.debug(e.toString());
            // Move to state ERROR and clean up
            this.setState(TrapTransportState.ERROR);
            if (this.socket != null)
                this.socket.close();
            throw new TrapTransportException(message, this.state);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
    
    private void performSend(byte[] raw) throws IOException
    {
        WSInterface mSock = this.socket;
        if (this.outBuf != null)
        {
            this.outBuf.write(raw);
            raw = this.outBuf.toByteArray();
            this.outBuf = null;
        }
        //char[] encoded = Base64.encode(raw);
        
        if (this.binary)
            mSock.send(raw);
        else
            mSock.send(StringUtil.toUtfString(raw));
        
        this.lastSend = System.currentTimeMillis();
        this.delayed = this.delayQueued = false;
    }
    
    @Override
    public void flushTransport()
    {
        synchronized (WebSocketTransport.this.socket)
        {
            try
            {
                if (this.outBuf != null)
                {
                    byte[] raw = WebSocketTransport.this.outBuf.toByteArray();
                    WebSocketTransport.this.outBuf = null;
                    
                    if (raw.length > 0)
                        WebSocketTransport.this.performSend(raw);
                }
            }
            catch (IOException e)
            {
                WebSocketTransport.this.logger.debug(e.toString());
                WebSocketTransport.this.forceError();
            }
        }
    }
    
    @Override
    protected boolean isClientConfigured()
    {
        String uriStr = this.getOption(WebSocketConstants.CONFIG_URI);
        
        if (uriStr == null)
            return false;
        
        try
        {
            new WSURI(uriStr);
            return true;
        }
        catch (Exception e)
        {
        }
        return false;
    }
    
    @Override
    protected void internalConnect() throws TrapException
    {
        synchronized (this)
        {
            if (this.socket != null)
                throw new TrapException("Cannot re-connect transport");
            
            this.outBuf = null;
            
            String uri = this.getOption(WebSocketConstants.CONFIG_URI);
            
            try
            {
                
                WSSecurityContext sc = null;
                if (this.getBooleanOption(TrapTransport.CERT_IGNORE_INVALID, false))
                    sc = new WSSecurityContext(SSLUtil.getInsecure());
                
                this.socket = WSFactory.createWebSocketClient(new WSURI(uri), null, WSFactory.VERSION_RFC_6455, sc);
                this.createListener(this.socket);
                this.socket.open();
            }
            catch (Exception e)
            {
                throw new TrapException(e);
            }
        }
    }
    
    @Override
    protected void internalDisconnect()
    {
        if (this.socket == null)
            return;
        
        synchronized (this)
        {
            if ((this.getState() != TrapTransportState.DISCONNECTED) && (this.getState() != TrapTransportState.DISCONNECTED) && (this.getState() != TrapTransportState.ERROR))
                this.setState(TrapTransportState.DISCONNECTING);
        }
        try
        {
            synchronized (this.socket)
            {
                this.socket.close();
                this.socket.setReadListener(null);
            }
            
        }
        catch (Exception e)
        {
            synchronized (this)
            {
                // TODO: Gracefully do something
                if (this.getState() != TrapTransportState.DISCONNECTED)
                    this.setState(TrapTransportState.ERROR);
            }
        }
        finally
        {
        }
    }
    
    public void notifyError()
    {
        this.setState(TrapTransportState.ERROR);
        
        if (this.socket != null)
            this.internalDisconnect();
    }
    
    public void notifyOpen(WSInterface socket)
    {
        this.fillContext(this.contextMap, this.contextKeys);
        this.setState(TrapTransportState.CONNECTED);
    }
    
    public synchronized void notifyClose()
    {
        if (this.getState() != TrapTransportState.ERROR)
            this.setState(TrapTransportState.DISCONNECTED);
        
        if (this.socket != null)
            this.socket.setReadListener(null);
        this.socket = null;
        
    }
    
    public void notifyMessage(String string)
    {
        //byte[] decoded = Base64.decode(string);
        
        // Disable binary mode, to prevent us from confusing the browser
        this.binary = false;
        
        byte[] decoded = StringUtil.toUtfBytes(string);
        this.receive(decoded, 0, decoded.length);
    }
    
    public void notifyMessage(byte[] data)
    {
        // Ensure binary mode is activated for correct response
        this.binary = true;
        this.receive(data, 0, data.length);
    }
    
    // TODO: Expose IP information on websocket level...
    @Override
    public void fillAuthenticationKeys(HashSet<String> keys)
    {
        super.fillAuthenticationKeys(keys);
        keys.add(TrapContextKeys.LocalIP);
        keys.add(TrapContextKeys.RemoteIP);
        keys.add(TrapContextKeys.LocalPort);
        keys.add(TrapContextKeys.RemotePort);
    }
    
    @Override
    public void fillContext(Map<String, Object> context, Collection<String> filter)
    {
        super.fillContext(context, filter);
        
        if (filter.contains(TrapContextKeys.LocalIP))
            context.put(TrapContextKeys.LocalIP, this.socket.getLocalSocketAddress().getAddress().getHostAddress());
        
        if (filter.contains(TrapContextKeys.LocalPort))
            context.put(TrapContextKeys.LocalPort, this.socket.getLocalSocketAddress().getPort());
        
        if (filter.contains(TrapContextKeys.RemoteIP))
            context.put(TrapContextKeys.RemoteIP, this.socket.getRemoteSocketAddress().getAddress().getHostAddress());
        
        if (filter.contains(TrapContextKeys.RemotePort))
            context.put(TrapContextKeys.RemotePort, this.socket.getRemoteSocketAddress().getPort());
        
    }
    
    public void notifyPong(byte[] payload)
    {
        // TODO: Change keepalives to use WebSockets?
        
    }
    
    public void notifyError(Throwable t)
    {
        this.logger.error("WebSocket Error", t);
        if (this.socket != null)
        {
            this.socket.close();
            this.socket.setReadListener(null);
        }
        this.socket = null;
        this.setState(TrapTransportState.ERROR);
    }
    
    protected void updateConfig()
    {
        
        String eString = this.getOption(OPTION_BINARY);
        if (eString != null)
        {
            try
            {
                this.binary = Boolean.parseBoolean(eString);
            }
            catch (Exception e)
            {
                this.logger.warn("Failed to parse transport {} binary flag", this.getTransportName(), e);
            }
        }
        
        super.updateConfig();
    }
    
}
