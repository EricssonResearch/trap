package com.ericsson.research.transport.ssl;

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
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.NioEndpoint;

public class SSLSocket extends ManagedSocket implements Runnable, NioEndpoint
{
    
    private final SSLEngine  engine;
    private final Executor   executor          = Executors.newSingleThreadExecutor();
    
    private static final int growSize          = 32 * 1024;
    private static final int bufSize           = 32 * 1024;
    
    private Object           readLock          = new Object();
    private Object           writeLock         = new Object();
    
    private ByteBuffer       netReadBuf        = ByteBuffer.allocate(bufSize);
    private ByteBuffer       sslNetReadBuf     = ByteBuffer.allocate(bufSize);
    private ByteBuffer       netWriteBuf       = ByteBuffer.allocate(bufSize);
    
    private ByteBuffer       clientReadBuf     = ByteBuffer.allocate(bufSize);
    private ByteBuffer       clientWriteBuf    = ByteBuffer.allocate(bufSize);
    private ByteBuffer       sslClientWriteBuf = ByteBuffer.allocate(bufSize);
    
    private SSLEngineResult  lastResult        = null;
    
    private boolean          needsWrap         = false;
    private boolean          needsUnwrap       = false;
    private boolean          needsDisconnect   = false;
    
    private int              errors     = 0;
    private static final int MAX_ERRORS = 10;
    
