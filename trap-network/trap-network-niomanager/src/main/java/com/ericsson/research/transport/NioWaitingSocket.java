package com.ericsson.research.transport;

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
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioWaitingSocket {
	
	private final NioEndpoint	socket;
	private final int			ops;
	private boolean				done		= false;
	private long				timeoutTime	= System.currentTimeMillis() + 30000;
	private int					retries		= 5;
	private final boolean		server;
	private final SocketAddress	addr;
	private SelectableChannel	channel;
	
	public NioWaitingSocket(NioEndpoint socket, SocketAddress addr, boolean server, int ops) {
		this.socket = socket;
		this.addr = addr;
		this.server = server;
		this.ops = ops;
	}
	
	public SelectableChannel getChannel() {
		return this.channel;
	}

	public SelectableChannel createChannel() throws IOException {
		if (this.server) {
			this.channel = ServerSocketChannel.open();
			this.channel.configureBlocking(false);
			((ServerSocketChannel) this.channel).socket().setReuseAddress(true);
			((ServerSocketChannel) this.channel).socket().bind(this.addr);
		} else {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);
			((SocketChannel) this.channel).connect(this.addr);
		}
		return this.channel;
	}

	public int getOps() {
		return this.ops;
	}

	public NioEndpoint getSocket() {
		return this.socket;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDone() {
		return this.done;
	}

	public void setTimeoutTime(long timeoutTime) {
		this.timeoutTime = timeoutTime;
	}

	public long getTimeoutTime() {
		return this.timeoutTime;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public int getRetries() {
		return this.retries;
	}
	
}