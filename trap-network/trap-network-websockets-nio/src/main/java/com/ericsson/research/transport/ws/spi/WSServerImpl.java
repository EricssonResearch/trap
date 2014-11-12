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

import com.ericsson.research.transport.ManagedServerSocket;
import com.ericsson.research.transport.ManagedServerSocketClient;
import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WSURI;

public class WSServerImpl implements ManagedServerSocketClient, WSServer {

	private ManagedServerSocket acceptSocket;
	private final WSAcceptListener serverListener;
	private final String host;
	private int	port;
	private final WSSecurityContext securityContext;

	public WSServerImpl(WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException {
		this("localhost", 0, serverListener, securityContext);
	}
	
	public WSServerImpl(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException {
		this.host = host;
		this.port = port;
		this.serverListener = serverListener;
		this.securityContext = securityContext;
		bind();
	}

	private void bind() throws IOException {
		if (securityContext == null)
			acceptSocket = new ManagedServerSocket();
		else
			acceptSocket = WSSecureSocketFactory.getSecureServerSocket(securityContext);
		acceptSocket.registerClient(this);
		acceptSocket.listen(host, port);
	}

	public void notifyAccept(final ManagedSocket socket) {
		serverListener.notifyAccept(new WSNioEndpoint(socket, new WSPrefetcher(securityContext), null));
	}
	
	public void notifyBound(ManagedServerSocket socket) {
		serverListener.notifyReady(this);
	}

	public void notifyError(Exception e) {
		if (port > 65000) {
			e.printStackTrace();
        } else {
			try {
				port++;
				bind();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public WSURI getURI() {
		String scheme = (securityContext==null ? "ws" : "wss");

		InetSocketAddress inetAddress = getAddress();
		String host = inetAddress.getAddress().getHostAddress();
		int port = inetAddress.getPort();
		String resource = "ws";
		
		byte[] address = inetAddress.getAddress().getAddress();
		boolean isv6 = address.length > 4;
		
		if (isv6)
			host = "[" + host + "]";

		try {
			return new WSURI(scheme + "://" + host + ":" + port + "/" + resource);
		} catch (IllegalArgumentException e) {
			// Should never happen
			e.printStackTrace();
			return null;
		}
	}
	
	public InetSocketAddress getAddress() {
		return acceptSocket.getInetAddress();
	}

	public void close() {
        acceptSocket.close();
	}

}
