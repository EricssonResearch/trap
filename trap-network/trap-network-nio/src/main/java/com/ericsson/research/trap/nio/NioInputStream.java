package com.ericsson.research.trap.nio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.ericsson.research.trap.nio.Socket.SocketHandler;

public class NioInputStream extends InputStream implements SocketHandler
{
	private static final ByteBuffer	EMPTY	    = ByteBuffer.allocate(0);
	private Object	                receiveLock	= new Object();
	private Object	                readLock	= new Object();
	LinkedBlockingDeque<ByteBuffer>	bufs	    = new LinkedBlockingDeque<ByteBuffer>();
	boolean	                        open	    = true;
	private SocketHandler	        handler;

	public NioInputStream()
	{
	}

	public NioInputStream(Socket sock)
	{
		handler = sock.getHandler();
		sock.setHandler(this);
	}

	public void setHandler(SocketHandler handler, Socket sock, boolean through)
	{
		synchronized (receiveLock)
		{
			this.handler = handler;
			if (through)
			{
				open = false;
				bufs.offer(EMPTY);
				synchronized (readLock)
				{
					for (ByteBuffer b : bufs)
						if (b.capacity() > 0 && b.remaining() > 0)
							handler.received(b, sock);
				}
			}
		}

	}

	@Override
	public void sent(Socket sock)
	{
		handler.sent(sock);
	}

	@Override
	public void received(ByteBuffer data, Socket sock)
	{
		synchronized (receiveLock)
		{
			if (open)
			{
				ByteBuffer b = ByteBuffer.allocate(data.remaining()).put(data);
				b.flip();
				bufs.add(b);
			}
			else
				handler.received(data, sock);
		}
	}

	@Override
	public void opened(Socket sock)
	{
		handler.opened(sock);
	}

	@Override
	public void closed(Socket sock)
	{
		synchronized (receiveLock)
		{
			if (open)
			{
				open = false;
				bufs.offer(EMPTY);
			}
		}

		handler.closed(sock);
	}

	@Override
	public void error(Throwable exc, Socket sock)
	{
		handler.error(exc, sock);
	}

	public ByteBuffer getNextBuf(long timeout) throws IOException
	{

		synchronized (readLock)
		{
			try
			{
				ByteBuffer buf;
				for (;;)
				{
					if (!open)
						return null;

					buf = bufs.poll(timeout, TimeUnit.MILLISECONDS);

					if (buf == null)
						return null;

					bufs.push(buf);

					if (!open)
						return null;
					
					if (!buf.hasRemaining())
					{
						bufs.pop();
					}
					else
						break;
				}
				return buf;
			}
			catch (InterruptedException e)
			{
				throw new IOException(e);
			}
		}
	}

	@Override
	public synchronized int read() throws IOException
	{

		ByteBuffer buf = getNextBuf(30000);

		if (buf == null)
			return -1;

		int rv = buf.get() & 0xFF;

		return rv;
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException
	{
		ByteBuffer buf = getNextBuf(30000);

		if (buf == null)
			return -1;

		int bs = Math.min(len, buf.remaining());
		buf.get(b, off, bs);
		return bs;
	}

	@Override
	public int available() throws IOException
	{
		ByteBuffer peek = bufs.peek();
		if (peek == null)
			return 0;
		
		return peek.remaining();
	}
}
