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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ericsson.research.trap.impl.TrapMessageImpl;
import com.ericsson.research.trap.nio.ServerSocket;
import com.ericsson.research.trap.nio.ServerSocket.ServerSocketHandler;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;
import com.ericsson.research.trap.nio.SocketFactory;
import com.ericsson.research.trap.spi.TrapMessage;

/**
 * Unit test for simple App.
 */
 @RunWith(Parameterized.class)
public class SocketTest
{
	private byte[]	      rcv	= null;

	private byte[]	      sent;

	private int	          rcvBytes;

	private static int	  i	    = 0;

	private SocketFactory	mgr	= new Nio2SocketFactory();

	private ServerSocket	server;

	protected Socket	  remote;

	private Socket	      client;

	private ByteBuffer	  sendBuf;

	@Parameters
	public static List<Object[]> params()
	{
		return Arrays.asList(new Object[10][0]);
	}

	@Test(timeout=15000)
	public void testConnection() throws IOException, InterruptedException
	{
		this.client = mgr.client();

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket attachment)
			{

			}

			@Override
			public void received(ByteBuffer data, Socket sock)
			{

				// System.out.println("Received back to client!");

				byte[] buf = new byte[data.remaining()];

				data.get(buf);

				for (int i = 0; i < buf.length; i++)
					Assert.assertEquals(SocketTest.this.sent[SocketTest.this.rcvBytes + i], buf[i]);

				SocketTest.this.rcvBytes += buf.length;

				if (SocketTest.this.rcvBytes == SocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SocketTest.this)
					{
						SocketTest.this.notify();
					}
				}
			}

			@Override
			public void opened(Socket sock)
			{
				// System.out.println("OPEN");
				sock.send(sendBuf);
			}

			@Override
			public void error(Throwable exc, Socket sock)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void closed(Socket sock)
			{
				// TODO Auto-generated method stub

			}
		});

		this.server = mgr.server(new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SocketTest.this.remote = sock;
				sock.setHandler(new SocketTestUtil.EchoHandler(sock));
			}
		});

		this.sent = ("Hello" + (i++)).getBytes("UTF-8");
		this.sendBuf = ByteBuffer.wrap(sent);
		final SocketTest mt = this;
		this.rcv = null;

		this.rcvBytes = 0;

		server.listen(0);
		client.open(server.getInetAddress());

		synchronized (mt)
		{
			while (mt.rcv == null)
				mt.wait();
		}

		this.server = null;
		this.client = null;
		this.remote = null;
	}

	@After
	public void printBytes()
	{
		// System.out.println("Received " + rcvBytes);
	}

	@Test(timeout = 15000)
	public void testBandwidth() throws IOException, InterruptedException
	{

		final int totalBytes = 10000000;
		sent = new byte[totalBytes];
		this.sendBuf = ByteBuffer.wrap(sent);

		for (int j = 0; j < totalBytes; j++)
			sent[j] = (byte) (j % 200);

		this.rcvBytes = 0;
		this.rcv = null;

		this.client = mgr.client();

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket sock)
			{
				sock.send(sendBuf);
			}

			@Override
			public void received(ByteBuffer data, Socket sock)
			{

				byte[] buf = new byte[data.remaining()];

				data.get(buf);

				for (int i = 0; i < buf.length; i++)
					Assert.assertEquals("Failed at index " + i, SocketTest.this.sent[SocketTest.this.rcvBytes + i], buf[i]);

				SocketTest.this.rcvBytes += buf.length;

				if (SocketTest.this.rcvBytes == SocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SocketTest.this)
					{
						SocketTest.this.notify();
					}
				}
			}

			@Override
			public void opened(Socket sock)
			{
				sock.send(sendBuf);
			}

			@Override
			public void error(Throwable exc, Socket sock)
			{

				exc.printStackTrace();
			}

			@Override
			public void closed(Socket sock)
			{
				// TODO Auto-generated method stub

			}
		});

		this.server = mgr.server(new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{

				exc.printStackTrace();
			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SocketTest.this.remote = sock;
				sock.setHandler(new SocketTestUtil.EchoHandler(sock));
			}
		});

		server.listen(0);
		client.open(server.getInetAddress());

		synchronized (this)
		{
			while (this.rcv == null)
				this.wait();
		}

		this.server = null;
		this.client = null;
		this.remote = null;
		sent = null;
		sendBuf = null;
	}

	@Test(timeout=30000)
	public void testThroughput() throws IOException, InterruptedException
	{
		for (int i = 0; i < 10; i++)
			stressBandwidth();
	}

	public void stressBandwidth() throws IOException, InterruptedException
	{

		final int totalBytes = 10000000;
		sent = new byte[totalBytes];
		this.sendBuf = ByteBuffer.wrap(sent);

		for (int j = 0; j < totalBytes; j++)
			sent[j] = (byte) (j % 200);

		this.rcvBytes = 0;
		this.rcv = null;

		this.client = mgr.client();

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket sock)
			{
				sock.send(sendBuf);
			}

			@Override
			public void received(ByteBuffer data, Socket sock)
			{

				SocketTest.this.rcvBytes += data.remaining();

				if (SocketTest.this.rcvBytes == SocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SocketTest.this)
					{
						SocketTest.this.notify();
					}
				}
			}

			@Override
			public void opened(Socket sock)
			{
				sock.send(sendBuf);
			}

			@Override
			public void error(Throwable exc, Socket sock)
			{
				exc.printStackTrace();
			}

			@Override
			public void closed(Socket sock)
			{
				// TODO Auto-generated method stub

			}
		});

		this.server = mgr.server(new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				exc.printStackTrace();
			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SocketTest.this.remote = sock;
				sock.setHandler(new SocketTestUtil.EchoHandler(sock));
			}
		});

		server.listen(0);
		client.open(server.getInetAddress());

		synchronized (this)
		{
			while (this.rcv == null)
				this.wait();
		}

		this.server = null;
		this.client = null;
		this.remote = null;
		sent = null;
		sendBuf = null;
	}

	ByteArrayOutputStream	bos	      = new ByteArrayOutputStream();

	/**
	 * Call this when data is received.
	 * 
	 * @param data
	 */

	// There is no reason for receive to be publicly synchronized other than
	// prevent it from
	// being called concurrently. We can order the requests using a lock better
	private Object	      receiveLock	= new Object();
	
	int lastId = 0;

	public int receive(byte[] data, int offset, int length)
	{

		int msgs = 0;

		synchronized (this.receiveLock)
		{
			int consumed = 0;

			// if (new String(data).contains("HTTP"))
			// System.out.println("Lolwut?");

			try
			{
				// We need to handle the case where message data is spread out
				// over two or more incoming data blobs (e.g. socket, udp,
				// etc)...
				// Therefore, we'll need to do some buffer shuffling.

				this.bos.write(data, offset, length);
				byte[] dataArray = this.bos.toByteArray();

				do
				{

					// Saving these for posterity. Essentially, this was an
					// attempt to check the integrity of the buffers.
					// There may be a case to be made for a Trap Checksum, but
					// I'm not going all the way there yet.
					// if (dataArray[consumed] > 32)
					// System.out.println("Potential break in the system here");

					TrapMessage m = new TrapMessageImpl();
					int thisLoop = m.deserialize(dataArray, consumed, dataArray.length - consumed);

					if (thisLoop == -1)
					{

						// if (dataArray[consumed] > 32 || dataArray[consumed]
						// == 5)
						// System.out.println("Potential break in the system here");

						break;
					}
					
					if (m.getMessageId() != lastId+1)
						System.out.println(lastId + " | " + m.getMessageId());
					
					lastId = m.getMessageId();
					
					msgs++;

					consumed += thisLoop;
				}
				while (consumed < dataArray.length);

				if (consumed > 0)
				{
					this.bos = new ByteArrayOutputStream();
					try
					{
						this.bos.write(dataArray, consumed, dataArray.length - consumed);
					}
					catch (Throwable t)
					{
						System.out.println(t);
					}
				}
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedOperationException e)
			{
				e.printStackTrace();
			}
		}

		return msgs;
	}

    @Test(timeout=30000)
	public void testTrapMessage() throws IOException, InterruptedException
	{

		final ConcurrentLinkedQueue<ByteBuffer> msgs = new ConcurrentLinkedQueue<>();

		final int totalMessages = 65535;
		for (int i = 0; i < totalMessages; i++)
			msgs.add(ByteBuffer.wrap(new TrapMessageImpl().setChannel(2).setMessageId(i).serialize()));

		this.rcvBytes = 0;
		this.rcv = null;

		this.client = mgr.client();

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket sock)
			{
				synchronized (msgs)
				{
					for (;;)
					{
						ByteBuffer buf = msgs.peek();

						if (buf == null)
							return;
						
						if (!buf.hasRemaining())
						{
							msgs.poll();
							continue;
						}
						sock.send(buf);
						
						if (buf.hasRemaining())
							return;
					}
				}

			}

			@Override
			public void received(ByteBuffer data, Socket socket)
			{

				byte[] rcvBuf = new byte[4096];

				while (data.hasRemaining())
				{
					int loopData = Math.min(rcvBuf.length, data.remaining());
					data.get(rcvBuf, 0, loopData);
					rcvBytes += receive(rcvBuf, 0, loopData);
				}

				if (rcvBytes == totalMessages)
				{
					synchronized (SocketTest.this)
					{
						rcv = new byte[0];
						SocketTest.this.notify();
					}
				}

			}

			@Override
			public void opened(Socket sock)
			{
				sent(sock);
			}

			@Override
			public void error(Throwable exc, Socket sock)
			{
				exc.printStackTrace();
			}

			@Override
			public void closed(Socket sock)
			{
				// TODO Auto-generated method stub

			}
		});

		this.server = mgr.server(new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				exc.printStackTrace();
			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SocketTest.this.remote = sock;
				sock.setHandler(new SocketTestUtil.EchoHandler(sock));
			}
		});

		server.listen(0);
		client.open(server.getInetAddress());

		synchronized (this)
		{
			while (this.rcv == null)
				this.wait();
		}

		this.server = null;
		this.client = null;
		this.remote = null;
		sent = null;
		sendBuf = null;
	}

}
