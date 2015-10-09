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
import java.net.InetSocketAddress;

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSURI;

public class WSHixie75 extends WSAbstractProtocol implements WSConstants {
	
	public WSHixie75(WSURI uri, WSSecurityContext securityContext) {
		super(uri, securityContext);
	}
	
	protected WSAbstractHandshake getHandshake() {
		if(this.handshake == null)
			this.handshake = new WSHixie75Handshake(this);
		return this.handshake;
	}
	
	public WSHixie75(WSSecurityContext securityContext) {
		super(securityContext);
	}

	public void send(byte[] data) throws IOException {
		if (this.state != OPEN)
			throw new IOException(ILLEGAL_STATE+" "+this.getState());
		new WSHixieFrame(WSAbstractFrame.BINARY_FRAME, data).serialize(this.getRawOutput());
	}
	
	public void send(String utf8String) throws IOException {
		if (this.state != OPEN)
			throw new IOException(ILLEGAL_STATE+" "+this.getState());
		new WSHixieFrame(WSAbstractFrame.TEXT_FRAME, utf8String.getBytes(UTF_8)).serialize(this.getRawOutput());
	}

	protected WSAbstractFrame createEmptyFrame() {
		return new WSHixieFrame();
	}

	protected void dispatchFrame(WSAbstractFrame frame) throws IOException, WSException {
		switch (frame.getType()) {
			case WSAbstractFrame.TEXT_FRAME:
				this.wrapper.notifyMessage(new String(frame.getPayload(), UTF_8));
				break;
			case WSAbstractFrame.BINARY_FRAME:
				this.wrapper.notifyMessage(frame.getPayload());
				break;
			default:
				throw new WSException("Unknown frame type "+frame.getType());
		}
	}
	
	public String toString() {
		return "Hixie-75";
	}

    @Override
    public InetSocketAddress getLocalSocketAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }
	
}
