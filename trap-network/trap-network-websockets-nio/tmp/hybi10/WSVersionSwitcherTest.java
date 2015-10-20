package com.ericsson.research.transport.ws.hybi10;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSServerImpl;

/**
 * The strategy for testing is to open a web socket between a client and a server and add a listener to both ends. That way, whenever we send sth from
 * the client we can assert equality on the payload received in the notify method of the server listener. Similarly, whenever we send sth from the
 * server we can assert equality on the payload received in the notify method of the client listener.
 * 
 * @author emicmra
 * 
 */
public class WSVersionSwitcherTest extends TestCase {
	private static final int WAIT = 1000;
	private WSInterface serverSocket;
	private WebSocketListener serverListener;
	private WSInterface clientSocket;
	private WebSocketListener clientListener;
	private WSServerImpl ss;

	private boolean ready = false;

	protected void init(int clientVersion) throws Exception, IOException, UnknownHostException, InterruptedException, TimeoutException {
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
		this.clientSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), clientListener, clientVersion, null);
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

	public void testSendMessage_Hixie_75() throws Exception {
		init(WSFactory.VERSION_HIXIE_75);
		
		clientSocket.send("Hello!");
		assertEquals("Hello!", serverListener.waitForString(WAIT));
	}

	public void testSendMessage_Hixie_76() throws Exception {
		init(WSFactory.VERSION_HIXIE_76);
		
		clientSocket.send("Hello!");
		assertEquals("Hello!", serverListener.waitForString(WAIT));
	}

	public void testSendMessage_HyBi_10() throws Exception {
		init(WSFactory.VERSION_HYBI_10);
		
		clientSocket.send("Hello!");
		assertEquals("Hello!", serverListener.waitForString(WAIT));
	}

}
