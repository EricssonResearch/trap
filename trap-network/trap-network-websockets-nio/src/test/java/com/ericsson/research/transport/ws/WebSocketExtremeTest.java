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


public class WebSocketExtremeTest extends TestCase
{
	private WSInterface	clientSocket;
    private WSDataListener wsReader;
	private WSInterface	serverSocket;
	private WSServer ss;
	private boolean	ready;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.wsReader = new WSDataListener();

		this.ready = false;
		
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
		}, null);
		
		while(!ready)
		{
			Thread.sleep(100);
		}
        WSDataListener clientReader = new WSDataListener();
		this.clientSocket = WSFactory.createWebSocketClient(ss.getURI(), clientReader, WSFactory.VERSION_HIXIE_75, null);
		this.clientSocket.open();
		wsReader.waitForOpen(1000);
		clientReader.waitForOpen(1000);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
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
	public WebSocketExtremeTest( String testName ) throws IOException
	{
		super( testName );
	}
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( WebSocketExtremeTest.class );
	}
	
	public void testLongStringMessage() throws Exception
	{
		String reallyLongString = "SupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydociousSupercalifradgalisticexpialydocious";
		echoTest(reallyLongString);
	}
	
	private void echoTest(String reallyLongString) throws IOException, TimeoutException
	{
		clientSocket.send(reallyLongString);
		assertEquals(reallyLongString, wsReader.waitForString(1000));
	}

	public void testShortStringMessage() throws Exception
	{
		echoTest("a");
	}

	public void testEmptyMessage() throws Exception
	{
		echoTest("");
	}
	
	public void testEmptyByteArray() throws Exception
	{
		echoTest(new byte[0]);
	}
	
	public void testEmptyByteArrayFirst() throws Exception
	{
		//echoTest(new byte[0]);
		clientSocket.send(new byte[0]);
		echoTest("are you still alive?");
	}
	
	private void echoTest(byte[] bs) throws TimeoutException, IOException
	{
		clientSocket.send(bs);
		assertTrue(Arrays.equals(bs, wsReader.waitForBytes(100000)));
		
	}

	public void testSingleBytes() throws Exception
	{
		// Test all single bytes
		for (int i = 0; i < 256; i++)
		{
			//System.out.println(i);
			echoTest(new byte[] {(byte) i});
		}
	}

	public void testSingleBytesCombinations() throws Exception
	{
		// Test all single bytes
		for (int i = 0; i < 256; i++)
		{
			echoTest(new byte[] {(byte) i});

			// Test all single bytes
			for (int j = i%16; j < 256; j=j+16)
			{
				echoTest(new byte[] {(byte) j});
			}
		}
	}
	
	public void testAllBytes() throws Exception
	{
		byte[] all = new byte[256];

		for (int i = 0; i < 256; i++)
		{
			all[i] = (byte) i;
		}
		
		echoTest(all);
	}

	public void testReallyManyBytes() throws Exception
	{

		byte[] all = new byte[65537];

		for (int i = 0; i < 65537; i++)
		{
			all[i] = (byte) i;
		}
		
		echoTest(all);
	}
	
	public void testReallyLongString() throws TimeoutException, IOException
	{
		
		char[] src = new char[1000000];  // TODO: This will fail with 1,000,000. Fix it.
		
		for (int i = 0; i < 1000000; i++)
			src[i] = (char) (Math.random()*253+1);
		
		String srcString = new String(src);
		clientSocket.send(srcString);
		assertEquals(srcString, wsReader.waitForString(5000));
		
	}
	
	public void testGrowingString() throws TimeoutException, IOException
	{
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < 40000; i++)
			sb.append((char)i);
		
		String srcString = sb.toString();
		clientSocket.send(srcString);
		String dstString = wsReader.waitForString(5000);
		
		for (int j = 0; j < 40000; j++)
		{
			char srcChar = srcString.charAt(j);
			char dstChar = dstString.charAt(j);
			
			if (srcChar != dstChar)
				System.out.println("Error: Expected " + ((int)srcChar) + " but received " + ((int)dstChar));
		}
		
		assertEquals(srcString, dstString);
		
	}
}

