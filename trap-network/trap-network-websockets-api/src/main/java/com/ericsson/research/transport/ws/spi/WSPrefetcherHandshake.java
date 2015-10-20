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

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSSecurityContext;

public class WSPrefetcherHandshake extends WSAbstractHandshake implements WSConstants {

	private final WSSecurityContext securityContext;
	
	public WSPrefetcherHandshake(WSSecurityContext securityContext) {
		super(null);
		this.securityContext = securityContext;
	}
	
	public void preambleRead() throws WSException {}

	public void headersRead() throws WSException {
		if(getHeader(SEC_WEBSOCKET_VERSION_HEADER)!=null) {
			if("13".equals(getHeader(SEC_WEBSOCKET_VERSION_HEADER)))
				protocol = new WSRfc6455(securityContext);
			else
				if("8".equals(getHeader(SEC_WEBSOCKET_VERSION_HEADER)))
					protocol = new WSHybi10(securityContext);
		} else {
			if(getHeader(SEC_WEBSOCKET_KEY1_HEADER) != null && getHeader(SEC_WEBSOCKET_KEY2_HEADER) != null)
				protocol = new WSHixie76(securityContext);
			else
				if(getHeader(SEC_WEBSOCKET_KEY1_HEADER) == null && getHeader(SEC_WEBSOCKET_KEY2_HEADER) == null && getHeader(SEC_WEBSOCKET_KEY_HEADER) == null)
					protocol = new WSHixie75(securityContext);
		}
		if(protocol == null)
			throw new WSException("Could not determine WebSocket client version");
		WSAbstractHandshake handshake = protocol.getHandshake();
		expectedHandshakeBodyLength = handshake.expectedHandshakeBodyLength;
		handshake.preamble = preamble;
		handshake.headers = headers;
		handshake.preambleRead();
		handshake.headersRead();
	}
	
	public void bodyRead() throws WSException {
		protocol.getHandshake().body = body;
		protocol.getHandshake().bodyRead();
	}

	public void sendResponse() throws IOException {
		protocol.getHandshake().sendResponse();
	}

	public void sendHandshake(OutputStream os) throws IOException {
		protocol.getHandshake().sendHandshake(os);
	}

}
