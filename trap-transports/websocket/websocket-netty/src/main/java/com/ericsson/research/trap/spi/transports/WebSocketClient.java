package com.ericsson.research.trap.spi.transports;

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

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.ws.netty.NettyWSClient;

public class WebSocketClient extends AbstractTransport
{

	NettyWSClient	socket;

	@Override
	public String getTransportName()
	{
		return "websocket";
	}
	
	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.WEBSOCKET;
	}

	public boolean canConnect()
	{
		return true;
	}

	@Override
	public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
	{
		try
		{
			this.socket.send(message.serialize());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected boolean isClientConfigured()
	{
		return this.getOption(WebSocketConstants.CONFIG_URI) != null;
	}

	@Override
	protected void internalConnect() throws TrapException
	{
		synchronized (this)
		{
			if (this.socket != null)
				throw new TrapException("Cannot re-connect transport");

			String uri = this.getOption(WebSocketConstants.CONFIG_URI);

			if (uri == null)
			{
				this.logger.debug("WebSocket Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.");
				this.setState(TrapTransportState.ERROR);
				return;
			}

			try
			{
				this.socket = new NettyWSClient(uri, this);
			}
			catch (Exception e)
			{
				throw new TrapException(e);
			}
		}
	}

	@Override
	protected void internalDisconnect()
	{
		this.socket.close();
	}

	public void notifyError()
	{
		System.out.println("Error!");
		this.setState(TrapTransportState.ERROR);
	}

	public void notifyOpen()
	{
		this.setState(TrapTransportState.CONNECTED);
	}

	public void notifyClose()
	{
		this.setState(TrapTransportState.DISCONNECTED);
		this.socket = null;

	}

	public void notifyMessage(byte[] data)
	{
		this.receive(data, 0, data.length);
	}

	@Override
	public void flushTransport()
	{
		// This socket has no flush method
	}

}
