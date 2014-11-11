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

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSURI;

public abstract class WSAbstractProtocol extends WSAbstractProtocolWrapper implements WSConstants {
	
	public static final int HANDSHAKING = 0;
	public static final int OPEN = 1;
	public static final int CLOSING = 2;
	public static final int CLOSED = 3;
	
	protected int state = HANDSHAKING;
	
	protected WSAbstractHandshake handshake;
	protected byte[] buf = empty;
	protected WSAbstractFrame currentFrame;

	protected boolean client;

	protected String host;
	protected String resource;
	protected int port;
	protected String origin;
	protected String location;
	protected String protocol;
	protected final WSSecurityContext securityContext;
	
	protected WSAbstractProtocolWrapper wrapper;
	
	public WSAbstractProtocol(WSURI uri, WSSecurityContext securityContext) {
		host = uri.getHost();
		origin = "http://"+host;
		port = uri.getPort();
		resource = uri.getPath();
		client = true;
		this.securityContext = securityContext;
	}
	
	public WSAbstractProtocol(WSSecurityContext securityContext) {
		this.securityContext = securityContext;
	}
	
	protected void setWrapper(WSAbstractProtocolWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public String getState() {
		switch (state) {
			case HANDSHAKING:
				return "HANDSHAKING";
			case OPEN:
				return "OPEN";
			case CLOSING:
				return "CLOSING";
			case CLOSED:
				return "CLOSED";
		}
		return "UKNOWN";
	}
	
	public synchronized void close() {
		if (state == CLOSED || state == CLOSING)
			return;
		state = CLOSING;
		forceClose();
	}
	
	public void forceClose() {
		wrapper.forceClose();
	}

	public OutputStream getRawOutput() throws IOException {
		return wrapper.getRawOutput();
	}

	public void notifyConnected() {
		try {
			if(client)
				getHandshake().sendHandshake(getRawOutput());
		} catch(IOException e) {
			notifyError(e);
		}
	}
	
	public void notifyDisconnected() {
		state = CLOSED;
		wrapper.notifyClose();
	}

	public void notifySocketData(byte[] data, int length) {
		try {
			if(length == 0)
				return;
			byte[] newbuf = new byte[buf.length + length];
			if(buf.length > 0)
				System.arraycopy(buf, 0, newbuf, 0, buf.length);
			System.arraycopy(data, 0, newbuf, buf.length, length);
			buf = newbuf;
			int read;
			do {
				if(state == HANDSHAKING)
					read = getHandshake().deserialize(buf, buf.length);
				else {
					if(currentFrame == null)
						currentFrame = createEmptyFrame();
					read = currentFrame.deserialize(buf, buf.length);
				}
				if(read > 0) {
					if(read == buf.length) {
						buf = empty;
						read = 0;
					} else {
						newbuf = new byte[buf.length - read];
						System.arraycopy(buf, read, newbuf, 0, newbuf.length);
						buf = newbuf;
					}
					if(state == HANDSHAKING) {
						// This is for prefetching handshake, it will choose the protocol,
						// to which we are switching here and feeding it with the rest
						// of the buffer if any.
						WSAbstractProtocol newProtocol = getHandshake().protocol;
						wrapper.setProtocol(newProtocol);
						if(!client)
							getHandshake().sendResponse();
						newProtocol.state = OPEN;
						newProtocol.wrapper.notifyOpen(newProtocol.wrapper);
						if(read > 0 && newProtocol != this) {
							newProtocol.wrapper.notifySocketData(buf, buf.length);
							return;
						}
					} else {
						dispatchFrame(currentFrame);
						currentFrame = null;
					}
				}
			} while(read > 0);
		} catch(Exception e) {
			notifyError(e);
		}
	}

	protected abstract WSAbstractHandshake getHandshake();
	protected abstract WSAbstractFrame createEmptyFrame();
	protected abstract void dispatchFrame(WSAbstractFrame frame) throws IOException, WSException;

}
