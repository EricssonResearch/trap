package com.ericsson.research.transport.ws2;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import junit.extensions.RepeatedTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSServerImpl;


public class WebSocketCoreTest extends TestCase
{
	private WSInterface	serverSocket;
	private WebSocketListener clientReader;
	private WebSocketListener wsReader;
	private WSInterface clientSocket;
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
	public WebSocketCoreTest( String testName ) throws IOException
	{
		super( testName );
	}
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new RepeatedTest(new TestSuite( WebSocketCoreTest.class ), 10);
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
	
	public void testClientRapidSendAndClose() throws TimeoutException, IOException
	{
		clientSocket.send("I am closing");
		clientSocket.close();
		clientReader.waitForClose(1000);
		assertEquals("I am closing", wsReader.waitForString(3000));
	}
	
	public void testServerNotifyClose() throws TimeoutException
	{
		serverSocket.close();
		wsReader.waitForClose(1000);
	}
	
	public void testManyClientMessages() throws Exception
	{
		for (int i = 0; i < 1000; i++)
			testClientRequestResponse();
	}
	
	
	
	public void _testManyLongMessages() throws Exception {
		String longMessage = createLongTextMessage(120);
		for (int i = 0; i < 1000; i++) {
			clientSocket.send(longMessage);
			assertEquals(longMessage, wsReader.waitForString(1000));
		}
	}
	
	// Medium length test (446 characters) string for testing send with alternative two for specifying length (2 bytes length field in frame)
	public static final String MEDIUM_TEXT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
	private String createLongTextMessage(int num) {
		String s = MEDIUM_TEXT_MESSAGE;
		
		for (int i = 0; i < num; i++) {
			s += MEDIUM_TEXT_MESSAGE;
		}
		
		return s;
	}

	
}

