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
import java.util.HashMap;
import java.util.StringTokenizer;

import com.ericsson.research.transport.ws.WSException;

public abstract class WSAbstractHandshake implements WSConstants {

	protected WSAbstractProtocol protocol;
	protected String preamble;
	protected HashMap<String, String> headers = new HashMap<String, String>(10);
	protected byte[] body;
	protected int expectedHandshakeBodyLength = 0;
	
	public WSAbstractHandshake(WSAbstractProtocol protocol) {
		this.protocol = protocol;
	}
	
	public String getHeader(String name) {
		return this.headers.get(name.toLowerCase());
	}
	
	public abstract void sendHandshake(OutputStream os) throws IOException;
	
	public void preambleRead() throws WSException {
		StringTokenizer st = new StringTokenizer(this.preamble, " ");
		if(st.hasMoreTokens()) {
			if(this.protocol.client) {
					this.checkHTTPProtocol(st.nextToken(), 1, 1);
					if(st.hasMoreTokens()) {
						String status = st.nextToken().trim();
						if(!"101".equals(status))
							throw new WSException("Invalid status code "+status);
					}
					if(st.hasMoreTokens())
						return;
			} else {
				if(!GET.equals(st.nextToken()))
					throw new WSException("Invalid HTTP method, only GET is supported");
				if(st.hasMoreTokens()) {
					this.protocol.resource = st.nextToken();
					if(st.hasMoreTokens()) {
						this.checkHTTPProtocol(st.nextToken(), 1, 1);
						return;
					}
				}
			}
		}
		throw new WSException("Invalid preamble ("+this.preamble+")");
	}
	
	public abstract void headersRead() throws WSException;
	public void bodyRead() throws WSException {}

    private static final byte PREAMBLE = 0;
	private static final byte HEADER_NAME = 1;
	private static final byte HEADER_VALUE = 2;
	private static final byte BODY = 3;
	
	private byte state = PREAMBLE; 
	private boolean crRead = false;
	private int pos = 0;
	private int h = 0;
	private int c = 0;
	
	public int deserialize(byte[] data, int length) throws WSException {
		for(;;) {
			if(length <= this.pos)
				return 0;
			if(this.crRead && data[this.pos] != '\n')
				throw new WSException("Missing expected LF");
			switch(this.state) {
				case PREAMBLE:
					if(this.crRead) {
						try {
							this.preamble = new String(data, 0, this.pos-1, UTF_8);
						} catch (UnsupportedEncodingException e) {
							throw new WSException(e);
						}
						this.crRead = false;
						this.state = HEADER_NAME;
						this.h = this.pos + 1;
						this.preambleRead();
					}
					break;
				case HEADER_NAME:
					if(this.crRead) {
						if(this.h+1 == this.pos) {
							if(!WEBSOCKET_VALUE.equalsIgnoreCase(this.getHeader(UPGRADE_HEADER)))
								throw new WSException("Invalid "+UPGRADE_HEADER+" header");
							boolean upgrade = false;
							if(this.getHeader(CONNECTION_HEADER)!=null) {
								StringTokenizer st = new StringTokenizer(this.getHeader(CONNECTION_HEADER), " ,");
								while(st.hasMoreTokens()) {
									String s = st.nextToken();
									if(s.equalsIgnoreCase(UPGRADE_VALUE)) {
										upgrade = true;
										break;
									}
								}
							}
							if(!upgrade)
								throw new WSException("Invalid "+CONNECTION_HEADER+" header");
							this.headersRead();
							this.state = BODY;
							if(this.expectedHandshakeBodyLength > 0) {
								this.h = this.pos + 1;
								this.pos += this.expectedHandshakeBodyLength;
								this.crRead = false;
								continue;
							} else
								return this.pos+1;
						} else
							throw new WSException("Unexpected end of header");
					} else
						if(data[this.pos] == ':') {
							this.state = HEADER_VALUE;
							this.c = this.pos;
						}
					break;
				case HEADER_VALUE:
					if(this.crRead) {
						try {
							String name = new String(data, this.h, this.c-this.h, UTF_8).trim().toLowerCase();
							String value = new String(data, this.c+1, this.pos-this.c-2, UTF_8);
							//FIXME: Hixie-76 may use spaces in arbitrary places of the header value
							if(value.startsWith(" "))
								value = value.substring(1);
							this.headers.put(name, value);
						} catch (UnsupportedEncodingException e) {
							throw new WSException(e);
						}
						this.crRead = false;
						this.state = HEADER_NAME;
						this.h = this.pos + 1;
					}
					break;
				case BODY:
					this.body = new byte[this.expectedHandshakeBodyLength];
					if(this.expectedHandshakeBodyLength > 0)
						System.arraycopy(data, this.h, this.body, 0, this.expectedHandshakeBodyLength);
					this.bodyRead();
					return this.pos + 1;
			}
			if(data[this.pos] == '\r')
				this.crRead = true;
			this.pos++;
		}
	}
	
	public void sendResponse() throws IOException {
		this.sendHandshake(this.protocol.getRawOutput());
	}
	
	protected byte[] computeSHA1(String s) throws WSException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(s.getBytes(ISO_8859_1), 0, s.length());
			return md.digest();
		} catch (Exception e) {
			throw new WSException(e);			
		}
	}
	
	protected void checkHTTPProtocol(String protocol, int minMaj, int minMin) throws WSException {
		if(!protocol.startsWith("HTTP/"))
			throw new WSException("Invalid protocol, only HTTP supported");
		String version = protocol.substring(5);
		int p = version.indexOf(".");
		if(p == -1)
			throw new WSException("Invalid HTTP version "+version);
		int major = Integer.parseInt(version.substring(0, p));
		int minor = Integer.parseInt(version.substring(p + 1));
		if (major < minMaj || (major == minMaj && minor < minMin))
			throw new WSException("Invalid protocol version, MUST be "+minMaj+"."+minMin+" or higher.");
	}
	
}
