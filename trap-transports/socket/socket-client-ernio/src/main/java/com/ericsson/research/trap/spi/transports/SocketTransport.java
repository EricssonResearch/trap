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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLContext;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.nio.Nio;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.SSLUtil;

public class SocketTransport extends AbstractTransport implements SocketHandler
{

	private Socket	      socket;
	private final boolean	canConnect;

	// Create a new SocketTransport for connecting (=client)
	public SocketTransport()
	{
		// Client can connect
		this.canConnect = true;
		this.transportPriority = TrapTransportPriority.SOCKET_ERNIO;
	}

	// Create a new SocketTransport for receiving (=server)
	public SocketTransport(Socket socket)
	{
		// When called this way, the socket is already created, so all we need
		// to do
		// is set the local variables and tell the socket to give us its
		// callbacks.
		this.socket = socket;
		socket.setHandler(this);
		this.state = TrapTransportState.CONNECTED;

		// Server cannot connect
		this.canConnect = false;

		this.transportPriority = -100;
	}

	public boolean canConnect()
	{
		return this.canConnect;
	}

	@Override
	public boolean canListen()
	{
		return !this.canConnect;
	}

	public String getTransportName()
	{
		return "socket";
	}

	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.TCP;
	}

	@Override
    public void init()
    {
		if (socket != null)
			socket.close();
		socket = null;
    }

	protected boolean isClientConfigured()
	{
		try
		{
			String host = this.getOption(SocketConstants.CONFIG_HOST);
			Integer.parseInt(this.getOption(SocketConstants.CONFIG_PORT));
			if (host == null)
				throw new NullPointerException();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/*
	 * Called when the Trap Core wants this transport to connect to a server.
	 * This function must read the configuration (using #getOption()) and
	 * connect to the remote server (or throw error/set state to error if it
	 * fails)
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ericsson.research.trap.spi.transports.AbstractTransport#internalConnect
	 * ()
	 */
	protected synchronized void internalConnect() throws TrapException
	{

		// We read out the same options as the ones we wrote earlier
		String host = this.getOption(SocketConstants.CONFIG_HOST);
		int port = Integer.parseInt(this.getOption(SocketConstants.CONFIG_PORT));
		this.outBuf = null;

		try
		{
			// Create a new socket (outgoing)
			if (this.getBooleanOption(SocketConstants.CONFIG_SECURE, false))
			{
				SSLContext sslc = null;
				if (this.getBooleanOption(TrapTransport.CERT_IGNORE_INVALID, false))
					sslc = SSLUtil.getInsecure();
				else
					sslc = SSLContext.getDefault();
				this.socket = Nio.factory().sslClient(sslc);
			}
			else
				this.socket = Nio.factory().client();
			// Set us for callbacks
			this.socket.setHandler(this);
			// Ask it to connect. We will get a callback later on.
			this.socket.open(host, port);
		}
		catch (UnknownHostException e)
		{
			this.socket = null;
			throw new TrapException(e);
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (KeyManagementException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			throw new TrapException(e);
		}
	}

	ConcurrentLinkedQueue<ByteBuffer>	queue	= new ConcurrentLinkedQueue<ByteBuffer>();

	ByteArrayOutputStream	          outBuf	= null;

	/*
	 * Called when TrapCore wants to send a message. We will not perform any
	 * transport optimisations, so we will merely send the message through the
	 * socket. (non-Javadoc)
	 * 
	 * @see
	 * com.ericsson.research.trap.spi.transports.AbstractTransport#internalSend
	 * (com.ericsson.research.trap.spi.TrapMessage, boolean)
	 */

	boolean	                          queued	= false;

	public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
	{
		byte[] msg;
		try
		{
			msg = message.serialize();
		}
		catch (IOException e1)
		{
			this.setState(TrapTransportState.ERROR);
			throw new TrapTransportException(message, this.state);
		}

		queue.add(ByteBuffer.wrap(msg));

		if (!expectMore)
			_flush();

	}

	/*
	 * Called when the socket receives data. We'll just forward it. (We might
	 * want to add a thread break here)
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ericsson.research.transport.ManagedSocketClient#notifySocketData(
	 * byte[], int)
	 */
	public void notifySocketData(byte[] data, int size)
	{
		this.receive(data, 0, size);
	}

	/*
	 * Called when Trap wants to disconnect this transport.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.ericsson.research.trap.spi.transports.AbstractTransport#
	 * internalDisconnect()
	 */
	protected synchronized void internalDisconnect()
	{
		if (this.socket != null)
			this.socket.close();
		this.socket = null;
		this.setState(TrapTransportState.DISCONNECTING);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void fillAuthenticationKeys(HashSet keys)
	{
		super.fillAuthenticationKeys(keys);
		keys.add(TrapContextKeys.LocalIP);
		keys.add(TrapContextKeys.LocalPort);
		keys.add(TrapContextKeys.RemoteIP);
		keys.add(TrapContextKeys.RemotePort);
	}

	public void fillContext(Map<String, Object> context, Collection<String> filter)
	{

		super.fillContext(context, filter);

		InetSocketAddress local;
		InetSocketAddress remote;
		synchronized (this)
		{
			if (this.socket == null)
				return; // We'll update later

			// Set up the authentication state.
			try
			{
				local = this.socket.getLocalSocketAddress();
				remote = this.socket.getRemoteSocketAddress();

				if (filter.contains(TrapContextKeys.LocalIP))
					context.put(TrapContextKeys.LocalIP, local.getAddress().getHostAddress());

				if (filter.contains(TrapContextKeys.LocalPort))
					context.put(TrapContextKeys.LocalPort, local.getPort());

				if (filter.contains(TrapContextKeys.RemoteIP))
					context.put(TrapContextKeys.RemoteIP, remote.getAddress().getHostAddress());

				if (filter.contains(TrapContextKeys.RemotePort))
					context.put(TrapContextKeys.RemotePort, remote.getPort());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}

	@Override
	public void flushTransport()
	{
		_flush();
	}

	@Override
	public void sent(Socket sock)
	{
		_flush();
	}

	void _flush()
	{
		synchronized (socket)
		{
			for (;;)
			{
				ByteBuffer head = queue.peek();

				if (head == null)
					return;
				
				if (!head.hasRemaining())
				{
					queue.poll();
					continue;
				}

				socket.send(head);

				if (head.hasRemaining())
					return;

			}
		}
	}

	byte[]	rcvBuf	= new byte[4096];

	@Override
	public synchronized void received(ByteBuffer data, Socket sock)
	{
		while (data.hasRemaining())
		{
			int loopData = Math.min(rcvBuf.length, data.remaining());
			data.get(rcvBuf, 0, loopData);
			receive(rcvBuf, 0, loopData);
		}
	}

	@Override
	public void opened(Socket sock)
	{
		this.fillContext(this.contextMap, this.contextKeys);
		// Notify
		this.setState(TrapTransportState.CONNECTED);
	}

	@Override
	public void closed(Socket sock)
	{
		this.setState(TrapTransportState.DISCONNECTED);
	}

	@Override
	public void error(Throwable e, Socket sock)
	{
		e.printStackTrace();
		this.logger.info("SocketTransport moving to state ERROR due to an error from the underlying implementation: {}", e);
		this.setState(TrapTransportState.ERROR);
	}

}
