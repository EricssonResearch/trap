package com.ericsson.research.transport.ws;

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
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class WebSocketSecureTest extends TestCase
{
	private WSInterface	serverSocket;
	private WSDataListener clientReader;
	private WSDataListener wsReader;
	private WSInterface clientSocket;
	private WSServer ss;
	private boolean	ready;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.wsReader = new WSDataListener();

		this.ready = false;

		WSSecurityContext sc = new WSSecurityContext("pkcs12", "src/test/resources/sel_Ericcson.pkcs12", "Ericcson", "jks", "src/test/resources/sel_Ericcson.keystore", "Ericcson");

		ss = WSFactory.createWebSocketServer("localhost", 0, new WSAcceptListener() {
			
			public void notifyAccept(WSInterface socket)
			{
				serverSocket = socket;
				socket.setReadListener(wsReader);
			}

			public void notifyReady(WSServer server)
			{
				ready = true;
			}

			public void notifyError(Throwable t) {
				t.printStackTrace();
			}
		}, sc);
		
		while(!ready)
		{
			Thread.sleep(100);
		}
		
		System.out.println(ss.getURI());
		this.clientReader = new WSDataListener();
		this.clientSocket = WSFactory.createWebSocketClient(ss.getURI(), clientReader, WSFactory.VERSION_HIXIE_75, sc);
		this.clientSocket.open();
		wsReader.waitForOpen(100000);
		clientReader.waitForOpen(1000);
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		clientSocket.close();
		serverSocket.close();
		ss.close();
 		super.tearDown();
	}
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public WebSocketSecureTest( String testName ) throws IOException
	{
		super( testName );
	}
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( WebSocketSecureTest.class );
	}
	
	public void testClientMessage() throws Exception
	{
		clientSocket.send("Hello!");
		assertEquals("Hello!", wsReader.waitForString(1000));
	}
	
	public void testServerMessage() throws Exception
	{
		
		serverSocket.send("Hello!");
		assertEquals("Hello!", clientReader.waitForString(1000));
	}
	
	public void testClientClose() throws Exception
	{
		clientSocket.close();
		wsReader.waitForClose(1000);
	}
	
	public void testClientCloseAfterSomeMessage() throws Exception
	{
		testServerMessage();
		testClientMessage();
		clientSocket.close();
		wsReader.waitForClose(1000);
	}
	
	public void testServerClose() throws Exception
	{
		serverSocket.close();
		clientReader.waitForClose(1000);
	}
	
	public void testServerCloseAfterSomeMessage() throws Exception
	{
		testClientMessage();
		testServerMessage();
		serverSocket.close();
		clientReader.waitForClose(1000);
	}
	
	public void testClientBinaryMessage() throws Exception
	{
		byte[] crap = new byte[] {'H', 'e', 'l', 'l', 'o', '!'};
		clientSocket.send(crap);
		assertTrue(Arrays.equals(crap, wsReader.waitForBytes(1000)));
	}
	
	public void testServerBinaryMessage() throws Exception
	{
		byte[] crap = new byte[] {'H', 'e', 'l', 'l', 'o', '!'};
		serverSocket.send(crap);
		assertTrue(Arrays.equals(crap, clientReader.waitForBytes(1000)));
	}
	
	public void testClientStringThenBinary() throws Exception
	{
		testClientMessage();
		testClientBinaryMessage();
	}
	
	public void testClientRequestResponse() throws Exception
	{
		testClientMessage();
		testServerMessage();
	}
	
	public void testClientNotifyClose() throws TimeoutException
	{
		clientSocket.close();
		clientReader.waitForClose(1000);
	}
	
	// In WS1 this test is unreliable
/*	public void testClientRapidSendAndClose() throws TimeoutException, IOException
	{
		clientSocket.send("I am closing");
		clientSocket.close();
		clientReader.waitForClose(1000);
		assertEquals("I am closing", wsReader.waitForString(3000));
	}*/
	
	public void testServerNotifyClose() throws TimeoutException
	{
		serverSocket.close();
		wsReader.waitForClose(1000);
	}
	
	public void testReallyLongString() throws TimeoutException, IOException
	{
		
		char[] src = new char[1000];  // TODO: This will fail with 1,000,000. Fix it.
		
		for (int i = 0; i < 1000; i++)
			src[i] = (char) (Math.random()*253+1);
		
		String srcString = new String(src);
		clientSocket.send(srcString);
		assertEquals(srcString, wsReader.waitForString(5000));
		
	}
	
	public void testReallyLongString2() throws TimeoutException, IOException
	{
		
		char[] src = new char[5*1024];  // TODO: This will fail with 1,000,000. Fix it.
		
		for (int i = 0; i < 5*1024; i++)
			src[i] = (char) (Math.random()*253+1);
		
		String srcString = new String(src);
		clientSocket.send(srcString);
		assertEquals(srcString, wsReader.waitForString(10000));
		
	}
}

