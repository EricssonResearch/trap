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

import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;

public abstract class WSAbstractProtocolWrapper implements WSInterface, WSListener {

	protected WSAbstractProtocol protocol;
	protected WSListener listener;
	
	WSAbstractProtocolWrapper() {}
	
	public WSAbstractProtocolWrapper(WSAbstractProtocol protocol, WSListener listener) {
		this.setProtocol(protocol);
		this.listener = listener;
	}

	public WSAbstractProtocol getProtocol() {
		return this.protocol;
	}
	
	protected void setProtocol(WSAbstractProtocol protocol) {
		if(this.protocol == protocol)
			return;
		if(this.protocol != null)
			this.protocol.setWrapper(null);
		this.protocol = protocol;
		if(this.protocol != null)
			protocol.setWrapper(this);
	}
	
	public abstract void forceClose();
	public abstract OutputStream getRawOutput() throws IOException;
	
	public void setReadListener(WSListener listener) {
		this.listener = listener;
	}
	
	public void open() throws IOException {
		this.notifyConnected();
	}
	
	public void send(byte[] binaryData) throws IOException {
		this.protocol.send(binaryData);
	}

	public void send(String utf8String) throws IOException {
		this.protocol.send(utf8String);
	}

	public void ping(byte[] payload) throws IOException {
		this.protocol.ping(payload);
	}

	public void close() {
		this.protocol.close();
	}
	
	public void notifyOpen(WSInterface socket) {
		if(this.listener != null)
			this.listener.notifyOpen(socket);
	}
	
	public void notifyMessage(String utf8String) {
		if(this.listener != null)
			this.listener.notifyMessage(utf8String);
	}
	
	public void notifyMessage(byte[] data) {
		if(this.listener != null)
			this.listener.notifyMessage(data);
	}
	
	public void notifyPong(byte[] payload) {
		if(this.listener != null)
			this.listener.notifyPong(payload);
	}
	
	public void notifyClose() {
		if(this.listener != null)
			this.listener.notifyClose();
	}

	public void notifyError(Throwable t) {
		if(this.listener != null)
			this.listener.notifyError(t);
		this.forceClose();
	}
	
	public void notifyConnected() {
		this.protocol.notifyConnected();
	}

	public void notifyDisconnected() {
		this.protocol.notifyDisconnected();
	}

	public void notifySocketData(byte[] data, int length) {
		this.protocol.notifySocketData(data, length);
	}

	public void notifyError(Exception e) {
		if(this.listener != null)
			this.listener.notifyError(e);
		this.forceClose();
	}

}
