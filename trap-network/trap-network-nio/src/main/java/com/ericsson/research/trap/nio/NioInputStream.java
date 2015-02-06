package com.ericsson.research.trap.nio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.ericsson.research.trap.nio.Socket.SocketHandler;

public class NioInputStream extends InputStream implements SocketHandler
{
	private Object	                receiveLock	= new Object();
	LinkedBlockingDeque<ByteBuffer>	bufs	    = new LinkedBlockingDeque<ByteBuffer>();
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
				if (bufs != null)
					for (ByteBuffer b : bufs)
						handler.received(b, sock);
				bufs = null;
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
			if (bufs != null)
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
		handler.closed(sock);
	}

	@Override
	public void error(Throwable exc, Socket sock)
	{
		handler.error(exc, sock);
	}

	public ByteBuffer getNextBuf(long timeout) throws IOException
	{

		try
		{
			ByteBuffer buf;
			for (;;)
			{
                if (bufs == null)
                    return null;
                
				buf = bufs.poll(timeout, TimeUnit.MILLISECONDS);
				
				if (bufs == null)
					return null;
				
				if (buf == null)
				    return null;
				
				bufs.push(buf);
				if (!buf.hasRemaining())
				{
					if (!bufs.isEmpty())
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

	@Override
	public synchronized int read() throws IOException
	{
		ByteBuffer buf = getNextBuf(30000);
		
		if (buf == null)
			return -1;
		
		return buf.get();
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
		if (bufs.size() > 1)
			return 1;

		return bufs.peek().remaining();
	}
}
