package com.ericsson.research.trap;

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

import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.transports.AbstractTransport;

public class TestTransport1 extends AbstractTransport
{
    
    public String name = "test1";
    public String protocol;
    public boolean canConnect;
    public boolean canListen;
    
    public TestTransport1(String name, String protocol, boolean connect, boolean listen)
    {
        this.name = name;
        this.protocol = protocol;
        this.canConnect = connect;
        this.canListen = listen;
    }
    
    @Override
    public boolean canConnect()
    {
        return this.canConnect;
    }

    @Override
    public boolean canListen()
    {
        return this.canListen;
    }

    public String getTransportName()
    {
        return this.name != null? this.name:"test1";
    }
    
    public void flushTransport()
    {
        
    }
    
    @Override
    protected String getProtocolName()
    {
        return this.protocol;
    }
    
    @Override
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        
    }
    
    @Override
    protected void internalConnect() throws TrapException
    {
        
    }
    
    @Override
    protected void internalDisconnect()
    {
        
    }
    
}
