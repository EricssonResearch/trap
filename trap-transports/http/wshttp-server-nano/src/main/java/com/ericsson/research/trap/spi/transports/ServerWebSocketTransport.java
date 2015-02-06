package com.ericsson.research.trap.spi.transports;

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
import java.util.Collection;

import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.spi.WSNioEndpoint;
import com.ericsson.research.transport.ws.spi.WSPrefetcher;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.TrapEndpointImpl;
import com.ericsson.research.trap.nhttpd.IHTTPSession;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.Response.Status;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.nhttp.FullRequestHandler;
import com.ericsson.research.trap.spi.nhttp.WebSocketConstants;

public class ServerWebSocketTransport extends AbstractListenerTransport implements ListenerTrapTransport, FullRequestHandler
{
	public static final String	          REGISTER_RESOURCE	= "_connectTrapWS";
	private ListenerTrapTransportDelegate	listenerDelegate;
	private Object	                      listenerContext;
	private boolean	                      defaultHost	    = true;
	private ListenerHttpTransport server;

	public String getTransportName()
	{
		return "websocket";
	}

	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.WEBSOCKET;
	}

	public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
	{

		TrapEndpointImpl ep = (TrapEndpointImpl) listener;
		Collection<TrapTransport> transports = ep.getTransports();

		server = null;

		for (TrapTransport t : transports)
			if (t instanceof ListenerHttpTransport)
				server = (ListenerHttpTransport) t;

		if (server == null)
			throw new TrapException("Could not locate the appropriate server!");

		this.listenerDelegate = listener;
		this.listenerContext = context;

		this.delegate = new TrapTransportDelegate()
		{

			@Override
			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
			{
				// TODO Auto-generated method stub

			}
		};
		server.hostedObjects.put(REGISTER_RESOURCE, this);
		
	}

	public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
	{

		InetSocketAddress address;
		try
		{
			address = this.server.server.getAddress();
		}
		catch (IOException e)
		{
			address = new InetSocketAddress(0);
		}

		// Check for pre-existing port
		String port = this.getOption("autoconfig.port");

		if (port == null)
			port = Integer.toString(address.getPort());

		String hostName = this.getOption("autoconfig.host");

		if (hostName == null)
			hostName = defaultHost;

		if (hostName == null)
			hostName = this.getHostName(address.getAddress(), this.defaultHost, true);

		String targetUri = "ws" + (this.server.secure ? "s" : "") + "://" + hostName + ":" + port + "/" + REGISTER_RESOURCE;

		destination.setOption(this.prefix, WebSocketConstants.CONFIG_URI, targetUri);

	}

	@Override
	protected void internalDisconnect()
	{
		server.hostedObjects.remove(REGISTER_RESOURCE);
	}

	@Override
	public void flushTransport()
	{

	}

	@Override
    public void handle(IHTTPSession request, Response response)
    {
		response.setStatus(Status.SWITCH_PROTOCOL);
		Socket socket = request.getSocket();
		WSNioEndpoint ws = new WSNioEndpoint(socket, new WSPrefetcher(new WSSecurityContext(server.server.getSslc())), null);
		WebSocketTransport transport = new WebSocketTransport(ws);
		this.listenerDelegate.ttsIncomingConnection(transport, this, this.listenerContext);
        request.upgrade(ws);
    }

}
