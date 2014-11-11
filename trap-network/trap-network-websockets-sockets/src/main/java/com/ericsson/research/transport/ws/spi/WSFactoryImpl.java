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

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WSURI;

public class WSFactoryImpl extends WSFactory {

	public WSInterface _createWebSocketClient(WSURI uri, WSListener listener, int version, WSSecurityContext securityContext) throws IOException
	{
		switch(version) {
			case WSFactory.VERSION_HIXIE_75:
				return new WSSocketEndpoint(new WSHixie75(uri, securityContext), listener);
			case WSFactory.VERSION_HIXIE_76:
				return new WSSocketEndpoint(new WSHixie76(uri, securityContext), listener);
			case WSFactory.VERSION_HYBI_10:
				return new WSSocketEndpoint(new WSHybi10(uri, securityContext), listener);
			case WSFactory.VERSION_RFC_6455:
				return new WSSocketEndpoint(new WSRfc6455(uri, securityContext), listener);
			default:
				throw new IOException("Invalid WS implementation version "+version);
		}
	}

	public WSServer _createWebSocketServer(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException
	{
		return new WSServerImpl(host, port, serverListener, securityContext);
	}

}