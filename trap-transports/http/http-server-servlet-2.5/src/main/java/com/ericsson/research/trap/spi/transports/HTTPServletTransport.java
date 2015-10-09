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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.http.HTTPHandler;
import com.ericsson.research.trap.impl.http.HTTPSession;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.ThreadPool;

public class HTTPServletTransport extends AbstractTransport implements HTTPHandler
{
    
    // Prevent the listener from being GC'd while the transport is active. 
    @SuppressWarnings("unused")
    private HTTPServletListener      listener;
    @SuppressWarnings("unused")
    private String                   id;
    
    private final Object             sendQueueLock         = new Object();
    LinkedBlockingQueue<TrapMessage> messagesToSend        = new LinkedBlockingQueue<TrapMessage>();
    LinkedBlockingQueue<TrapMessage> flushedMessages       = new LinkedBlockingQueue<TrapMessage>();
    private TrapTransportState       oldState;
    private final Object             sendLock              = new Object();
    private final Object             receiveLock           = new Object();
    private HTTPSession              longpoll;
    private HTTPReaper               reaper                = new HTTPReaper();
    long                             expirationDelay       = 28000;                                 // Almost 30 seconds
    long                             reregistrationTimeout = 10 * 1000;                             // Ten seconds reregistration timeout
                                                                                                     
    public HTTPServletTransport(HTTPServletListener listener, String id)
    {
        this.listener = listener;
        this.id = id;
        this.state = TrapTransportState.CONNECTED;
        ThreadPool.executeAfter(this.reaper, this.expirationDelay + this.reregistrationTimeout);
    }
    
    @Override
    public String getTransportName()
    {
        return "http";
    }
    
    @Override
    protected String getProtocolName()
    {
        return TrapTransportProtocol.HTTP;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void fillAuthenticationKeys(@SuppressWarnings("rawtypes") HashSet keys)
    {
        super.fillAuthenticationKeys(keys);
    }
    
    @Override
    protected void updateConfig()
    {
        super.updateConfig();
        
        String newReregistrationTimeout = this.getOption("reregistrationTimeout");
        try
        {
            this.reregistrationTimeout = Long.parseLong(newReregistrationTimeout);
        }
        catch (Exception e)
        {
        }
        
        String newexpirationDelay = this.getOption("expirationDelay");
        try
        {
            this.expirationDelay = Long.parseLong(newexpirationDelay);
        }
        catch (Exception e)
        {
        }
    }
    
    @Override
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        
        synchronized (this.sendLock)
        {
            if ((this.getState() != TrapTransportState.AVAILABLE) && (message.getMessageId() != 0))
                throw new TrapTransportException(message, this.getState());
            
            if (this.logger.isTraceEnabled())
                this.logger.trace("Queueing message id {} to send with HTTP; got more is [{}]", message.getMessageId(), expectMore);
            
            synchronized (this.sendQueueLock)
            {
                this.messagesToSend.add(message);
            }
            
            if (expectMore)
                return;
            
            this.flushTransport();
            
        }
        
    }
    
    public void flushTransport()
    {
        synchronized (this.sendLock)
        {
            
            // kick the sending thread
            if (this.getState() == TrapTransportState.AVAILABLE)
            {
                this.oldState = this.getState();
                this.setState(TrapTransportState.UNAVAILABLE);
            }
            
            this.finishLongpoll(false);
        }
    }
    
    @Override
    protected void internalConnect() throws TrapException
    {
        throw new TrapException("Cannot connect a servlet transport");
    }
    
    @Override
    protected void internalDisconnect()
    {
        this.mDisconnect();
    }
    
    @Override
    public void forceError()
    {
        this.mDisconnect();
        super.forceError();
    }
    
    @Override
    protected void setState(TrapTransportState newState)
    {
        super.setState(newState);
    }
    
    public void handle(final HTTPSession session)
    {
        String method = session.request().getMethod();
        this.lastAlive = System.currentTimeMillis();
        
        if ("POST".equalsIgnoreCase(method))
        {
            try
            {
                synchronized (this.receiveLock)
                {
                    // RECEIVE data from client.
                    ServletInputStream is = session.request().getInputStream();
                    
                    // We can read chunks of 4096 bytes and hand it over to parsing. This reduces the size of temp buffers.
                    int read = 0;
                    int received = 0;
                    byte[] buf = new byte[4096];
                    while ((read = is.read(buf)) > -1)
                    {
                        received++;
                        this.receive(buf, 0, read);
                    }
                    
                    if (received == 0)
                    {
                        // HACK: Disconnect
                        this.mDisconnect();
                    }
                    
                    session.response().setStatus(204);
                    session.response().setContentLength(0);
                    session.response().flushBuffer();
                    session.finish();
                    
                }
            }
            catch (Exception e)
            {
                this.logger.error("Exception during receiving of a message", e);
                session.response().setStatus(500);
                session.finish();
            }
        }
        else if ("GET".equalsIgnoreCase(method))
        {
            // This is a longpoll! Better synchronize
            synchronized (this.sendLock)
            {
                
                if (this.longpoll != null)
                {
                    
                    if (this.longpoll == session)
                    {
                        // TODO: This may be an error.
                        this.logger.error("If you see this message, report as bug, kkthx");
                    }
                    else
                    {
                        this.logger.warn("Received long poll while old poll wasn't null. Oops...");
                        this.finishLongpoll(true);
                    }
                }
                
                if (this.messagesToSend.size() > 0)
                {
                    this.longpoll = session;
                    this.finishLongpoll(false);
                    return;
                }
                
                if (this.getState() == TrapTransportState.DISCONNECTED || this.getState() == TrapTransportState.ERROR)
                {
                    session.response().setStatus(404);
                    try
                    {
                        session.response().flushBuffer();
                    }
                    catch (IOException e)
                    {
                    }
                    session.finish();
                    return;
                }
                
                this.longpoll = session;
                
                if (this.getState() == TrapTransportState.UNAVAILABLE)
                    this.setState(this.oldState);
                else if (this.getState() == TrapTransportState.CONNECTING)
                    this.setState(TrapTransportState.CONNECTED);
                
                ThreadPool.executeAfter(new Runnable() {
                    
                    @Override
                    public void run()
                    {
                        synchronized (HTTPServletTransport.this.sendLock)
                        {
                            // Wrap up the session, if not already done so.
                            if (!session.isFinished())
                            {
                                if (session == HTTPServletTransport.this.longpoll)
                                {
                                    HTTPServletTransport.this.finishLongpoll(true);
                                }
                                else
                                {
                                    session.response().setStatus(400);
                                    session.finish();
                                }
                            }
                        }
                    }
                }, this.reregistrationTimeout);
            }
        }
        else if ("DELETE".equalsIgnoreCase(method))
        {
            this.mDisconnect();
            session.response().setStatus(204);
            session.response().setContentLength(0);
            session.finish();
        }
        else if ("PUT".equalsIgnoreCase(method))
        {
            this.mDisconnect();
            session.response().setStatus(204);
            session.response().setContentLength(0);
            session.finish();
        }
        else if ("OPTIONS".equalsIgnoreCase(method))
        {
            session.response().setStatus(200);
            session.response().setContentLength(0);
            session.finish();
        }
    }
    
