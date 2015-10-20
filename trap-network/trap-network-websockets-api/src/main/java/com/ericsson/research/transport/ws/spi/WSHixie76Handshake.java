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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ericsson.research.transport.ws.WSException;

public class WSHixie76Handshake extends WSAbstractHandshake implements WSConstants {

	private int num1,num2;
	private String key3;
	
	public WSHixie76Handshake(WSAbstractProtocol protocol) {
		super(protocol);
		if(protocol.client)
			expectedHandshakeBodyLength = 16;
		else
			expectedHandshakeBodyLength = 8;
	}

	public void sendHandshake(OutputStream os) throws IOException {
		StringBuffer h = new StringBuffer();
		if(protocol.client) {
			h.append(GET);
			h.append(" ");
			h.append(protocol.resource);
			h.append(" ");
			h.append(HTTP11);
			h.append(WSHixie75Handshake.COMMON_PART);
			h.append(HOST_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.host);
			if ((protocol.securityContext==null && protocol.port!=80) || (protocol.securityContext!=null && protocol.port!=443)) {
				h.append(":");
				h.append(protocol.port);
			}
			h.append(CRLF);
			h.append(ORIGIN_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.origin);
			int spaces1 = (int) (Math.random()*12+1);
			int spaces2 = (int) (Math.random()*12+1);
			int max1 = Integer.MAX_VALUE / spaces1;
			int max2 = Integer.MAX_VALUE / spaces2;
			num1 = (int) (Math.random() * max1 + 1);
			num2 = (int) (Math.random() * max2 + 1);
			String spc1 = generateString(spaces1, 0x20, 0x20);
			String spc2 = generateString(spaces2, 0x20, 0x20);
			String gbg1 = generateString((int) (Math.random()*12+1), 0x3A, 0x7E);
			String gbg2 = generateString((int) (Math.random()*12+1), 0x3A, 0x7E);
			String key1 = splice(splice(String.valueOf(num1*spaces1), spc1), gbg1);
			String key2 = splice(splice(String.valueOf(num2*spaces2), spc2), gbg2);
			h.append(CRLF);
			h.append(SEC_WEBSOCKET_KEY1_HEADER);
			h.append(COLON_SPACE);
			h.append(key1);
			h.append(CRLF);
			h.append(SEC_WEBSOCKET_KEY2_HEADER);
			h.append(COLON_SPACE);
			h.append(key2);
			h.append(CRLFCRLF);
			key3 = generateString(8, 0x00, 0xFF);
			h.append(key3);
		} else {
			h.append(WSHixie75Handshake.SERVER_PREAMBLE);
			h.append(WSHixie75Handshake.COMMON_PART);
			h.append(SEC_WEBSOCKET_ORIGIN_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.origin);
			h.append(CRLF);
			h.append(SEC_WEBSOCKET_LOCATION_HEADER);
			h.append(COLON_SPACE);
			h.append(protocol.location);
			h.append(CRLFCRLF);
		}
		synchronized(os) {
			os.write(h.toString().getBytes(ISO_8859_1));
			if(!protocol.client)
				os.write(body);
			os.flush();
		}
	}

	private String splice(String src1, String src2) {
		StringBuffer sb = new StringBuffer(src1);
		for (int i = 0; i < src2.length(); i++) {
			int pos = (int) Math.round(Math.random() * sb.length());
			sb.insert(pos, src2.charAt(i));
		}
		return sb.toString();
	}

