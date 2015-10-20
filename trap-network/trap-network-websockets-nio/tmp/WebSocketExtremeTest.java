package com.ericsson.research.transport.ws2;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSServerImpl;

public class WebSocketExtremeTest extends TestCase
{
	private WSInterface clientSocket;
	private WebSocketListener clientReader;
	private WebSocketListener wsReader;
	private WSInterface	serverSocket;
	private WSServerImpl	ss;
	private boolean	ready = false;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.wsReader = new WebSocketListener();
		this.ss = new WSServerImpl("localhost", 0, new WSAcceptListener() {
			
			public void notifyAccept(WSInterface socket)
			{
				serverSocket = socket;
				socket.setReadListener(wsReader);
			}

			public void notifyReady(WSServer server)
			{
				ready = true;
			}
		}, null);
		
		while (!ready )
			Thread.sleep(100);
		
		this.clientReader = new WebSocketListener();
		this.clientSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), clientReader, WSFactory.VERSION_HIXIE_76, null);
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
		assertTrue(Arrays.equals(bs, wsReader.waitForBytes(1000)));
		
	}

	public void testSingleBytes() throws Exception
	{
		// Test all single bytes
		for (int i = 0; i < 256; i++)
		{
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
		
		StringBuffer sb = new StringBuffer();
		
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

