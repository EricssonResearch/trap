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
TestCase('configure', {
	
	setUp: function()
	{
		this.tc = new Trap.Configuration();
	},
	
	testTest: function()
	{
		assertTrue(true);
	},

	testCreateFromString: function()
	{
		var s = "option=value\nanotheroption=anothervalue\n";
		
		var tc = new Trap.Configuration(s);
		
		Assert.assertEquals(tc.config.size(), 2);
		Assert.assertEquals(tc.config.get("option"), "value");
		Assert.assertEquals(tc.config.get("anotheroption"), "anothervalue");
	},	
	
	testBaseOption: function()
	{
		this.tc.setOption("foo", "bar");
		
		Assert.assertEquals("bar", this.tc.getOption("foo"));
	},
	
	testOptionTree: function()
	{
		this.tc.setOption("erf.foo", "bar");
		Assert.assertEquals("bar", this.tc.getOption("erf.foo"));
	},
	
	testPrefixMethods: function()
	{
		this.tc.setOption("erf", "foo", "foo");
		Assert.assertEquals("foo", this.tc.getOption("erf", "foo"));
	},
	
	testPrefixInterchangeability: function()
	{
		this.tc.setOption("prefix", "key", "value");
		this.tc.setOption("combined.key", "setting");
		
		Assert.assertEquals("value", this.tc.getOption("prefix", "key"));
		Assert.assertEquals("setting", this.tc.getOption("combined.key"));
	},
	
	testDeepTree: function()
	{
		this.tc.setOption("com.ericsson.research.trap.settingA", "true");
		this.tc.setOption("com.ericsson.research.trap", "settingB", "false");
		
		Assert.assertEquals("true", this.tc.getOption("com.ericsson.research.trap", "settingA"));
		Assert.assertEquals("false", this.tc.getOption("com.ericsson.research.trap.settingB"));
	},
	
	testConfigDeserialization: function()
	{
		var config = "trap.version = 0.1\ntrap.config = true\n\ntrap.wrong1\ntrap.wrong2=\ntrap.transport.websocket = false";
		this.tc.initFromString(config);
		
		Assert.assertEquals("0.1", this.tc.getOption("trap", "version"));
		Assert.assertEquals("true", this.tc.getOption("trap.config"));
		Assert.assertEquals("false", this.tc.getOption("trap.transport.websocket"));
		Assert.assertEquals(null, this.tc.getOption("trap.wrong1"));
		Assert.assertEquals(null, this.tc.getOption("trap.wrong2"));
	},
	
	testConfigSerialization: function()
	{
		this.tc.setOption("trap.version", "0.1");
		this.tc.setOption("trap.config", "true");
		this.tc.setOption("trap.transport.websocket", "false");
		
		var cfgString = this.tc.toString();

		Assert.assertTrue(cfgString.contains("trap.version = 0.1\n"));
		Assert.assertTrue(cfgString.contains("trap.config = true\n"));
		Assert.assertTrue(cfgString.contains("trap.transport.websocket = false"));
	},
	
	testSerializeDeserialize: function()
	{
		
		this.tc.setOption("trap.version", "0.1");
		this.tc.setOption("trap.config", "true");
		this.tc.setOption("trap.transport.websocket", "false");
		
		var cfgString = this.tc.toString();
		var tc2 = new Trap.Configuration(null);
		tc2.initFromString(cfgString);
		
		Assert.assertEquals("0.1", tc2.getOption("trap", "version"));
		Assert.assertEquals("true", tc2.getOption("trap.config"));
		Assert.assertEquals("false", tc2.getOption("trap.transport.websocket"));
	},
	
	testGetOptions: function()
	{
		this.tc.setOption("trap.transport.websocket.port", "81");
		this.tc.setOption("trap.transport.http.port", "80");
		this.tc.setOption("trap.transport.websocket.enabled", "false");

		var m = this.tc.getOptions("trap.transport.websocket");

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
	},
	
	testGetOptionsWithPrefixes: function()
	{
		this.tc.setOption("trap.transport.websocket.port", "81");
		this.tc.setOption("trap.transport.http.port", "80");
		this.tc.setOption("trap.transport.websocket.enabled", "false");

		var m = this.tc.getOptions("trap.transport.websocket.", false);

		Assert.assertEquals(m.size(), 2);
		Assert.assertEquals(m.get("trap.transport.websocket.port"), "81");
		Assert.assertEquals(m.get("trap.transport.websocket.enabled"), "false");
	},

	testCustomConfigDeserialization: function()
	{
		var config = "trap.version = 0.1\ntrap.config = true\n\ntrap.transport.websocket = false";
		var tcc = new Trap.CustomConfiguration(config);
		
		Assert.assertEquals("0.1", tcc.getOption("trap", "version"));
		Assert.assertEquals("true", tcc.getOption("trap.config"));
		Assert.assertEquals("false", tcc.getOption("trap.transport.websocket"));
	},
	
	testCustomConfigSerialization: function()
	{
		var tcc = new Trap.CustomConfiguration("trap.static=true");
		
		tcc.setOption("trap.version", "0.1");
		tcc.setOption("trap.config", "true");
		tcc.setOption("trap.transport.websocket", "false");
		
		var cfgString = tcc.toString();

		Assert.assertTrue(cfgString.contains("trap.static = true\n"));
		Assert.assertTrue(cfgString.contains("trap.version = 0.1\n"));
		Assert.assertTrue(cfgString.contains("trap.config = true\n"));
		Assert.assertTrue(cfgString.contains("trap.transport.websocket = false"));
	},
	
	testCustomOptions: function()
	{
		var defaultConfig = "trap.transport.websocket.port=81\ntrap.transport.http.port=80\ntrap.transport.websocket.enabled=false";

		var tcc = new Trap.CustomConfiguration(defaultConfig);
		
		Assert.assertEquals(tcc.getOption("trap.transport.websocket.port"), "81");
		
		tcc.setOption("trap.transport.http.port", "83");

		Assert.assertEquals(tcc.getOption("trap.transport.http.port"), "83");
		
		var m = tcc.getOptions("trap.transport.websocket.");
		m.put("enabled", "true");
		
		Assert.assertEquals(m.get("enabled"), "true");
		Assert.assertEquals(tcc.getOption("trap.transport.websocket.enabled"), "true");
		
		m = tcc.getOptions("undefined");
		
		Assert.assertEquals(m.size(), 0);
		
		m.put("key", "value");
		
		Assert.assertEquals(m.get("key"), "value");
		Assert.assertEquals(tcc.getOption("undefined.key"), "value");
		
	}
	
});
