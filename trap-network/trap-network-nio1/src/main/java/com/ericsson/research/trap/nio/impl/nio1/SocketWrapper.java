package com.ericsson.research.trap.nio.impl.nio1;

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

import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.ManagedSocketClient;
import com.ericsson.research.trap.nio.Socket;

public class SocketWrapper implements Socket, ManagedSocketClient
{
	
	private ManagedSocket socket;
	private boolean secure;
	private SocketHandler handler;

	SocketWrapper(ManagedSocket socket, boolean secure)
	{
		this.socket = socket;
		this.secure = secure;
		socket.registerClient(this);
	}

	@Override
	public void setHandler(SocketHandler handler)
	{
		this.handler = handler;
	}

	@Override
	public void send(ByteBuffer src)
	{
		if (src.hasArray())
		{
			try
            {
	            socket.write(src.array());
            }
            catch (IOException e)
            {
            	handler.error(e, this);
            }
			src.clear();
			src.limit(0);
		}
		else
			throw new RuntimeException("Whoops! Need to implement this wrapper (or do I?)");
		
		handler.sent(this);
	}

	@Override
	public void open(InetSocketAddress remote) throws IOException
	{
		socket.connect(remote);
	}

	@Override
	public void open(InetAddress host, int port) throws IOException
	{
		socket.connect(new InetSocketAddress(host, port));
	}

	@Override
	public void open(String addr, int port) throws IOException
	{
		socket.connect(addr, port);
	}

	@Override
	public void close()
	{
		socket.disconnect();
	}

	@Override
	public boolean isSecure()
	{
		return secure;
	}

	@Override
	public InetSocketAddress getLocalSocketAddress() throws IOException
	{
		return socket.getLocalSocketAddress();
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() throws IOException
	{
		return socket.getRemoteSocketAddress();
	}

	@Override
    public void notifyConnected()
    {
		handler.opened(this);
    }

	@Override
    public void notifyDisconnected()
    {
		handler.closed(this);
    }

	@Override
    public void notifySocketData(byte[] data, int size)
    {
		handler.received(ByteBuffer.wrap(data, 0, size), this);
    }

	@Override
    public void notifyError(Exception e)
    {
		handler.error(e, this);
    }

    @Override
    public SocketHandler getHandler()
    {
        return handler;
    }

}
