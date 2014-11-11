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
import java.net.URI;
import java.util.HashSet;
import java.util.WeakHashMap;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.http.HTTPHandler;
import com.ericsson.research.trap.impl.http.HTTPHoster;
import com.ericsson.research.trap.impl.http.HTTPServletAdaptor;
import com.ericsson.research.trap.impl.http.HTTPSession;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapHostingTransport;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.WeakMap;

public class HTTPServletListener extends AbstractListenerTransport implements ListenerTrapTransport, TrapHostingTransport
{
	
	private ListenerTrapTransportDelegate	listener;
	private Object							listenerContext;
	private String							ctxPath;
	
	public HTTPServletListener() throws TrapException
	{
		super();
		this.transportPriority = TrapTransportPriority.HTTP_SERVLET;
		if (!HTTPServletAdaptor.hasContexts())
			throw new TrapException("No contexts found for Servlet listener. Aborting...");
	}
	
	@Override
	public String getTransportName()
	{
		return "http";
	}
	
	@Override
	public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
	{
		this.listener = listener;
		this.listenerContext = context;
		String ctxPath = this.getOption("context");
		String resPath = this.getOption("path");
		this.ctxPath = HTTPServletAdaptor.addListener(this, ctxPath, resPath);
		
		if (this.getOption("autoconfig.port") == null)
			this.logger.warn("Servlet HTTP transport improperly configured. Please configure autoconfig.port. Autoconfig has been disabled for the listener...");
		
		if (this.getOption("autoconfig.host") == null)
			this.logger.warn("Servlet HTTP transport improperly configured. Please configure autoconfig.host. Defaulting to localhost with no discovery...");
		
	}
	
	@Override
	public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
	{
		String url = this.getUrl(defaultHost);
		
		if (url != null)
			destination.setOption(this.prefix, "url", url);
	}
	
	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.HTTP;
	}
	
	@Override
	public void fillAuthenticationKeys(HashSet<String> keys)
	{
		super.fillAuthenticationKeys(keys);
	}
	
	@Override
	protected void internalDisconnect()
	{
		HTTPServletAdaptor.removeListener(this.ctxPath);
	}
	
	WeakMap<String, HTTPHandler>	sessions	= new WeakMap<String, HTTPHandler>();
	
	public HTTPHandler handle(HTTPSession session, String trapSessionId)
	{
		if (trapSessionId != null && trapSessionId.trim().length() != 0)
		{
			HTTPHandler transport = this.sessions.get(trapSessionId);
			
			if (transport == null)
			{
				session.response().setStatus(404);
				session.finish();
				return null;
			}
			else
			{
				transport.handle(session);
				return transport;
			}
		}
		else
		{
			String id = UID.randomUID();
			
			// Create a listener for the requests
			HTTPServletTransport t = new HTTPServletTransport(this, id);
			
			synchronized (this.sessions)
			{
				this.sessions.put(id, t);
			}
			
			this.listener.ttsIncomingConnection(t, this, this.listenerContext);
			
			try
			{
				session.response().setStatus(200);
				session.response().getWriter().print(id);
				session.finish();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			return t;
			
		}
	}
	
	// This HashMap has weak keys but strong values.
	WeakHashMap<TrapHostable, HTTPHandler>	hosters	= new WeakHashMap<TrapHostingTransport.TrapHostable, HTTPHandler>();
	
	@Override
	public URI addHostedObject(TrapHostable hosted, String preferredPath)
	{
		HTTPHoster hoster = null;
		synchronized (this.sessions)
		{
			if (preferredPath == null || this.sessions.containsKey(preferredPath))
				preferredPath = UID.randomUID();
			hoster = new HTTPHoster(hosted);
			this.sessions.put(preferredPath, hoster);
		}
		this.hosters.put(hosted, hoster);
		URI uri = URI.create(this.getUrl(null) + "/" + preferredPath);
		hosted.setURI(uri);
		return uri;
	}
	
	private String getUrl(String defaultHost)
	{
		// Check for pre-existing port
		String port = this.getOption("autoconfig.port");
		
		if (port == null)
		{
			return null;
		}
		
		String hostName = this.getOption("autoconfig.host");
        
        if (hostName == null)
            hostName = defaultHost;
        
        if (hostName == null)
            hostName = "localhost";
		
		String scheme = this.getOption("autoconfig.scheme");
		
		if (scheme == null)
			scheme = "http";
		
		String path = this.getOption("autoconfig.path");
		
		if (path == null)
			path = this.ctxPath;
		
		String url = scheme + "://" + hostName + ":" + port + this.ctxPath;
		
		return url;
	}

	@Override
	public void flushTransport()
	{
		
	}
	
}
