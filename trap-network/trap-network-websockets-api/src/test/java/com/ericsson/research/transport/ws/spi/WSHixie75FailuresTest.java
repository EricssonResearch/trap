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
import java.io.OutputStream;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.transport.ws.WSURI;

public class WSHixie75FailuresTest {

	private WSLoopbackEndpoint le;
	
	@Before
	public void setup() {
		le = new WSLoopbackEndpoint(new WSHixie75(new WSURI("ws://localhost:12345"), null), null, new WSHixie75(null), null);
	}
	
	@Test(expected=IOException.class)
	public void testClosedSendString() throws IOException {
		le.send("test");
	}

	@Test(expected=IOException.class)
	public void testClosedSendBinary() throws IOException {
		le.send("test".getBytes());
	}

	@Test
	public void testWrongFrameType() throws IOException {
		le.open();
		OutputStream os = le.getRawOutput();
		synchronized(os) {
			os.write(new byte[]{(byte)0x81, 0x00});
			os.flush();
		}
		Assert.assertTrue(le.isClosed());
	}
	
	@Test
	public void testWrongOrigin() throws IOException {
		le = new WSLoopbackEndpoint(new WSHixie75(new WSURI("ws://localhost:12345"), null), null, new WSHixie75(null) {
			protected WSAbstractHandshake getHandshake() {
				if(handshake == null)
					handshake = new WSHixie75Handshake(this) {
						public void sendHandshake(OutputStream os) throws IOException {
							protocol.origin = "asefafg";
							super.sendHandshake(os);
						}
					};
				return handshake;
			}
		}, null);
		le.open();
		Assert.assertTrue(le.isClosed());
	}
	
	@Test
	public void testWrongLocation() throws IOException {
		le = new WSLoopbackEndpoint(new WSHixie75(new WSURI("ws://localhost:12345"), null), null, new WSHixie75(null) {
			protected WSAbstractHandshake getHandshake() {
				if(handshake == null)
					handshake = new WSHixie75Handshake(this) {
						public void sendHandshake(OutputStream os) throws IOException {
							protocol.location = "asefafg";
							super.sendHandshake(os);
						}
					};
				return handshake;
			}
		}, null);
		le.open();
		Assert.assertTrue(le.isClosed());
	}
	
}
