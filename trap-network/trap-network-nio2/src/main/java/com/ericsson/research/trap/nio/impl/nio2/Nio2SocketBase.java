package com.ericsson.research.trap.nio.impl.nio2;

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ericsson.research.trap.nio.Socket;

public abstract class Nio2SocketBase implements Socket
{

	static final int	            BUF_SIZE	 = 16 * 1024;
	final AsynchronousSocketChannel	sock;
	InetSocketAddress	            remote;
	SocketHandler	                handler;
	ByteBuffer	                    readBuf	     = ByteBuffer.allocateDirect(BUF_SIZE);
	final ByteBuffer[]	            writeBufs	 = new ByteBuffer[2];
	volatile int	                writeBuf	 = 0;
	final AtomicBoolean	            isWriting	 = new AtomicBoolean(false);
	final AtomicBoolean	            needsWriting	= new AtomicBoolean(false);

	public Nio2SocketBase(AsynchronousSocketChannel sock)
	{
		this.sock = sock;
		for (int i = 0; i < 2; i++)
		{
			writeBufs[i] = ByteBuffer.allocateDirect(BUF_SIZE);
			writeBufs[i].limit(0);
		}
		writeBufs[0].clear();
	}

	abstract void _read();

	void open()
	{
		sock.connect(remote, this, new CompletionHandler<Void, Nio2SocketBase>()
		{

			@Override
			public void completed(Void result, Nio2SocketBase attachment)
			{
				handler.opened(Nio2SocketBase.this);
				_read();
			}

			@Override
			public void failed(Throwable exc, Nio2SocketBase attachment)
			{
				handler.error(exc, Nio2SocketBase.this);
			}
		});
	}

	@Override
	public void close()
	{
		_close();
		handler.closed(this);
	}

	void _close()
	{
		try
		{
			sock.close();
		}
		catch (IOException ex)
		{
			if (!(ex instanceof AsynchronousCloseException))
				Logger.getLogger(Nio2Socket.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public InetSocketAddress getLocalSocketAddress() throws IOException
	{
		return (InetSocketAddress) sock.getLocalAddress();
	}

	public InetSocketAddress getRemoteSocketAddress() throws IOException
	{
		return (InetSocketAddress) sock.getRemoteAddress();
	}

	@Override
	public void setHandler(SocketHandler handler)
	{
		this.handler = handler;
	}

	@Override
	public void open(InetSocketAddress remote) throws IOException
	{
		this.remote = remote;
		open();
	}

	@Override
	public void open(InetAddress host, int port) throws IOException
	{
		this.open(new InetSocketAddress(host, port));
	}

	@Override
	public void open(String addr, int port) throws IOException
	{
		this.open(new InetSocketAddress(InetAddress.getByName(addr), port));

	}
}
