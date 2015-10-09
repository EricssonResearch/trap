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

public class WSHixie75Handshake extends WSAbstractHandshake implements WSConstants {

	protected static final String SERVER_PREAMBLE = HTTP11 + " 101 Web Socket Protocol Handshake";
	protected static final String COMMON_PART = CRLF+UPGRADE_HEADER+COLON_SPACE+WEBSOCKET_VALUE+CRLF+CONNECTION_HEADER+COLON_SPACE+UPGRADE_VALUE+CRLF;
	
	public WSHixie75Handshake(WSAbstractProtocol protocol) {
		super(protocol);
	}
	
	public void sendHandshake(OutputStream os) throws IOException {
		StringBuffer h = new StringBuffer();
		if(protocol.client) {
			h.append(GET);
			h.append(" ");
			h.append(protocol.resource);
			h.append(" ");
			h.append(HTTP11);
			h.append(COMMON_PART);
			h.append(HOST_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.host);
			if ((protocol.securityContext==null && protocol.port!=80) || (protocol.securityContext!=null && protocol.port!=443)) {
				h.append(":");
				h.append(protocol.port);
			}
			h.append(CRLF);
			h.append(ORIGIN_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.origin);
			h.append(CRLFCRLF);
		} else {
			h.append(SERVER_PREAMBLE);
			h.append(COMMON_PART);
			h.append(WEBSOCKET_ORIGIN_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.origin);
			h.append(CRLF);
			h.append(WEBSOCKET_LOCATION_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.location);
			h.append(CRLFCRLF);
		}
		synchronized(os) {
			os.write(h.toString().getBytes(ISO_8859_1));
			os.flush();
		}
	}

	public void headersRead() throws WSException {
		if(protocol.client) {
			if(!protocol.origin.equals(getHeader(WEBSOCKET_ORIGIN_HEADER)))
				throw new WSException("Failed to match origin ("+protocol.origin+" != "+getHeader(WEBSOCKET_ORIGIN_HEADER)+")");
			StringBuffer location = new StringBuffer();
			if(protocol.securityContext!=null)
				location.append(WSS_SCHEMA);
			else
				location.append(WS_SCHEMA);
			location.append(protocol.host);
			if((protocol.securityContext==null && protocol.port != 80) || (protocol.securityContext!=null && protocol.port != 443)) {
				location.append(":");
				location.append(protocol.port);
			}
			location.append(protocol.resource);
			if (!location.toString().equals(getHeader(WEBSOCKET_LOCATION_HEADER)))
				throw new WSException("Failed to match location ("+location+" != "+getHeader(WEBSOCKET_LOCATION_HEADER)+")");
		} else {
			protocol.origin = getHeader(ORIGIN_HEADER);
			StringBuffer loc = new StringBuffer();
			if(protocol.securityContext!=null)
				loc.append(WSS_SCHEMA);
			else
				loc.append(WS_SCHEMA);
			loc.append(getHeader(HOST_HEADER));
			loc.append(protocol.resource);
			protocol.location = loc.toString();
		}
	}

}
