package com.ericsson.research.transport.ws.hybi10;

import java.util.Arrays;

import junit.framework.TestCase;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSSecurityContext;
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
public class WebSocketHybi10SecureTest extends TestCase {
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

		WSSecurityContext sc = new WSSecurityContext("pkcs12", "src/test/resources/sel_Ericcson.pkcs12", "Ericcson", "jks", "src/test/resources/sel_Ericcson.keystore", "Ericcson");
		
		this.ss = new WSServerImpl("localhost", 0, new WSAcceptListener() {
			public void notifyAccept(WSInterface socket) {
				serverSocket = socket;
				socket.setReadListener(serverListener);
			}

			public void notifyReady(WSServer server) {
				ready = true;
			}
			// The 4th argument = true will trigger SSL
		}, sc);

		while (!ready) {
			Thread.sleep(100);
		}

		this.clientListener = new WebSocketListener();
		this.clientSocket = new WSImplementationHybi10(ss.getURI(), clientListener, sc) {
			// Override the pong handling so that we can verify that the ping method is actually returning the payload in the pong.
			// The real handler does nothing.
			protected void handlePong(java.io.ByteArrayInputStream bais) {
				byte[] pongPayload = FrameParser.parseSingleFrameMessagePayload(bais);
				listener.notifyMessage(pongPayload);
			};
		}.open();
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

	public void testPing() throws Exception {
		((WSImplementationHybi10) clientSocket).ping();
		assertTrue(Arrays.equals(new byte[] { 'P', 'i', 'n', 'g' }, clientListener.waitForBytes(1000)));
	}

}
