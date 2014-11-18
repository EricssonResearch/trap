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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;

import junit.framework.Assert;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.ericsson.research.trap.nio.ServerSocket;
import com.ericsson.research.trap.nio.SocketFactory;
import com.ericsson.research.trap.nio.ServerSocket.ServerSocketHandler;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;

/**
 * Unit test for simple App.
 */
//@RunWith(Parameterized.class)
public class SSLSocketTest
{
	private byte[]	          rcv	= null;

	private byte[]	          sent;

	private int	              rcvBytes;

	private static int	      i	  = 0;

	private static SSLContext	sslc;

	private SocketFactory	      mgr	= new Nio2SocketFactory();

	private ServerSocket	  server;

	protected Socket	      remote;

	private Socket	          client;

	private int	              sentBytes;

	private ByteBuffer	      sendBuf;

	@Parameters
	public static List<Object[]> params()
	{
		return Arrays.asList(new Object[10][0]);
	}

	@BeforeClass
	public static void setupSsl()
	{
		sslc = SSLUtil.getContext(new SSLMaterial("pkcs12", "src/test/resources/sel_Ericcson.pkcs12", "Ericcson"), new SSLMaterial("jks",
		        "src/test/resources/sel_Ericcson.keystore", "Ericcson"));
	}

	@Test(timeout=15000)
	public void testConnection() throws IOException, InterruptedException
	{
		this.client = mgr.sslClient(sslc);

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket attachment)
			{
				attachment.send(sendBuf);
			}

			@Override
			public void received(ByteBuffer data, Socket sock)
			{

				// System.out.println("Received back to client!");

				byte[] buf = new byte[data.remaining()];

				data.get(buf);

				for (int i = 0; i < buf.length; i++)
					Assert.assertEquals(SSLSocketTest.this.sent[SSLSocketTest.this.rcvBytes + i], buf[i]);

				SSLSocketTest.this.rcvBytes += buf.length;

				if (SSLSocketTest.this.rcvBytes == SSLSocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SSLSocketTest.this)
					{
						SSLSocketTest.this.notify();
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

		this.server = mgr.sslServer(sslc, new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SSLSocketTest.this.remote = sock;
				sock.setHandler(new SocketTestUtil.EchoHandler(sock));
			}
		});

		this.sent = ("Hello" + (i++)).getBytes("UTF-8");
		this.sendBuf = ByteBuffer.wrap(sent);
		final SSLSocketTest mt = this;
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

		this.client = mgr.sslClient(sslc);

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
					Assert.assertEquals("Failed at index " + i, SSLSocketTest.this.sent[SSLSocketTest.this.rcvBytes + i], buf[i]);

				SSLSocketTest.this.rcvBytes += buf.length;

				if (SSLSocketTest.this.rcvBytes == SSLSocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SSLSocketTest.this)
					{
						SSLSocketTest.this.notify();
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

		this.server = mgr.sslServer(sslc, new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SSLSocketTest.this.remote = sock;
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

	@Test
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

		this.client = mgr.sslClient(sslc);

		client.setHandler(new SocketHandler()
		{

			@Override
			public void sent(Socket sock)
			{

				if (sentBytes < sent.length)
				{
					sock.send(sendBuf);
				}

			}

			@Override
			public void received(ByteBuffer data, Socket sock)
			{

				SSLSocketTest.this.rcvBytes += data.remaining();

				if (SSLSocketTest.this.rcvBytes == SSLSocketTest.this.sent.length)
				{
					rcv = new byte[] { 0 };
					synchronized (SSLSocketTest.this)
					{
						SSLSocketTest.this.notify();
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
				// TODO Auto-generated method stub

			}

			@Override
			public void closed(Socket sock)
			{
				// TODO Auto-generated method stub

			}
		});

		this.server = mgr.sslServer(sslc, new ServerSocketHandler()
		{

			@Override
			public void error(Throwable exc, ServerSocket ss)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void accept(Socket sock, ServerSocket ss)
			{
				SSLSocketTest.this.remote = sock;
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
