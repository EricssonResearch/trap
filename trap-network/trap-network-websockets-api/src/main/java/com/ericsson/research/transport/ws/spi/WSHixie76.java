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

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSURI;

public class WSHixie76 extends WSHixie75
{
    
    protected final static Timer t = new Timer();
    
    protected TimerTask          task;
    
    public WSHixie76(WSURI uri, WSSecurityContext securityContext)
    {
        super(uri, securityContext);
    }
    
    public WSHixie76(WSSecurityContext securityContext)
    {
        super(securityContext);
    }
    
    protected WSAbstractHandshake getHandshake()
    {
        if (this.handshake == null)
            this.handshake = new WSHixie76Handshake(this);
        return this.handshake;
    }
    
    public void close()
    {
        synchronized (this.wrapper)
        {
            if ((this.state == CLOSED) || (this.state == CLOSING))
                return;
            this.state = CLOSING;
            try
            {
                this.task = new TimerTask() {
                    public void run()
                    {
                        WSHixie76.this.forceClose();
                    }
                };
                t.schedule(this.task, CLOSE_TIMEOUT);
                this.internalClose();
            }
            catch (Exception e)
            {
                this.forceClose();
            }
        }
    }
    
    public void notifyDisconnected()
    {
        synchronized (this.wrapper)
        {
            if (this.task != null)
            {
                this.task.cancel();
                this.task = null;
            }
            super.notifyDisconnected();
        }
    }
    
    protected void dispatchFrame(WSAbstractFrame frame) throws IOException, WSException
    {
        switch (frame.getType())
        {
            case WSAbstractFrame.CLOSE_FRAME:
                synchronized (this)
                {
                    switch (this.state)
                    {
                        case OPEN:
                            this.close();
                            break;
                        case CLOSING:
                            this.forceClose();
                            break;
                            
                            default:
                                throw new WSException("Invalid state");
                    }
                }
                break;
            default:
                super.dispatchFrame(frame);
        }
    }
    
    protected void internalClose() throws IOException
    {
        new WSHixieFrame(WSAbstractFrame.CLOSE_FRAME, null).serialize(this.getRawOutput());
    }
    
    public String toString()
    {
        return "Hixie-76";
    }
    
}