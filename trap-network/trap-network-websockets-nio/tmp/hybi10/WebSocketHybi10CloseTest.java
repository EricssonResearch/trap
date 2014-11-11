package com.ericsson.research.transport.ws.hybi10;

import junit.framework.TestCase;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSConstants;
import com.ericsson.research.transport.ws.spi.WSServerImpl;

/**
 * The strategy for testing is to open a web socket between a client and a server and add a listener to both ends. That way, whenever we send sth from
 * the client we can assert equality on the payload received in the notify method of the server listener. Similarly, whenever we send sth from the
 * server we can assert equality on the payload received in the notify method of the client listener.
 * 
 * @author emicmra
 * 
 */
public class WebSocketHybi10CloseTest extends TestCase {
	protected static final int WAIT = 1000;
	protected WSInterface serverSocket;
	protected WebSocketListener serverListener;
	protected WSInterface clientSocket;
	protected WebSocketListener clientListener;
	protected WSServerImpl ss;

	private boolean ready = false;

	@Override
	protected void setUp() throws Exception {
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
		clientListener.notifyClose();
		serverListener.notifyClose();
		ss.close();
	}
	
	public void testClientInitiatedClose() throws Exception {
		clientSocket.close();
		serverListener.waitForClose(WAIT); // Server should receive CLOSE frame
		clientListener.waitForClose(WAIT); // Client should receive response CLOSE frame
	}

	public void testServerInitiatedClose() throws Exception {
		serverSocket.close();
		clientListener.waitForClose(WAIT); // Client should receive CLOSE frame
		serverListener.waitForClose(WAIT); // Server should receive response CLOSE frame
	}
	
	public void testClientInitiatedCloseJustAfterSendingMessage() throws Exception {
		String s = "This should make it to the server listener before close terminates the connection.";
		clientSocket.send(s);
		clientSocket.close();
		serverListener.waitForClose(WAIT); // Server should receive CLOSE frame
		clientListener.waitForClose(WAIT); // Client should receive response CLOSE frame
		assertEquals(s, serverListener.waitForString(WAIT));
	}

	public void testServerInitiatedCloseJustAfterSendingMessage() throws Exception {
		String s = "This should make it to the client listener before close terminates the connection.";
		serverSocket.send(s);
		serverSocket.close();
		clientListener.waitForClose(WAIT); // Client should receive CLOSE frame
		serverListener.waitForClose(WAIT); // Server should receive response CLOSE frame
		assertEquals(s, clientListener.waitForString(WAIT));
	}	

	public void testClientAndServerInitiatedClose() throws Exception {
		clientSocket.close();
		serverSocket.close();
		clientListener.waitForClose(WSConstants.CLOSE_TIMEOUT + 1000); // Client should timeout waiting for CLOSE response frame OR receive a response FRAME
		serverListener.waitForClose(WSConstants.CLOSE_TIMEOUT + 1000); // Server should timeout waiting for CLOSE response frame OR receive a response FRAME
	}

	public void testBreaks() throws Exception {
		serverSocket.close();
		clientSocket.send("Late message!!!");
		clientListener.waitForClose(WAIT); // Client should receive CLOSE frame
		serverListener.waitForClose(WAIT); // Server should receive response CLOSE frame
	}


}
