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

import java.util.Collection;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.tomcat.WSServletAdaptor;
import com.ericsson.research.trap.spi.tomcat.WSServletListener;
import com.ericsson.research.trap.spi.tomcat.WSSocket;

public class TomcatWSListener extends AbstractListenerTransport implements ListenerTrapTransport, WSServletListener
{

	private ListenerTrapTransportDelegate	listenerDelegate;
	private Object							listenerContext;
	@SuppressWarnings("unused")
    private String							host;
	@SuppressWarnings("unused")
	private boolean							defaultHost	= false;
	private String	resPath;

	public TomcatWSListener()
	{
		this.transportPriority = -1001;
	}
	
	public String getTransportName()
	{
		return "websocket";
	}
	
	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.WEBSOCKET;
	}

	@Override
	public boolean canConnect()
	{
		return false;
	}

	@Override
	public boolean canListen()
	{
		return true;
	}

	public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
	{
		this.listenerDelegate = listener;
		this.listenerContext = context;

		this.delegate = new TrapTransportDelegate() {

			@Override
			public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
			{
			}

			@Override
			public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
			{
			}

			@Override
			public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
			{
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
		
		if (this.getOption("autoconfig.port") == null)
			this.logger.warn("WebSocket Tomcat transport improperly configured. Please configure autoconfig.port. Autoconfig has been disabled for the listener...");
		
		if (this.getOption("autoconfig.host") == null)
			this.logger.warn("WebSocket Tomcat transport improperly configured. Please configure autoconfig.host. Defaulting to localhost with no discovery...");

		try
		{
			this.resPath = WSServletAdaptor.addListener(this, null, null);
			this.setState(TrapTransportState.CONNECTED);
		}
		catch (Exception e)
		{
			throw new TrapException(e);
		}
	}

	public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
	{
        
		// Check for pre-existing port
		String port = this.getOption("autoconfig.port");
		
		if (port == null)
		{
			return;
		}
		
		String hostName = this.getOption("autoconfig.host");
		
		if (hostName == null)
		    hostName = defaultHost;
		
		if (hostName == null)
			hostName = "localhost";
		
		String scheme = this.getOption("autoconfig.scheme");
		
		if (scheme == null)
			scheme = "ws";
		
		String path = this.getOption("autoconfig.path");
		
		if (path == null)
			path = this.resPath;
        
		String targetUri = scheme + "://"+hostName+":" + port + this.resPath;
		destination.setOption(this.prefix, TomcatWSConstants.CONFIG_URI, targetUri);
	}

	@Override
	public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void internalConnect() throws TrapException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void internalDisconnect()
	{
		//this.serverSocket.close();
	}

	public void notifyError(Exception e)
	{
		System.err.println("Error!");
		this.setState(TrapTransportState.ERROR);
	}

	public void notifyError(Throwable t) {
		System.err.println("Error!");
		this.logger.error("WebSocket Error", t);
	}

	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
	}

	@Override
	public void flushTransport()
	{
		
	}

	@Override
	public WSSocket create()
	{
		// Tomcat should retain the socket, which should retain the transport.
		TomcatWSTransport t = new TomcatWSTransport();
		this.listenerDelegate.ttsIncomingConnection(t, this, this.listenerContext);
		return t.getSocket();
	}

}
