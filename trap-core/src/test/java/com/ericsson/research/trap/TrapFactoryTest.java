package com.ericsson.research.trap;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.trap.impl.TrapConfigurationImpl;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.github.kristofa.test.http.Method;
import com.github.kristofa.test.http.MockHttpServer;
import com.github.kristofa.test.http.SimpleHttpResponseProvider;

public class TrapFactoryTest
{
	  private static final int PORT = 51234;
	  private static final String baseUrl = "http://localhost:" + PORT;
	
	private SimpleHttpResponseProvider	responseProvider;
	private MockHttpServer				server;
	
	@Before
	public void setUp() throws Exception
	{
		this.responseProvider = new SimpleHttpResponseProvider();
		this.server = new MockHttpServer(PORT, this.responseProvider);
		this.server.start();
	}
	
	@After
	public void tearDown() throws Exception
	{
		this.server.stop();
	}
	
	@Test
	public void testHttpResolution() throws Exception
	{
		String url = "http://trapjs.org";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testHttpPortResolution() throws Exception
	{
		String url = "http://trapjs.org:42";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(42, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testHttpsResolution() throws Exception
	{
		String url = "https://trapjs.org";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(443, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testWsResolution() throws Exception
	{
		String url = "ws://trapjs.org";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.websocket.wsuri"));
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.websocket.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.websocket.host"));
	}
	
	@Test
	public void testWsPortResolution() throws Exception
	{
		String url = "ws://trapjs.org:43";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.websocket.wsuri"));
		Assert.assertEquals(43, cfg.getIntOption("trap.transport.websocket.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.websocket.host"));
	}
	
	@Test
	public void testWssResolution() throws Exception
	{
		String url = "wss://trapjs.org";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(url, cfg.getOption("trap.transport.websocket.wsuri"));
		Assert.assertEquals(443, cfg.getIntOption("trap.transport.websocket.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.websocket.host"));
	}
	
	@Test
	public void testSocketResolution() throws Exception
	{
		String url = "socket://trapjs.org:80";
		String cfgStr = TrapFactory.resolveConfiguration(url);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		// CFG must NOT modify URL in any way
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.socket.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.socket.host"));
	}
	
	@Test
	public void testRemoteResolution() throws Exception
	{
		String testRes = "/test1/trap.txt";
		this.responseProvider.expect(Method.GET, testRes).respondWith(200, "text/plain", "http://trapjs.org");
		String cfgStr = TrapFactory.resolveConfiguration(baseUrl + testRes);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		Assert.assertEquals("http://trapjs.org", cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testSecondResolution() throws Exception
	{
		String testRes = "/test2/trap.txt";
		this.responseProvider.expect(Method.GET, testRes).respondWith(200, "text/plain", "http://trapjs.org");
		String cfgStr = TrapFactory.resolveConfiguration(baseUrl + "/foobar\n" + baseUrl + testRes);
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		Assert.assertEquals("http://trapjs.org", cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testSkippedResolution() throws Exception
	{
		// This test should skip the invalid HTTP line, and just give the backup config.
		String cfgStr = TrapFactory.resolveConfiguration(baseUrl + "/foobar\n" + "http://trapjs.org");
		TrapConfiguration cfg = new TrapConfigurationImpl(cfgStr);
		
		Assert.assertEquals("http://trapjs.org", cfg.getOption("trap.transport.http.url"));
		Assert.assertEquals(80, cfg.getIntOption("trap.transport.http.port", 0));
		Assert.assertEquals("trapjs.org", cfg.getOption("trap.transport.http.host"));
	}
	
	@Test
	public void testInvalidProtocol() throws Exception
	{
		// This test should skip the invalid HTTP line, and just give the backup config.
		String cfgStr = TrapFactory.resolveConfiguration("ftp://trapjs.org");
		Assert.assertEquals(0, cfgStr.trim().length());
	}
	
}