    public synchronized void disconnect()
    {
        this.needsDisconnect = true;
        
        try
        {
            this.executor.execute(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public SSLSocket(SSLContext sslc)
    {
        this(sslc, true);
    }
    
    public SSLSocket(SSLContext sslc, boolean clientMode)
    {
        super(!clientMode);
        this.engine = sslc.createSSLEngine();
        this.engine.setUseClientMode(clientMode);
        String[] procols = { "TLSv1" };
        this.engine.setEnabledProtocols(procols);
    }
    
    ByteBuffer grow(ByteBuffer src, int size)
    {
        src.flip();
        ByteBuffer b1 = ByteBuffer.allocate(src.capacity() + size);
        b1.put(src);
        return b1;
    }
    
    public void receive(byte[] data, int size)
    {
        
        if (super.getState() == State.NOT_CONNECTED)
            return;
        byte[] mData = new byte[size];
        System.arraycopy(data, 0, mData, 0, size);
        
        synchronized (this.readLock)
        {
            if (this.netReadBuf.remaining() < size)
            {
                // Grow the buffer
                this.netReadBuf = this.grow(this.netReadBuf, Math.max(growSize, size));
                
            }
            
            this.netReadBuf.put(mData, 0, size);
        }
        synchronized (this)
        {
            this.needsUnwrap = true;
            this.executor.execute(this);
        }
    }
    
    public void write(byte[] data) throws IOException
    {
        this.write(data, data.length);
    }
    
    @Override
    public void write(byte[] data, int size) throws IOException
    {
        if (super.getState() == State.NOT_CONNECTED)
            return;
        if (data.length < size)
            throw new IOException("Data size may not exceed array length; array length was " + data.length + " and copy length requested was " + size);
        
        synchronized (this.writeLock)
        {
            if (this.clientWriteBuf.position() + size >= this.clientWriteBuf.limit())
            {
                // Grow the buffer
                this.clientWriteBuf = this.grow(this.clientWriteBuf, Math.max(growSize, size));
            }
            
            this.clientWriteBuf.put(data, 0, size);
        }
        
        synchronized (this)
        {
            this.needsWrap = true;
            this.executor.execute(this);
        }
    }
    
    public synchronized void run()
    {
        try
        {
            // Run loop that checks last status and runs through the buffers, wrapping and unwrapping as necessary.
            if (this.lastResult == null)
            {
                this.engine.beginHandshake();
                HandshakeStatus handshakeStatus = this.engine.getHandshakeStatus();
                
                if (handshakeStatus == HandshakeStatus.NEED_UNWRAP)
                    this.needsUnwrap = true;
                else
                    this.needsWrap = true;
            }
            
            if (this.needsUnwrap)
                this.unwrap();
            
            if (this.needsWrap)
                this.wrap();
            
            if (this.needsDisconnect)
            {
                if (!this.needsWrap) // If we still need to wrap (send) something, don't lose data...
                    super.disconnect();
            }
            
            this.errors = 0;
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.errors++;
            
            // Be a little forgiving. This might fix an issue with WSS and Safari/Chrome
            if (this.errors > MAX_ERRORS)
            {
                this.needsWrap = false;
                this.needsUnwrap = false;
                super.disconnect();
            }
        }
        
    }
    
    void unwrap() throws SSLException
    {
        if (this.sslNetReadBuf.position() <= 0 && this.sslNetReadBuf.remaining() == this.sslNetReadBuf.capacity()) // Empty buffer
        {
            synchronized (this.readLock)
            {
                ByteBuffer b = this.netReadBuf;
                this.netReadBuf = this.sslNetReadBuf;
                this.sslNetReadBuf = b;
                if (this.sslNetReadBuf.position() <= 0 && (this.lastResult == null || this.lastResult.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP))
                    return;
            }
        }
        this.sslNetReadBuf.flip();
        this.lastResult = this.engine.unwrap(this.sslNetReadBuf, this.clientReadBuf);
        this.sslNetReadBuf.position(this.lastResult.bytesConsumed());
        this.sslNetReadBuf.compact();
        Status status = this.lastResult.getStatus();
        
        if (status == Status.CLOSED)
            return;
        
        if (status == Status.BUFFER_UNDERFLOW)
        {
            if (this.sslNetReadBuf.position() > 0 && this.netReadBuf.position() > 0)
            {
                // Consolidate the two buffers (in case a message is split up between them)		
                synchronized (this.netReadBuf)
                {
                    
                    this.netReadBuf.flip();
                    if (this.sslNetReadBuf.remaining() < this.netReadBuf.limit())
                    {
                        // Grow the buffer
                        this.sslNetReadBuf = this.grow(this.sslNetReadBuf, Math.max(growSize, this.netReadBuf.limit()));
                        
                    }
                    
                    this.sslNetReadBuf.put(this.netReadBuf);
                    this.netReadBuf.clear();
                }
                
                // Attempt to unwrap using the consolidated buffer.
                this.executor.execute(this);
                
            }
            return; // We'll read more data later
        }
        
        if (status == Status.BUFFER_OVERFLOW)
        {
            // Grow the target buffer
            this.clientReadBuf = this.grow(this.clientReadBuf, growSize);
            this.unwrap(); // Execute again
            return;
        }
        
        HandshakeStatus handshakeStatus = this.lastResult.getHandshakeStatus();
        
        if (handshakeStatus != HandshakeStatus.NEED_TASK && this.netReadBuf.position() == 0)
            this.needsUnwrap = false;
        else if (this.netReadBuf.position() > 0)
            this.executor.execute(this);
        
        if (this.clientReadBuf.position() > 0)
        {
            super.receive(this.clientReadBuf.array(), this.clientReadBuf.position());
            this.clientReadBuf.clear();
        }
        
        this.handleHandshakeStatus();
        
    }
    
    void wrap() throws IOException
    {
        
        if (this.sslClientWriteBuf.position() <= 0)
        {
            synchronized (this.writeLock)
            {
                ByteBuffer b = this.clientWriteBuf;
                this.clientWriteBuf = this.sslClientWriteBuf;
                this.sslClientWriteBuf = b;
                if (this.sslClientWriteBuf.position() <= 0 && (this.lastResult == null || this.lastResult.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP))
                    return;
            }
        }
        
        this.sslClientWriteBuf.flip();
        this.lastResult = this.engine.wrap(this.sslClientWriteBuf, this.netWriteBuf);
        this.sslClientWriteBuf.compact();
        Status status = this.lastResult.getStatus();
        
        if (status == Status.BUFFER_OVERFLOW)
        {
            // Grow target buffer
            this.netWriteBuf = this.grow(this.netWriteBuf, growSize);
            this.wrap();
            return;
        }
        
        if (status == Status.BUFFER_UNDERFLOW)
        // TODO: Does this ever happen?
        {
            return; // We'll read more data later
        }
        
        if (this.lastResult.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING && (this.sslClientWriteBuf.remaining() != this.sslClientWriteBuf.capacity() || this.clientWriteBuf.remaining() != this.clientWriteBuf.capacity()))
            this.executor.execute(this);
        else if (this.lastResult.getHandshakeStatus() != HandshakeStatus.NEED_TASK)
            this.needsWrap = false;
        
        if (this.netWriteBuf.position() > 0)
        {
            byte[] mData = new byte[this.netWriteBuf.position()];
            System.arraycopy(this.netWriteBuf.array(), 0, mData, 0, this.netWriteBuf.position());
            super.write(mData, mData.length);
            this.netWriteBuf.clear();
            
        }
        
        this.handleHandshakeStatus();
        
    }
    
    private void handleHandshakeStatus()
    {
        
        switch (this.lastResult.getHandshakeStatus())
        {
        
            case NEED_TASK:
                Runnable r;
                while ((r = this.engine.getDelegatedTask()) != null)
                    this.executor.execute(r);
                this.executor.execute(this);
                return;
                
            case NEED_WRAP:
                this.needsWrap = true;
                this.executor.execute(this);
                return;
                
            case NEED_UNWRAP:
                this.needsUnwrap = true;
                this.executor.execute(this);
                return;
                
            case FINISHED:
                if (this.sslClientWriteBuf.position() > 0 || this.clientWriteBuf.position() > 0)
                {
                    this.needsWrap = true;
                    this.executor.execute(this);
                }
                
                if (this.sslNetReadBuf.position() > 0 || this.netReadBuf.position() > 0)
                {
                    this.needsUnwrap = true;
                    this.executor.execute(this);
                }
                break;
            
            case NOT_HANDSHAKING:
                if (this.lastResult.getStatus() != Status.OK)
                    break;
                
                if (this.sslClientWriteBuf.position() > 0 || this.clientWriteBuf.position() > 0)
                {
                    this.needsWrap = true;
                    this.executor.execute(this);
                }
                
                if (this.sslNetReadBuf.position() > 0 || this.netReadBuf.position() > 0)
                {
                    this.needsUnwrap = true;
                    this.executor.execute(this);
                }
                
        }
        
    }
    
}
