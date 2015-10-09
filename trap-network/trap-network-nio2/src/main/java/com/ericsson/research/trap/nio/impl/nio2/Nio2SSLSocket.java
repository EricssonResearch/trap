package com.ericsson.research.trap.nio.impl.nio2;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary
 * forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

public class Nio2SSLSocket extends Nio2SocketBase
{

	private static HandshakeStatus	hsStatus;
	private final ByteBuffer	    decodeBuf;
	private final SSLEngine	        engine;
	private final static ByteBuffer	zero	  = ByteBuffer.allocate(0);

	public Nio2SSLSocket(AsynchronousSocketChannel sock, SSLContext sslc, boolean clientMode)
	{
		super(sock);
		this.engine = sslc.createSSLEngine();
		this.engine.setUseClientMode(clientMode);
		String[] procols = { "TLSv1" };
		this.engine.setEnabledProtocols(procols);

		int packetBufferSize = engine.getSession().getPacketBufferSize();
		decodeBuf	= ByteBuffer.allocateDirect(packetBufferSize);
		readBuf = ByteBuffer.allocateDirect(packetBufferSize);
		
		for (int i = 0; i < 2; i++)
		{
			writeBufs[i] = ByteBuffer.allocateDirect(packetBufferSize);
			writeBufs[i].limit(0);
		}
		writeBufs[0].clear();
	}

	@Override
	public synchronized void send(ByteBuffer src)
	{

		if (src.remaining() == 0)
			return;

		_sslSend(src);
	}

	private synchronized void _sslSend(ByteBuffer src)
	{
		ByteBuffer buf = writeBufs[writeBuf];

		if (buf.remaining() == 0)
			return;

		HandshakeStatus hsStatus = null;

		try
		{
			SSLEngineResult result = this.engine.wrap(src, buf);

			switch (result.getStatus())
			{
			case BUFFER_OVERFLOW:
				return;
			case BUFFER_UNDERFLOW:
				throw new SSLException("Buffer UNDERflow on send. This should not happen.");
			case CLOSED:
				throw new SSLException("Closed SSL Engine");
			case OK:
				break;
			default:
				break;

			}
			hsStatus = runDelegatedTasks(result);

		}
		catch (SSLException e)
		{
			handler.error(e, this);
			return;
		}

		needsWriting.getAndSet(true);
		_write();

		switch (hsStatus)
		{
		case FINISHED:
			if (src == zero)
				handler.sent(this);
			break;
		case NOT_HANDSHAKING:
			send(src);
			break;
		case NEED_TASK:
			handler.error(new SSLException("TASK required once all were run"), this);
			break;
		case NEED_UNWRAP:
			break;
		case NEED_WRAP:
			_sslSend(zero);
			break;
		default:
			break;

		}

	}

	private HandshakeStatus runDelegatedTasks(SSLEngineResult result)
	{
		HandshakeStatus hsStatus = Nio2SSLSocket.hsStatus = result.getHandshakeStatus();
		if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK)
		{
			Runnable runnable;
			while ((runnable = engine.getDelegatedTask()) != null)
			{
				runnable.run();
			}
			hsStatus = engine.getHandshakeStatus();
			Nio2SSLSocket.hsStatus = hsStatus;
		}
		return hsStatus;
	}

	@Override
	public boolean isSecure()
	{
		return true;
	}

	void _read()
	{

		sock.read(readBuf, 128, TimeUnit.DAYS, this, new CompletionHandler<Integer, Nio2SSLSocket>()
		{

			@Override
			public void completed(Integer result, Nio2SSLSocket attachment)
			{
				try
				{

					
					if (result == -1)
					{
						handler.closed(Nio2SSLSocket.this);
						return;
					}
					
					readBuf.flip();

					unwrapLoop: while (readBuf.hasRemaining())
					{

						SSLEngineResult unwrap = engine.unwrap(readBuf, decodeBuf);

						switch (unwrap.getStatus())
						{
						case BUFFER_OVERFLOW:
							throw new RuntimeException("Buffer overflow that should not happen");

						case BUFFER_UNDERFLOW:
							readBuf.compact();
							break unwrapLoop;

						case CLOSED:
							return;
						case OK:
							decodeBuf.flip();
							if (decodeBuf.hasRemaining())
								handler.received(decodeBuf, Nio2SSLSocket.this);
							decodeBuf.clear();

							break;
						default:
							break;
						}

						HandshakeStatus hsStatus = runDelegatedTasks(unwrap);

						switch (hsStatus)
						{
						case FINISHED:
							synchronized (Nio2SSLSocket.this)
							{
								handler.sent(Nio2SSLSocket.this);
							}
							break;
						case NEED_TASK:
							break;
						case NEED_UNWRAP:
							break;
						case NEED_WRAP:
							_sslSend(zero);
							break;
						case NOT_HANDSHAKING:
							break;
						default:
							break;

						}
					}
					if (!readBuf.hasRemaining())
						readBuf.clear();

					_read();
				}
				catch (Exception exc)
				{
					handler.error(exc, Nio2SSLSocket.this);
					_close();
				}
			}

			@Override
			public void failed(Throwable exc, Nio2SSLSocket attachment)
			{
				handler.error(exc, Nio2SSLSocket.this);
				_close();
			}
		});
	}

	synchronized void _write()
	{

		// Cut off repeat entries
		if (!isWriting.compareAndSet(false, true))
			return;

		try
		{

			int cBuf = (writeBuf + 1) % 2;
			ByteBuffer buf = writeBufs[cBuf];

			if (buf.remaining() == 0)
			{

				// Maybe we need to flip the buffers for more data
				if (!needsWriting.compareAndSet(true, false))
				{
					isWriting.getAndSet(false);
					return;
				}

				// Clear the buffer so it can be used again
				buf.clear();

				// We do. Flip the buffers to get more data to write.
				writeBuf = (writeBuf + 1) % 2;
				cBuf = (writeBuf + 1) % 2;
				buf = writeBufs[cBuf];
				buf.flip();

				if (hsStatus == HandshakeStatus.FINISHED || hsStatus == HandshakeStatus.NOT_HANDSHAKING)
					handler.sent(this);
			}

			final ByteBuffer mBuf = buf;

			sock.write(mBuf, this, new CompletionHandler<Integer, Nio2SSLSocket>()
			{

				@Override
				public void completed(Integer result, Nio2SSLSocket attachment)
				{
					isWriting.getAndSet(false);
					_write();
				}

				@Override
				public void failed(Throwable exc, Nio2SSLSocket attachment)
				{
					isWriting.getAndSet(false);
					handler.error(exc, Nio2SSLSocket.this);
				}
			});

		}
		catch (RuntimeException e)
		{
			isWriting.getAndSet(false);
			throw e;
		}
	}

}