    void finishLongpoll(boolean timeout)
    {
        synchronized (this.sendLock)
        {
            if (this.longpoll == null)
                return;
            
            try
            {
                
                // Switch the queues around. This will allow the servlet to continue queueing messages
                // while we flush the latest batch. This should allow higher performance.
                synchronized (this.sendQueueLock)
                {
                    if (this.messagesToSend.size() > 0)
                    {
                        LinkedBlockingQueue<TrapMessage> flushing = this.messagesToSend;
                        this.messagesToSend = this.flushedMessages;
                        this.flushedMessages = flushing;
                    }
                }
                
                if (this.flushedMessages.size() > 0)
                {
                    
                    try
                    {
                        
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        
                        for (TrapMessage m : this.flushedMessages)
                        {
                            if (this.logger.isTraceEnabled())
                            {
                                this.logger.trace("Flushing message with id {}", m.getMessageId());
                            }
                            
                            bos.write(m.serialize());
                        }
                        
                        byte[] body = bos.toByteArray();
                        
                        this.longpoll.response().setStatus(201);
                        this.longpoll.response().setContentLength(body.length);
                        ServletOutputStream os = this.longpoll.response().getOutputStream();
                        os.write(body);
                        os.flush();
                        os.close();
                        this.longpoll.finish();
                    }
                    catch (Exception e)
                    {
                        this.logger.debug("Error during serialization of messages", e);
                        if (this.flushedMessages.size() > 0)
                        {
                            // Recombulate the flushing queue with the flushed, to make one coherent queue of failed messages.
                            synchronized (this.sendQueueLock)
                            {
                                this.flushedMessages.addAll(this.messagesToSend);
                                this.messagesToSend.clear();
                                LinkedBlockingQueue<TrapMessage> tmp = this.flushedMessages;
                                this.flushedMessages = this.messagesToSend;
                                this.messagesToSend = tmp;
                                
                                LinkedList<TrapMessage> failedMessages = new LinkedList<TrapMessage>();
                                
                                for (TrapMessage m : this.messagesToSend)
                                    if (m.getMessageId() != 0)
                                        failedMessages.add(m);
                                
                                this.delegate.ttMessagesFailedSending(failedMessages, this, this.delegateContext);
                            }
                        }
                        this.longpoll.response().setStatus(500);
                        this.longpoll.response().setContentLength(0);
                        this.longpoll.finish();
                    }
                    finally
                    {
                        this.flushedMessages.clear();
                    }
                }
                else
                {
                    if (timeout)
                    {
                        this.longpoll.response().setStatus(204);
                        this.longpoll.response().setContentLength(0);
                        this.longpoll.finish();
                    }
                }
            }
            finally
            {
                
                if (this.longpoll.isFinished())
                    this.longpoll = null;
                
                // Also flip to unavailable when this GET expires...
                if ((this.getState() == TrapTransportState.CONNECTED) || (this.getState() == TrapTransportState.AVAILABLE))
                {
                    this.oldState = this.getState();
                    this.setState(TrapTransportState.UNAVAILABLE);
                }
            }
            
        }
    }
    
    void mDisconnect()
    {
        
        if (this.state == TrapTransportState.DISCONNECTED || this.state == TrapTransportState.ERROR)
            return;
        
        this.setState(TrapTransportState.DISCONNECTING);
        this.setState(TrapTransportState.DISCONNECTED);
        
        this.finishLongpoll(true);
        
        synchronized (this.sendQueueLock)
        {
            if (this.messagesToSend.size() > 0)
            {
                this.delegate.ttMessagesFailedSending(this.messagesToSend, this, this.delegateContext);
            }
        }
    }
    
    @Override
    protected void acknowledgeTransitMessage(TrapMessage message)
    {
    }
    
    class HTTPReaper implements Runnable
    {
        
        @Override
        public void run()
        {
            if (HTTPServletTransport.this.longpoll == null)
            {
                long expiry = HTTPServletTransport.this.lastAlive + HTTPServletTransport.this.expirationDelay + HTTPServletTransport.this.reregistrationTimeout;
                
                if (System.currentTimeMillis() >= expiry)
                {
                    // This transport has disconnected.
                    HTTPServletTransport.this.mDisconnect();
                }
            }
        }
        
    }
    
}
