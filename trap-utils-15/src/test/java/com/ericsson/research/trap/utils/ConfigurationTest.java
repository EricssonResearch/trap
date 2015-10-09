package com.ericsson.research.trap.utils;

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

import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.trap.utils.spi.ConfigurationImpl;


/*
 * trap.version = 0.1 trap.provider = Ericsson Trap Services trap.transports = 3
 * trap.wakeup = 1
 * 
 * trap.transport.http = true trap.transport.http.uri =
 * http://example.com/trap/http
 * 
 * trap.transport.longpoll = true trap.transport.longpoll.uri =
 * http://example.com/trap/lp
 * 
 * trap.transport.ws = true trap.transport.ws.uri = ws://example.com/trap/ws
 * 
 * trap.wakeup.applepush = true trap.wakeup.applepush.config = value
 */

public class ConfigurationTest
{
	
	private Configuration	tc;

	@Before
	public void createConfig()
	{
		this.tc = new ConfigurationImpl();
	}

	@Test
	public void testCreateFromString()
	{
		String s = "option=value\nanotheroption=anothervalue\n";
		
		ConfigurationImpl tc = new ConfigurationImpl(s);
		
		Assert.assertEquals(tc.getConfig().size(), 2);
		Assert.assertEquals(tc.getConfig().get("option"), "value");
		Assert.assertEquals(tc.getConfig().get("anotheroption"), "anothervalue");
	}	
	
	@Test
	public void testBaseOption()
	{
		this.tc.setOption("foo", "bar");
		
		Assert.assertEquals("bar", this.tc.getOption("foo"));
	}
	
	@Test
	public void testOptionTree()
	{
		this.tc.setOption("erf.foo", "bar");
		Assert.assertEquals("bar", this.tc.getOption("erf.foo"));
	}
	
	@Test
	public void testPrefixMethods()
	{
		this.tc.setOption("erf", "foo", "foo");
		Assert.assertEquals("foo", this.tc.getOption("erf", "foo"));
	}
	
	@Test
	public void testPrefixInterchangeability()
	{
		this.tc.setOption("prefix", "key", "value");
		this.tc.setOption("combined.key", "setting");
		
		Assert.assertEquals("value", this.tc.getOption("prefix", "key"));
		Assert.assertEquals("setting", this.tc.getOption("combined.key"));
	}
	
	@Test
	public void testDeepTree()
	{
		this.tc.setOption("com.ericsson.research.trap.settingA", "true");
		this.tc.setOption("com.ericsson.research.trap", "settingB", "false");
		
		Assert.assertEquals("true", this.tc.getOption("com.ericsson.research.trap", "settingA"));
		Assert.assertEquals("false", this.tc.getOption("com.ericsson.research.trap.settingB"));
	}

	@Test
	public void testConfigDeserialization()
	{
		String config = "trap.version = 0.1\ntrap.config = true\n\ntrap.wrong1\ntrap.wrong2=\ntrap.transport.websocket = false";
		this.tc.initFromString(config);
		
		Assert.assertEquals("0.1", this.tc.getOption("trap", "version"));
		Assert.assertEquals("true", this.tc.getOption("trap.config"));
		Assert.assertEquals("false", this.tc.getOption("trap.transport.websocket"));
		Assert.assertEquals(null, this.tc.getOption("trap.wrong1"));
		Assert.assertEquals(null, this.tc.getOption("trap.wrong2"));
	}
	
	@Test
	public void testConfigSerialization()
	{
		this.tc.setOption("trap.version", "0.1");
		this.tc.setOption("trap.config", "true");
		this.tc.setOption("trap.transport.websocket", "false");
		
		String cfgString = this.tc.toString();

		Assert.assertTrue(cfgString.contains("trap.version = 0.1\n"));
		Assert.assertTrue(cfgString.contains("trap.config = true\n"));
		Assert.assertTrue(cfgString.contains("trap.transport.websocket = false"));
	}
	
	@Test
	public void testSerializeDeserialize()
	{
		
		this.tc.setOption("trap.version", "0.1");
		this.tc.setOption("trap.config", "true");
		this.tc.setOption("trap.transport.websocket", "false");
		
		String cfgString = this.tc.toString();
		ConfigurationImpl tc2 = new ConfigurationImpl((String) null);
		tc2.initFromString(cfgString);
		
		Assert.assertEquals("0.1", tc2.getOption("trap", "version"));
		Assert.assertEquals("true", tc2.getOption("trap.config"));
		Assert.assertEquals("false", tc2.getOption("trap.transport.websocket"));
	}
	
	@Test
	public void testGetOptions()
	{
		this.tc.setOption("trap.transport.websocket.port", "81");
		this.tc.setOption("trap.transport.http.port", "80");
		this.tc.setOption("trap.transport.websocket.enabled", "false");

		Map<String, String> m = this.tc.getOptions("trap.transport.websocket");

		Assert.assertEquals(m.size(), 2);
		Assert.assertEquals(m.get("port"), "81");
		Assert.assertEquals(m.get("enabled"), "false");
		
		m.put("enabled", "true");
		
		Assert.assertEquals(m.get("enabled"), "true");
		Assert.assertEquals(this.tc.getOption("trap.transport.websocket.enabled"), "true");

		m = this.tc.getOptions("undefined");
		
		Assert.assertEquals(m.size(), 0);
		
		m.put("key", "value");
		
		Assert.assertEquals(m.get("key"), "value");
		Assert.assertEquals(this.tc.getOption("undefined.key"), "value");
	}

	@Test
	public void testGetOptionsWithPrefixes()
	{
		this.tc.setOption("trap.transport.websocket.port", "81");
		this.tc.setOption("trap.transport.http.port", "80");
		this.tc.setOption("trap.transport.websocket.enabled", "false");

		Map<?, ?> m = this.tc.getOptions("trap.transport.websocket.", false);

		Assert.assertEquals(m.size(), 2);
		Assert.assertEquals(m.get("trap.transport.websocket.port"), "81");
		Assert.assertEquals(m.get("trap.transport.websocket.enabled"), "false");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNull()
	{
		this.tc.getOptions("").put(null, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullValue()
	{
		this.tc.getOptions("").put(null, "");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullKey()
	{
		this.tc.getOptions("").put("", null);
	}
	
}
