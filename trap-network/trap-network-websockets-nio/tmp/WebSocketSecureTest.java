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
import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSServerImpl;


public class WebSocketSecureTest extends TestCase
{
	private WSInterface	serverSocket;
	private WebSocketListener clientReader;
	private WebSocketListener wsReader;
	private WSInterface clientSocket;
	private WSServerImpl	ss;
	private boolean ready = false;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		
		System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
		System.setProperty("javax.net.ssl.keyStore", "/sel_Ericcson.pkcs12");  // path to file
		System.setProperty("javax.net.ssl.keyStorePassword", "Ericcson");
		System.setProperty("javax.net.ssl.trustStoreType", "jks");
		
		System.setProperty("javax.net.ssl.trustStore", "/sel_Ericcson.keystore");  // path to file
		System.setProperty("javax.net.ssl.trustStorePassword", "Ericcson");

		this.wsReader = new WebSocketListener();
		
		WSSecurityContext sc = new WSSecurityContext("pkcs12", "src/test/resources/sel_Ericcson.pkcs12", "Ericcson", "jks", "src/test/resources/sel_Ericcson.keystore", "Ericcson");

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
		}, sc);
		
		while (!ready)
			Thread.sleep(100);
		
		this.clientReader = new WebSocketListener();
		this.clientSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), clientReader, WSFactory.VERSION_HIXIE_76, sc);
		this.clientSocket.open();
		wsReader.waitForOpen(100000);
		clientReader.waitForOpen(100000);
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
		assertEquals("Hello!", wsReader.waitForString(2000));
	}
	
	public void testServerMessage() throws Exception
	{
		
		serverSocket.send("Hello!");
		assertEquals("Hello!", clientReader.waitForString(2000));
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
		wsReader.waitForClose(2000);
	}
	
	public void testServerClose() throws Exception
	{
		serverSocket.close();
		clientReader.waitForClose(2000);
	}
	
	public void testServerCloseAfterSomeMessage() throws Exception
	{
		testClientMessage();
		testServerMessage();
		serverSocket.close();
		clientReader.waitForClose(2000);
	}
	
	public void testClientBinaryMessage() throws Exception
	{
		byte[] crap = new byte[] {'H', 'e', 'l', 'l', 'o', '!'};
		clientSocket.send(crap);
		assertTrue(Arrays.equals(crap, wsReader.waitForBytes(2000)));
	}
	
	public void testServerBinaryMessage() throws Exception
	{
		byte[] crap = new byte[] {'H', 'e', 'l', 'l', 'o', '!'};
		serverSocket.send(crap);
		assertTrue(Arrays.equals(crap, clientReader.waitForBytes(2000)));
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
		clientReader.waitForClose(2000);
	}
	
	public void testClientRapidSendAndClose() throws TimeoutException, IOException
	{
		clientSocket.send("I am closing");
		clientSocket.close();
		clientReader.waitForClose(2000);
		assertEquals("I am closing", wsReader.waitForString(3000));
	}
	
	public void testServerNotifyClose() throws TimeoutException
	{
		serverSocket.close();
		wsReader.waitForClose(2000);
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
	
	public void test10000Messages() throws Exception
	{
		String srcString = "HelloTest";
		for (int i = 0; i < 10000; i++)
		{
			clientSocket.send(srcString);
			assertEquals(srcString, wsReader.waitForString(10000));
		}
	}
}

