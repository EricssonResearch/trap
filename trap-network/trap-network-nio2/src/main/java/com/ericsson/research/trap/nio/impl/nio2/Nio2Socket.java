/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */

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

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Vladimir Katardjiev
 */
public class Nio2Socket extends Nio2SocketBase
{

	ReadWriteLock[]	rws	= new ReadWriteLock[2];

	public Nio2Socket(AsynchronousSocketChannel sock)
	{
		super(sock);

		for (int i = 0; i < 2; i++)
		{
			rws[i] = new ReentrantReadWriteLock();
		}
	}

	@Override
	public void send(ByteBuffer src)
	{

		if (src.remaining() == 0)
			return;

		ByteBuffer buf;
		Lock lock;
		synchronized (writeBufs)
		{
			buf = writeBufs[writeBuf];
			lock = rws[writeBuf].writeLock();
			lock.lock();
		}

		try
		{
			if (buf.remaining() == 0)
				return;

			if (src.remaining() > buf.remaining())
			{
				int lim = src.limit();
				src.limit(src.position() + buf.remaining());
				buf.put(src);
				src.limit(lim);
			}
			else
			{
				buf.put(src);
			}
			needsWriting.getAndSet(true);
		}
		finally
		{
			lock.unlock();
		}
		_write();

		send(src);
	}

	@Override
	public boolean isSecure()
	{
		return false;
	}

	public void _read()
	{

		sock.read(readBuf, 128, TimeUnit.DAYS, this, new CompletionHandler<Integer, Nio2Socket>()
		{

			@Override
			public void completed(Integer result, Nio2Socket attachment)
			{
				
				if (result == -1)
				{
					handler.closed(Nio2Socket.this);
					return;
				}
				
				try
				{
					readBuf.flip();
					handler.received(readBuf, Nio2Socket.this);
					readBuf.clear();
					_read();
				}
				catch (Exception exc)
				{
					handler.error(exc, Nio2Socket.this);
					_close();
				}
			}

			@Override
			public void failed(Throwable exc, Nio2Socket attachment)
			{
				if (!(exc instanceof ClosedChannelException))
				{
					handler.error(exc, Nio2Socket.this);
					_close();
				}
			}
		});
	}

	int	lastWriteRemaining	= 0;

	void _write()
	{

		// Cut off repeat entries
		synchronized (isWriting)
		{
			if (!isWriting.compareAndSet(false, true))
				return;
		}

		try
		{

			int cBuf = (writeBuf + 1) % 2;
			ByteBuffer buf = writeBufs[cBuf];

			if (buf.remaining() == 0)
			{

				synchronized (isWriting)
				{
					// Maybe we need to flip the buffers for more data
					if (!needsWriting.compareAndSet(true, false))
					{
						isWriting.getAndSet(false);
						return;
					}
				}

				// Clear the buffer so it can be used again
				buf.clear();

				// We do. Flip the buffers to get more data to write.
				synchronized (writeBufs)
				{
					writeBuf = (writeBuf + 1) % 2;
					cBuf = (writeBuf + 1) % 2;
					buf = writeBufs[cBuf];
					rws[cBuf].readLock().lock();
				}
				rws[cBuf].readLock().unlock();

				// Ensure the other thread finishes flushing

				buf.flip();

				lastWriteRemaining = buf.remaining();

				handler.sent(this);
			}

			final ByteBuffer mBuf = buf;

			sock.write(mBuf, this, new CompletionHandler<Integer, Nio2Socket>()
			{

				@Override
				public void completed(Integer result, Nio2Socket attachment)
				{
					lastWriteRemaining = mBuf.remaining();
					isWriting.getAndSet(false);
					_write();
				}

				@Override
				public void failed(Throwable exc, Nio2Socket attachment)
				{
					isWriting.getAndSet(false);
					handler.error(exc, Nio2Socket.this);
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
