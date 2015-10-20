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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WSURI;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;

public class ServerWebSocketTransport extends AbstractListenerTransport implements ListenerTrapTransport, WSAcceptListener
{

	private WSServer						serverSocket;
	private ListenerTrapTransportDelegate	listenerDelegate;
	private Object							listenerContext;
	private String							host;
	private boolean							defaultHost	= false;

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
		this.listenerDelegate = listener;
		this.listenerContext = context;

		this.delegate = new TrapTransportDelegate() {

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

		try
		{

			int port = 0;
			try
			{
				port = Integer.parseInt(this.getOption(WebSocketConstants.CONFIG_SERVER_PORT));
			}
			catch (Throwable t)
			{
			}

			this.host = this.getOption(WebSocketConstants.CONFIG_SERVER_HOST);

			if ((this.host == null) || (this.host.trim().length() == 0))
			{
				this.host = "0.0.0.0";
				this.defaultHost = true;
			}
			// TODO: This listener IP# should be configurable
            
            WSSecurityContext secContext = null;
            
            if (this.getOption(CERT_USE_INSECURE_TEST) != null)
            {
                secContext = new WSSecurityContext("jks", "trapserver.jks", "Ericsson", "jks", "trapserver.jks", "Ericsson");
                this.logger.warn("Using insecure SSL context");
            }
            else
            {
                try
                {
                    String keyType = this.getOption(CERT_KEYSTORE_TYPE);
                    String keyName = this.getOption(CERT_KEYSTORE_NAME);
                    String keyPass = this.getOption(CERT_KEYSTORE_PASS);
                    
                    String trustType = this.getOption(CERT_TRUSTSTORE_TYPE);
                    String trustName = this.getOption(CERT_TRUSTSTORE_NAME);
                    String trustPass = this.getOption(CERT_TRUSTSTORE_PASS);
                    
                    if (keyName != null)
                        secContext = new WSSecurityContext(keyType, keyName, keyPass, trustType, trustName, trustPass);
                    
                    this.logger.info("Using provided SSL context. Keystore [{}], Truststore [{}]", keyName, trustName);
                    
                }
                catch (Exception e)
                {
                }
            }
            
            this.serverSocket = WSFactory.startWebSocketServer(this.host, port, this, secContext);
			this.setState(TrapTransportState.CONNECTED);
		}
		catch (Exception e)
		{
			throw new TrapException(e);
		}
	}

	public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
	{


		WSURI uri = this.serverSocket.getURI();
		InetSocketAddress address = this.serverSocket.getAddress();

		// Check for pre-existing port
		String port = this.getOption("autoconfig.port");

		if (port == null)
			port = Integer.toString(uri.getPort());

		String hostName = this.getOption("autoconfig.host");
		
		if (hostName == null)
		    hostName = defaultHost;

		if (hostName == null)
			hostName = this.getHostName(address.getAddress(), this.defaultHost, true);

		String targetUri = uri.getScheme() + "://" + hostName + ":" + port + uri.getPath();

		destination.setOption(this.prefix, WebSocketConstants.CONFIG_URI, targetUri);

	}

	@Override
	protected void internalDisconnect()
	{
        try
        {
			WSFactory.stopWebSocketServer(this.serverSocket);
		}
        catch (Exception e)
        {
        }
	}

	public void notifyError(Exception e)
	{
		System.err.println("Error!");
		this.setState(TrapTransportState.ERROR);
	}

	@Override
	public void notifyAccept(WSInterface socket)
	{
		WebSocketTransport transport = new WebSocketTransport(socket);
		this.listenerDelegate.ttsIncomingConnection(transport, this, this.listenerContext);

	}

	@Override
	public void notifyReady(WSServer server)
	{
		this.serverSocket = server;
	}

	@Override
	public void fillAuthenticationKeys(@SuppressWarnings("rawtypes") HashSet keys)
	{
		// Authentication is redundant on servers.
	}

	@Override
    public void notifyError(Throwable t)
    {
		this.logger.error("WebSocket Error", t);
	}

	@Override
	public void flushTransport()
	{
		
	}

}
