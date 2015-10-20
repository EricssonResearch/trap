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

import junit.framework.Assert;

import org.junit.Test;

import com.ericsson.research.transport.ws.WSURI;

public class WSURITest {

	@Test
	public void testWsURI() {
		WSURI ws = new WSURI("ws://host:1/r");
		Assert.assertEquals("ws", ws.getScheme());
		Assert.assertEquals("host", ws.getHost());
		Assert.assertEquals(1, ws.getPort());
		Assert.assertEquals("/r", ws.getPath());
	}

	@Test
	public void testWssURI() {
		WSURI ws = new WSURI("wss://host:2/s");
		Assert.assertEquals("wss", ws.getScheme());
		Assert.assertEquals("host", ws.getHost());
		Assert.assertEquals(2, ws.getPort());
		Assert.assertEquals("/s", ws.getPath());
	}
	
	@Test
	public void testWsEmptyResource() {
		WSURI ws = new WSURI("ws://host:3");
		Assert.assertEquals("ws", ws.getScheme());
		Assert.assertEquals("host", ws.getHost());
		Assert.assertEquals(3, ws.getPort());
		Assert.assertEquals("/", ws.getPath());
	}
	
	@Test
	public void testWsDefaultPort() {
		WSURI ws = new WSURI("ws://host3");
		Assert.assertEquals("ws", ws.getScheme());
		Assert.assertEquals("host3", ws.getHost());
		Assert.assertEquals(80, ws.getPort());
		Assert.assertEquals("/", ws.getPath());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testWrongURI() {
		new WSURI("wsshost:2/s");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWrongScheme() {
		new WSURI("wrs://host:2/s");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWrongPort() {
		new WSURI("ws://host:s2/s");
	}

	@Test
	public void testToString() {
		String s = new WSURI("ws://example.com:5/resource").toString();
		Assert.assertEquals("ws://example.com:5/resource", s);
		s = new WSURI("wss://example.com:6/resource").toString();
		Assert.assertEquals("wss://example.com:6/resource", s);
		
		s = new WSURI("ws://example.com/resource").toString();
		Assert.assertEquals("ws://example.com/resource", s);
		s = new WSURI("wss://example.com/resource").toString();
		Assert.assertEquals("wss://example.com/resource", s);

		s = new WSURI("ws://example.com:80/resource").toString();
		Assert.assertEquals("ws://example.com/resource", s);
		s = new WSURI("wss://example.com:443/resource").toString();
		Assert.assertEquals("wss://example.com/resource", s);

		s = new WSURI("wss://example.com:80/resource").toString();
		Assert.assertEquals("wss://example.com:80/resource", s);
		s = new WSURI("ws://example.com:443/resource").toString();
		Assert.assertEquals("ws://example.com:443/resource", s);	
	}
	
}