	private String generateString(int length, int startChar, int endChar) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int j = (int) (Math.floor(Math.random() * (endChar - startChar)) + startChar);
			sb.append((char)j);
		}
		return sb.toString();
	}

	public void headersRead() throws WSException {
		if(protocol.client) {
			if (!protocol.origin.equals(getHeader(SEC_WEBSOCKET_ORIGIN_HEADER)))
				throw new WSException("Failed to match origin ("+protocol.origin+" != "+getHeader(SEC_WEBSOCKET_ORIGIN_HEADER));
			StringBuffer location = new StringBuffer();
			if(protocol.securityContext!=null)
				location.append(WSS_SCHEMA);
			else
				location.append(WS_SCHEMA);
			location.append(protocol.host);
			if((protocol.securityContext==null && protocol.port != 80) || (protocol.securityContext!=null && protocol.port != 443)) {
				location.append(":");
				location.append(protocol.port);
			}
			location.append(protocol.resource);
			if (!location.toString().equals(getHeader(SEC_WEBSOCKET_LOCATION_HEADER)))
				throw new WSException("Failed to match location ("+location+" != "+getHeader(SEC_WEBSOCKET_LOCATION_HEADER)+")");
		} else {
			if (getHeader(SEC_WEBSOCKET_KEY1_HEADER) == null || getHeader(SEC_WEBSOCKET_KEY2_HEADER) == null)
				throw new WSException("One of Sec-WebSocket-Key* headers was null");
			protocol.origin = getHeader(ORIGIN_HEADER);
			StringBuffer loc = new StringBuffer();
			if (protocol.securityContext!=null)
				loc.append(WSS_SCHEMA);
			else
				loc.append(WS_SCHEMA);
			loc.append(getHeader(HOST_HEADER));
			loc.append(protocol.resource);
			protocol.location = loc.toString();
		}
	}

	public void bodyRead() throws WSException {
		if(protocol.client) {
			try {
				byte[] bs = key3.getBytes("ISO-8859-1");
				bs = createDigest(num1, num2, bs);
				for(int i = 0; i<bs.length; i++)
					if(body.length < i+1 || bs[i] != body[i])
						throw new WSException("Handshake result doesn't match");
			} catch (UnsupportedEncodingException e) {
				throw new WSException(e);
			}
		} else {
			// Count spaces
			int key1Spaces = countSpaces(getHeader(SEC_WEBSOCKET_KEY1_HEADER));
			int key2Spaces = countSpaces(getHeader(SEC_WEBSOCKET_KEY2_HEADER));
			if (key1Spaces <= 0 || key2Spaces <= 0)
				throw new WSException("Invalid number of spaces in handshake");
			// Remove non-number characters from the keys and parse them as integers
			long key1Num = Long.parseLong(getHeader(SEC_WEBSOCKET_KEY1_HEADER).replaceAll("[^0-9]", ""));
			long key2Num = Long.parseLong(getHeader(SEC_WEBSOCKET_KEY2_HEADER).replaceAll("[^0-9]", ""));
			long maxUint = Integer.MAX_VALUE;
			maxUint <<= 1;
			maxUint += 2;
			if (key1Num > maxUint || key2Num > maxUint)
				throw new WSException("Input values exceed permitted buffer size");
			// Divide the numbers as part of the spec.
			key1Num = key1Num / key1Spaces;
			key2Num = key2Num / key2Spaces;
			body = createDigest(key1Num, key2Num, body);
		}
	}
	
	private byte[] createDigest(long n1, long n2, byte[] n3) throws WSException {
		try {
			byte[] q = new byte[16];
			q[0] = (byte)((n1 & 0xFF000000) >> 24);
			q[1] = (byte)((n1 & 0xFF0000) >> 16);
			q[2] = (byte)((n1 & 0xFF00) >> 8);
			q[3] = (byte)((n1 & 0xFF));
			q[4] = (byte)((n2 & 0xFF000000) >> 24);
			q[5] = (byte)((n2 & 0xFF0000) >> 16);
			q[6] = (byte)((n2 & 0xFF00) >> 8);
			q[7] = (byte)((n2 & 0xFF));
			System.arraycopy(n3, 0, q, 8, 8);
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md.digest(q);
		} catch(NoSuchAlgorithmException e) {
			throw new WSException(e);
		}
	}
	
	private int countSpaces(String source) {
		int j = 0;
		for (int i = 0; i < source.length(); i++)
			if (source.charAt(i) == ' ')
				j++;
		return j;
	}
	
}
