package com.ericsson.research.transport.ws.hybi10;

import java.util.Arrays;

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

/**
 * The strategy for testing is to open a web socket between a client and a server and add a listener to both ends.
 * That way, whenever we send sth from the client we can assert equality on the payload received in the notify method of the server listener.
 * Similarly, whenever we send sth from the server we can assert equality on the payload received in the notify method of the client listener.
 * 
 * @author emicmra
 *
 */
public class WebSocketHybi10CoreTest extends TestCase {
	private static final int WAIT = 1000;
	private WSInterface serverSocket;
	private WebSocketListener serverListener;
	private WSInterface clientSocket;
	private WebSocketListener clientListener;
	private WSServerImpl ss;

	private boolean ready = false;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.serverListener = new WebSocketListener();

		this.ss = new WSServerImpl("localhost", 0, new WSAcceptListener() {
			public void notifyAccept(WSInterface socket) {
				serverSocket = socket;
				socket.setReadListener(serverListener);
			}

			public void notifyReady(WSServer server) {
				ready = true;
			}
		}, null);

		while (!ready) {
			Thread.sleep(100);
		}

		this.clientListener = new WebSocketListener();
		this.clientSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), clientListener, WSFactory.VERSION_HYBI_10, null);
		this.clientSocket.open();
		serverListener.waitForOpen(WAIT);
		clientListener.waitForOpen(WAIT);
	}

	@Override
	protected void tearDown() throws Exception {
		clientSocket.close();
		serverSocket.close();
		ss.close();
		super.tearDown();
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new RepeatedTest(new TestSuite(WebSocketHybi10CoreTest.class), 10);
	}

	public void testClientMessage() throws Exception {
		clientSocket.send("Hello!");
		assertEquals("Hello!", serverListener.waitForString(WAIT));
	}

	public void testServerMessage() throws Exception {

		serverSocket.send("Hello!");
		assertEquals("Hello!", clientListener.waitForString(1000));
	}

	public void testClientClose() throws Exception {
		clientSocket.close();
		serverListener.waitForClose(1000);
	}

	public void testClientBinaryMessage() throws Exception {
		byte[] crap = new byte[] { 'H', 'e', 'l', 'l', 'o' };
		clientSocket.send(crap);
		assertTrue(Arrays.equals(crap, serverListener.waitForBytes(1000)));
	}

	public void testServerBinaryMessage() throws Exception {
		byte[] crap = new byte[] { 'H', 'e', 'l', 'l', 'o' };
		serverSocket.send(crap);
		assertTrue(Arrays.equals(crap, clientListener.waitForBytes(1000)));
	}

	public void testClientStringThenBinary() throws Exception {
		testClientMessage();
		testClientBinaryMessage();
	}

	public void testClientRequestResponse() throws Exception {
		testClientMessage();
		testServerMessage();
	}

	public void testManyClientMessages() throws Exception {
		for (int i = 0; i < 1000; i++)
			testClientRequestResponse();
	}

}
