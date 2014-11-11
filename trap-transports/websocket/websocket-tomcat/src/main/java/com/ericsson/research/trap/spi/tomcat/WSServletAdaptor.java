package com.ericsson.research.trap.spi.tomcat;

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

import java.util.concurrent.ConcurrentSkipListSet;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.WeakMap;

public class WSServletAdaptor
{
	
	private static final WeakMap<String, WSServletListener>	listeners		= new WeakMap<String, WSServletListener>();
	private static final ConcurrentSkipListSet<String>		servletContexts	= new ConcurrentSkipListSet<String>();
	
	/**
	 * Adds a listener to the HTTP servlet core.
	 * 
	 * @param listener
	 * @param servletPath The path to the servlet from the server URI root, including context and subpaths.
	 * @param resPath
	 * @return
	 * @throws TrapException
	 */
	public synchronized static String addListener(WSServletListener listener, String servletPath, String resPath) throws TrapException
	{
		if (servletPath != null && !servletContexts.contains(servletPath))
			throw new TrapException("Tried to register a context at an unknown path; context " + servletPath + " not represented in known contexts " + servletContexts);
		
		if (resPath != null && listeners.containsKey(resPath))
			throw new TrapException("Requested path [" + resPath + "] already occupied.");
		
		if (servletPath == null)
			servletPath = servletContexts.first();
		
		String id = resPath != null ? resPath : UID.randomUID();
		
		listeners.put(id, listener);
		
		return servletPath + "/" + id;
	}
	
	public static void removeListener(String contextPath)
	{
		// TODO: This doesn't work. Fix it.
		if (contextPath.contains("/"))
			contextPath = contextPath.split("/")[1];
		listeners.remove(contextPath);
	}
	
	public static void addServletContext(String ctx)
	{
		servletContexts.add(ctx);
	}
	
	public static void removeServletContext(String ctx)
	{
		servletContexts.remove(ctx);
	}
	
	public static WSSocket getSocket(String servletPath, String trapContextName)
	{
		
		if (trapContextName != null && trapContextName.trim().length() >= 0)
		{
			
			WSServletListener listener = listeners.get(trapContextName);
			
			if (listener != null)
			{
				return listener.create();
			}
			
		}
		
		// There is no associated Trap listener to deal with this connection.
		return null;
	}
	
	/*public static HTTPHandler handle(HTTPSession session, String servletContextName, String trapContextName, String trapSessionId)
	{
		if (trapContextName != null && trapContextName.trim().length() >= 0)
		{
			
			WSServletListener listener = listeners.get(trapContextName);
			
			if (listener != null)
			{
				return listener.handle(session, trapSessionId);
			}
			
		}
		
		// There is no associated Trap listener to deal with this connection.
		session.response().setStatus(404);
		session.finish();
		return null;
	}*/
	
	public static boolean hasContexts()
	{
		return servletContexts.size() > 0;
	}
	
}
