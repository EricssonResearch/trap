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

import java.io.IOException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.ericsson.research.transport.ws.WSURI;


public class WSBasicTest {

	private static final String str128 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$0123456789abcdefghijklmnopqrstuvwxyz!@#$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$12345678";
	
	private static final byte[] longdata;
	
	static {
		longdata = new byte[66666];
		Random r = new Random();
		for(int i=0;i<66666;i++)
			longdata[i] = (byte)r.nextInt(256);
	}
	
	private String testString;
	private byte[] testData;
	
	class WSReceivingListener extends WSTestListener {
		public WSReceivingListener(String name) {
			super(name);
		}
		public void notifyMessage(String string) {
			testString = string;
		}
		public void notifyMessage(byte[] data) {
			testData = data;
		}
	}
	
	class WSEchoListener extends WSTestListener {
		public WSEchoListener(String name) {
			super(name);
		}
		public void notifyMessage(String string) {
			try {
				socket.send(string);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void notifyMessage(byte[] data) {
			try {
				socket.send(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void testProtocol(WSLoopbackEndpoint client) throws IOException {
		client.open();
		Assert.assertTrue(client.isOpen());
		testString = null;
		client.send("test");
		Assert.assertEquals("test", testString);
		testString = null;
		client.send(str128);
		Assert.assertEquals(str128, testString);
		testData = null;
		for(int i=0;i<256;i++) {
			byte[] test = new byte[]{(byte)i};
			client.send(test);
			Assert.assertArrayEquals(test, testData);
		}
		testData = null;
		client.send(longdata);
		Assert.assertArrayEquals(longdata, testData);
		client.close();
		Assert.assertTrue(client.isClosed());
	}
	
	@Test
	public void testHixie75() throws IllegalArgumentException, IOException {
		testProtocol(
			new WSLoopbackEndpoint(new WSHixie75(new WSURI("ws://localhost:12345"), null), new WSReceivingListener("hixie75-client"),
								   new WSHixie75(null), new WSEchoListener("hixie75-server"))
		);
	}
	
	@Test
	public void testHixie76() throws IllegalArgumentException, IOException {
		testProtocol(
			new WSLoopbackEndpoint(new WSHixie76(new WSURI("ws://localhost:12345"), null), new WSReceivingListener("hixie76-client"),
								   new WSHixie76(null), new WSEchoListener("hixie76-server"))
		);
	}

	@Test
	public void testHybi10() throws IllegalArgumentException, IOException {
		testProtocol(
			new WSLoopbackEndpoint(new WSHybi10(new WSURI("ws://localhost:12345"), null), new WSReceivingListener("hybi10-client"),
								   new WSHybi10(null), new WSEchoListener("hybi10-server"))
		);
	}

	@Test
	public void testRfc6455() throws IllegalArgumentException, IOException {
		testProtocol(
			new WSLoopbackEndpoint(new WSRfc6455(new WSURI("ws://localhost:12345"), null), new WSReceivingListener("rfc6455-client"),
								   new WSRfc6455(null), new WSEchoListener("rfc6455-server"))
		);
	}
	
}
