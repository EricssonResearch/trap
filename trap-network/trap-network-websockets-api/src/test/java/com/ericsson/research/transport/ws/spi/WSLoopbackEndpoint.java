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
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.ericsson.research.transport.ws.WSListener;

class WSLoopbackEndpoint extends WSAbstractProtocolWrapper {
	
	private WSAbstractProtocol server;
	private WSLoopbackEndpoint remote;
	private OutputStream output;
	private WSListener serverListener;
	
	public WSLoopbackEndpoint(WSAbstractProtocol client, WSListener clientListener, WSAbstractProtocol server, WSListener serverListener) {
		super(client, clientListener);
		this.server = server;
		this.serverListener = serverListener;
	}

	public WSLoopbackEndpoint(WSLoopbackEndpoint client, WSAbstractProtocol server, WSListener serverListener) {
		super(server, serverListener);
		this.remote = client;
	}
	
	public void open() throws IOException {
		this.remote = new WSLoopbackEndpoint(this, this.server, this.serverListener);
		super.open();
	}

	public OutputStream getRawOutput() throws IOException {
		if(this.output == null)
			this.output = new LoopbackOutputStream(this.remote);
		return this.output;
	}

	public void forceClose() {
		this.output = null;
		this.notifyDisconnected();
		if(this.remote!=null)
			this.remote.notifyDisconnected();
		this.remote = null;
		this.server = null;
	}
	
	public boolean isOpen() {
		return this.protocol.state == WSAbstractProtocol.OPEN;
	}

	public boolean isClosed() {
		return this.protocol.state == WSAbstractProtocol.CLOSED;
	}
	
	public String toString() {
		return "Loopback ("+this.protocol+")";
	}

    public InetSocketAddress getLocalSocketAddress()
    {
        return InetSocketAddress.createUnresolved("localhost", 0);
    }
    
    public InetSocketAddress getRemoteSocketAddress()
    {
        return InetSocketAddress.createUnresolved("localhost", 0);
    }
	
}