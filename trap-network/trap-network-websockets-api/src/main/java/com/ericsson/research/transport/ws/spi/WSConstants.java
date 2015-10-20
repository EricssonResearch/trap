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


public interface WSConstants {
	
	String ISO_8859_1 = "iso-8859-1";
	String UTF_8 = "UTF-8";
	String HTTP11 = "HTTP/1.1";
	String CRLF = "\r\n";
	String COLON_SPACE = ": ";
	String CRLFCRLF = "\r\n\r\n";
	String GET = "GET";
	String WS_SCHEMA = "ws://";
	String WSS_SCHEMA = "wss://";
	String ILLEGAL_STATE = "Illegal state";
	byte[] empty = new byte[0];
	long CLOSE_TIMEOUT = 5000;
	
	// Common
	String HOST_HEADER = "Host";
	String UPGRADE_HEADER = "Upgrade";
	String CONNECTION_HEADER = "Connection";
	String WEBSOCKET_VALUE = "WebSocket";
	String UPGRADE_VALUE = "Upgrade";

	// Hixie-75
	String ORIGIN_HEADER = "Origin"; // Also RFC 6455
	String WEBSOCKET_LOCATION_HEADER = "WebSocket-Location";
	String WEBSOCKET_ORIGIN_HEADER = "WebSocket-Origin";

	// Hixie-76
	String SEC_WEBSOCKET_KEY1_HEADER = "Sec-WebSocket-Key1";
	String SEC_WEBSOCKET_KEY2_HEADER = "Sec-WebSocket-Key2";
	String SEC_WEBSOCKET_ORIGIN_HEADER = "Sec-WebSocket-Origin";
	String SEC_WEBSOCKET_LOCATION_HEADER = "Sec-WebSocket-Location";

	// Hybi-10
	String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	String SEC_WEBSOCKET_KEY_HEADER = "Sec-WebSocket-Key";
	String SEC_WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";
	String SEC_WEBSOCKET_VERSION_HEADER = "Sec-WebSocket-Version";
	String SEC_WEBSOCKET_ACCEPT_HEADER = "Sec-WebSocket-Accept";

}
