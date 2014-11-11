package com.ericsson.research.trap.impl.http;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTPServletUtil
{
	public static void addCorsHeaders(HttpServletRequest req, HttpServletResponse resp)
	{
		String origin = req.getHeader("Origin");
		
		if (origin == null)
			origin = "null";
		
		// Prevent HTTP Response Splitting.
		origin = origin.replaceAll("[\\n\\r]", "");
		
		resp.setHeader("Allow", "GET,PUT,POST,DELETE,OPTIONS");
		resp.setHeader("Access-Control-Allow-Origin", origin);
		resp.setHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
		resp.setHeader("Access-Control-Request-Methods", "GET,PUT,POST,DELETE,OPTIONS");
		resp.setHeader("Access-Control-Request-Headers", "Content-Type");
		resp.setHeader("Access-Control-Max-Age", "3600");
	}
	
	public static String[] getContexts(HttpServletRequest request, String prefix)
	{
		
		String ctx = "";
		String sess = "";
		String path = request.getPathInfo();
		if (path != null)
		{
			
			if (path.startsWith("/"))
				path = path.substring(1);
			
			String[] parts = path.split("/");
			if (parts.length >= 2)
			{
				ctx = parts[0];
				sess = parts[1];
			}
			else
				ctx = path;
		}
		
		String[] arr = new String[3];
		arr[0] = request.getContextPath();
		arr[1] = ctx;
		arr[2] = sess;
		
		return arr;
	}
}
