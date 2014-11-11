package com.ericsson.research.transport;

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
import java.security.NoSuchAlgorithmException;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class SocketTest 
 extends TestCase
{
	private NioManager	cm;
	
	private byte[] rcv = null;
	
	private byte[]	sent;
	
	private ManagedSocket	client;
	
	private ManagedServerSocket	server;
	
	@SuppressWarnings("unused")
	// Prevents serverSock from being prematurely GCd
	private ManagedSocket	serverSock;
	
	private int	rcvBytes;
	
	protected void setUp() throws Exception
	{
		super.setUp();
		this.cm = NioManager.instance();
	}
	
	protected void tearDown() throws Exception
	{
		this.cm.stop();
		Thread.sleep(1000);
		super.tearDown();
	}
	
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public SocketTest(String testName)
	{
		super(testName);
	}
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite(SocketTest.class);
	}
	
	private static int i = 0;
	
	public synchronized void stestConnection() throws IOException, InterruptedException
	{
		this.server = new ManagedServerSocket();
		this.client = new ManagedSocket();
		this.serverSock = null;
		
		this.sent = ("Hello"+(i++)).getBytes("UTF-8");
		final SocketTest mt = this;
		this.rcv = null;
		
		this.rcvBytes = 0;
		
		this.server.registerClient(new ManagedServerSocketClient() {
			
			public void notifyAccept(final ManagedSocket socket)
			{
				SocketTest.this.serverSock = socket;
				socket.registerClient(new ManagedSocketClient() {
					
					public void notifySocketData(byte[] data, int size)
					{
						try
						{
							System.out.println("Received " + size + " bytes on server; echoing");
							socket.write(data, size);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						
					}
					
					public void notifyError(Exception e)
					{
						System.out.println("ERR");
						
					}
					
					public void notifyDisconnected()
					{
						System.out.println("DC");
						
					}
					
					public void notifyConnected()
					{
					}
				});
				
			}
			
			public void notifyBound(ManagedServerSocket socket)
			{
				//System.out.println("I am bound");
				// Allowing this one to remain synchronous to test both entrant (SSLSocket) and reentrant (this) calls.
				SocketTestCommons.connect(SocketTest.this.cm, SocketTest.this.client, SocketTest.this.server.getInetAddress());
			}
			
			public void notifyError(Exception e)
			{
				/*mt.rcv = "error".getBytes();
				synchronized(mt)
				{
					mt.notify();
				}*/
				
				// Try to rebind
				//System.out.println("Trying to rebind...");
				try
				{
					SocketTestCommons.bind(SocketTest.this.cm, SocketTest.this.server);
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
				
			}
		});
		
		this.client.registerClient( new ManagedSocketClient() {
			
			public void notifySocketData(byte[] data, int size)
			{
				for (int i = 0; i < size; i++)
					Assert.assertEquals(SocketTest.this.sent[SocketTest.this.rcvBytes+i], data[i]);
				
				SocketTest.this.rcvBytes += size;
				
				if (SocketTest.this.rcvBytes == SocketTest.this.sent.length)
				{
					mt.rcv = new byte[]{0};
					synchronized(mt)
					{
						mt.notify();
					}
				}
			}
			
			public void notifyError(Exception e)
			{
				System.out.println("Errorr");
				
			}
			
			public void notifyDisconnected()
			{
			}
			
			public void notifyConnected()
			{
				try
				{
					SocketTest.this.client.write(SocketTest.this.sent);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
		});
		
		SocketTestCommons.bind(this.cm, this.server);
		
		//client.connect(addr);
		
		synchronized(mt)
		{
			while(mt.rcv == null)
				mt.wait();
		}
		
		this.server = null;
		this.client = null;
		this.serverSock = null;
	}
	
	public synchronized void stestThreads() throws Exception
	{
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while(tg.getParent() != null) {
			tg = tg.getParent();
		}
		//Thread[] t = new Thread[tg.activeCount()];
		//tg.enumerate(t);
		
		int initialThreads = tg.activeCount();
		
		for (int i = 0; i < 100; i++)
		{
			this.stestConnection();
			//System.out.println(tg.activeCount());
		}
		
		// Allow some time for all threads to die
		for (int j = 0; j < 20; j++)
		{
			System.gc();
			Thread.sleep(100);
		}
		
		// Invoke GC again to clean up any stray objects.
		System.gc();
		
		// Allow some time for all threads to die
		for (int j = 0; j < 20; j++)
		{
			System.gc();
			Thread.sleep(100);
		}
		
		// Invoke GC again to clean up any stray objects.
		System.gc();
		int finalThreads = tg.activeCount();
		System.out.println("Initial: " +initialThreads);
		System.out.println("Final: " +finalThreads);
		assertTrue(initialThreads >= finalThreads);
		
	}
	
	public synchronized void testBandwidth() throws NoSuchAlgorithmException, IOException, InterruptedException
	{
		this.server = new ManagedServerSocket();
		this.client = new ManagedSocket();
		this.serverSock = null;
		
		final int totalBytes = 10000000;
		final byte[] bytes = new byte[totalBytes];
		
		for (int j=0; j<totalBytes; j++)
			bytes[j] = (byte) (j % 200);
		
		this.rcvBytes = 0;
		final SocketTest mt = this;
		this.rcv = null;
		
		this.server.registerClient(new ManagedServerSocketClient() {
			
			public void notifyAccept(final ManagedSocket socket)
			{
				SocketTest.this.serverSock = socket;
				socket.registerClient(new ManagedSocketClient() {
					
					public void notifySocketData(byte[] data, int size)
					{

						for (int i = 0; i < size; i++)
							Assert.assertEquals(bytes[SocketTest.this.rcvBytes+i], data[i]);
						
						SocketTest.this.rcvBytes += size;
						System.out.println("Verified " + SocketTest.this.rcvBytes);
						
						if (SocketTest.this.rcvBytes == totalBytes)
						{
							mt.rcv = new byte[]{0};
							synchronized(mt)
							{
								mt.notify();
							}
						}
						
					}
					
					public void notifyError(Exception e)
					{
						System.out.println("ERR");
						
					}
					
					public void notifyDisconnected()
					{
						System.out.println("DC");
						
					}
					
					public void notifyConnected()
					{
					}
				});
			}
			
			public void notifyBound(ManagedServerSocket socket)
			{
				new Thread() {
					public void run() {
						//System.out.println("I am bound");
						SocketTestCommons.connect(SocketTest.this.cm, SocketTest.this.client, SocketTest.this.server.getInetAddress());
					}
				}.start();
			}
			
			public void notifyError(Exception e)
			{
				/*mt.rcv = "error".getBytes();
				synchronized(mt)
				{
					mt.notify();
				}*/
				
				// Try to rebind
				//System.out.println("Trying to rebind...");
				try
				{
					SocketTestCommons.bind(SocketTest.this.cm, SocketTest.this.server);
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
				
			}
		});
		
		this.client.registerClient( new ManagedSocketClient() {
			
			public void notifySocketData(byte[] data, int size)
			{
			}
			
			public void notifyError(Exception e)
			{
				System.out.println("Errorr");
				
			}
			
			public void notifyDisconnected()
			{
			}
			
			public void notifyConnected()
			{
				try
				{
					SocketTest.this.client.write(bytes);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
		});
		
		SocketTestCommons.bind(this.cm, this.server);
		
		//client.connect(addr);
		
		synchronized(mt)
		{
			while(mt.rcv == null)
				mt.wait();
		}
		
		this.server = null;
		this.client = null;
		this.serverSock = null;
	}
}
