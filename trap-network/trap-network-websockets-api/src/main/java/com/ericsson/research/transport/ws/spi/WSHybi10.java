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

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSURI;

public class WSHybi10 extends WSHixie76 {

	public WSHybi10(WSURI uri, WSSecurityContext securityContext) {
		super(uri, securityContext);
	}
	
	public WSHybi10(WSSecurityContext securityContext) {
		super(securityContext);
	}

	protected WSAbstractHandshake getHandshake() {
		if(handshake == null)
			handshake = new WSHybi10Handshake(this);
		return handshake;
	}
	
	public void send(byte[] data) throws IOException {
		new WSHybiFrame(WSHybiFrame.BINARY_FRAME, data, client).serialize(getRawOutput());
	}

	public void send(String utf8String) throws IOException {
		new WSHybiFrame(WSHybiFrame.TEXT_FRAME, utf8String.getBytes(UTF_8), client).serialize(getRawOutput());
	}

	public void ping(byte[] payload) throws IOException {
		if(state != OPEN)
			throw new IOException(ILLEGAL_STATE+" "+getState());
		new WSHybiFrame(WSHybiFrame.PING_FRAME, payload, client).serialize(getRawOutput());
	}
	
	protected void pong(byte[] payload) throws IOException {
		if(state != OPEN)
			throw new IOException(ILLEGAL_STATE+" "+getState());
		new WSHybiFrame(WSHybiFrame.PONG_FRAME, payload, client).serialize(getRawOutput());
	}
	

    
    public void close(int code, String reason) {
        
    };
	
	protected void internalClose() throws IOException {
		new WSHybiFrame(WSHybiFrame.CLOSE_FRAME, empty, client).serialize(getRawOutput());
	}
	
	protected WSAbstractFrame createEmptyFrame() {
		return new WSHybiFrame();
	}
	
	protected void dispatchFrame(WSAbstractFrame frame) throws IOException, WSException {
		switch (frame.getType()) {
			case WSHybiFrame.PING_FRAME:
				pong(frame.getPayload());
				break;
			case WSHybiFrame.PONG_FRAME:
				wrapper.notifyPong(frame.getPayload());
				break;
			default:
				super.dispatchFrame(frame);
		}
	}
	
	public String toString() {
		return "Hybi-10";
	}

}
