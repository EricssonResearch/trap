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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.ericsson.research.transport.ws.WSListener;

public class WSSocketEndpoint extends WSAbstractProtocolWrapper {

	protected Socket socket;
	protected OutputStream os;
	
	public WSSocketEndpoint(WSAbstractProtocol protocol, WSListener listener) {
		super(protocol, listener);
	}

	public WSSocketEndpoint(Socket socket, WSAbstractProtocol protocol, WSListener listener) {
		super(protocol, listener);
		this.socket = socket;
		this.startListenerThread(socket);
	}
	
	private void startListenerThread(final Socket socket) {
		new Thread() {
			public void run() {
				byte[] data = new byte[4096];
				try {
					InputStream is = socket.getInputStream();
					for(;;) {
						int length = is.read(data);
						if(length == -1) {
							WSSocketEndpoint.this.notifyDisconnected();
							return;
						}
						WSSocketEndpoint.this.notifySocketData(data, length);
					}
				} catch (Exception e) {
					WSSocketEndpoint.this.notifyError(e);
				}
			}
		}.start();
	}
	
	public void open() throws IOException {
		this.socket = new Socket(this.protocol.host, this.protocol.port);
		this.startListenerThread(this.socket);
		this.protocol.open();
	}
	
	public OutputStream getRawOutput() throws IOException {
		if(this.os == null)
			this.os = this.socket.getOutputStream();
		return this.os;
	}
	
	public void forceClose() {
		try {
			if(this.socket!=null)
				this.socket.close();
		} catch (IOException e) {}
		this.notifyDisconnected();
		this.os = null;
		this.socket = null;
	}
	
	public String toString() {
		return "Socket ("+this.protocol+")";
	}

    public InetSocketAddress getLocalSocketAddress()
    {
        return (InetSocketAddress) this.socket.getLocalSocketAddress();
    }
    
    public InetSocketAddress getRemoteSocketAddress()
    {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
    }

}
