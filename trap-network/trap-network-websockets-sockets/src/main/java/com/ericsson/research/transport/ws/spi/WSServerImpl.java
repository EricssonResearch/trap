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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WSURI;

public class WSServerImpl implements WSServer {

	private String host;
	private int	port;
	private WSAcceptListener serverListener;
	private WSSecurityContext securityContext;
	private ServerSocket acceptSocket;
	private boolean closing;
	
	public WSServerImpl(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException {
		this.host = host;
		this.port = port;
		this.serverListener = serverListener;
		this.securityContext = securityContext;
		bind();
	}

	private void bind() throws IOException {
		closing = false;
		for(int i=0;i<=65000;i++) {
			try {
				if (securityContext == null)
					acceptSocket = new ServerSocket(port, 0, InetAddress.getByName(host));
				else
					throw new UnsupportedOperationException();
					//acceptSocket = WSSecureSocketFactory.getSecureServerSocket(securityContext);
				new Thread() {
					public void run() {
						for(;!closing;) {
							try {
								Socket socket = acceptSocket.accept();
								serverListener.notifyAccept(new WSSocketEndpoint(socket, new WSPrefetcher(securityContext), null));
							} catch(IOException e) {
								if(serverListener!=null)
									serverListener.notifyError(e);
							}
						}
					}
				}.start();
				serverListener.notifyReady(this);
				break;
			} catch(IOException e) {
				if(port > 65000)
					throw e;
				else
					port++;
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
			if(serverListener!=null)
				serverListener.notifyError(e);
			return null;
		}
	}

	public InetSocketAddress getAddress() {
		return (InetSocketAddress)acceptSocket.getLocalSocketAddress();
	}

	public void close() {
		closing = true;
		try {
			acceptSocket.close();
		} catch(IOException e) {
			if(serverListener!=null)
				serverListener.notifyError(e);
		}
	}

}