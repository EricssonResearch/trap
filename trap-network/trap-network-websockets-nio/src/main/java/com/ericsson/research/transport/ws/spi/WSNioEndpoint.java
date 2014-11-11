package com.ericsson.research.transport.ws.spi;

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
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.ManagedSocketClient;
import com.ericsson.research.transport.ws.WSListener;

public class WSNioEndpoint extends WSAbstractProtocolWrapper implements ManagedSocketClient
{
    
    private ManagedSocket   socket;
    private NioOutputStream os;
    private boolean         closing;
    
    public WSNioEndpoint(WSAbstractProtocol protocol, WSListener listener) throws IOException
    {
        super(protocol, listener);
        if (protocol.securityContext != null)
            this.socket = WSSecureSocketFactory.getSecureSocket(protocol.securityContext);
        else
            this.socket = new ManagedSocket();
        this.socket.registerClient(this);
    }
    
    public WSNioEndpoint(ManagedSocket socket, WSAbstractProtocol protocol, WSListener listener)
    {
        super(protocol, listener);
        if (socket == null)
            throw new IllegalArgumentException("Socket cannot be null");
        this.socket = socket;
        socket.registerClient(this);
    }
    
    public synchronized void open() throws IOException
    {
        this.closing = false;
        this.socket.connect(this.protocol.host, this.protocol.port);
    }
    
    class NioOutputStream extends ByteArrayOutputStream
    {
        
        private final ManagedSocket socket;
        
        public NioOutputStream(ManagedSocket socket)
        {
            this.socket = socket;
        }
        
        public synchronized void flush() throws IOException
        {
            if (this.count == 0)
                return;
            //System.out.println(PrettyPrinter.toHexString(buf, count));
            if (WSNioEndpoint.this.socket == null)
                throw new IOException("The socket has already been closed");
            if (this.socket.getState() != ManagedSocket.State.CONNECTED)
                return; // TODO: Should we throw instead?
            this.socket.write(this.buf, this.count);
            this.reset();
        }
        
        public synchronized void close() throws IOException
        {
            this.flush();
            super.close();
        }
        
    }
    
    public synchronized OutputStream getRawOutput() throws IOException
    {
        if (this.closing)
            throw new IOException("The socket is already closing");
        if (this.os == null)
            this.os = new NioOutputStream(this.socket);
        return this.os;
    }
    
    public synchronized void forceClose()
    {
        if (this.closing)
            return;
        this.closing = true;
        this.socket.disconnect();
        this.socket = null;
        this.os = null;
    }
    
    public String toString()
    {
        return "Nio (" + this.protocol + ")";
    }
    
    public InetSocketAddress getLocalSocketAddress()
    {
        return this.socket.getLocalSocketAddress();
    }
    
    public InetSocketAddress getRemoteSocketAddress()
    {
        return this.socket.getRemoteSocketAddress();
    }
    
}