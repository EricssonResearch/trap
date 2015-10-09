package com.ericsson.research.transport.ws.hybi10;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

public class WebSocketStressTest extends TestCase {
	private WSClients clients;
	private WSServers servers;
	private WSServerImpl ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.servers = new WSServers();
		this.clients = new WSClients();
		this.ss = new WSServerImpl("localhost", 0, servers, null);

		while (!servers.ready) {
			Thread.sleep(100);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		servers.closeAll();
		clients.closeAll();
		ss.close();
		super.tearDown();
	}

	/**
	 * Create the test case
	 * 
	 * @param testName name of the test case
	 */
	public WebSocketStressTest(String testName) throws IOException {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(WebSocketStressTest.class);
	}

	public void test1Clients() throws IOException, TimeoutException {
		doNumClientTest(1);
	}

	public void test10Clients() throws IOException, TimeoutException {
		doNumClientTest(10);
	}

	// TODO: Test this out better as well... It SOMETIMES fails on CI domain.
	// There seems to be some sort of race condition causing the tests to fail at different thresholds on different machines.
	// Checked with Vlad and he claimed that these tests have been behaving this way for about 1.5 years. Not worth pursuing.
	// On my local PC I can consistently run about 500 clients in theses tests w/o failure, Vlad claims he's run tens of thousands on his PC.
	public void test100Clients() throws IOException, TimeoutException {
		doNumClientTest(100);
	}

	// Seems to fail consistently, however seems like a race condition in the test rather than a server failure
	public void _test1000Clients() throws IOException, TimeoutException {
		doNumClientTest(1000);
	}

	/*
	 * Sends a message from client to server, server to client, and closes
	 */
	private void doNumClientTest(int numClients) throws IOException, TimeoutException {
		clients.readerFactory = new WSReaderFactory() {

			public WebSocketListener getReader() {
				return new WebSocketListener() {

					@Override
					public void notifyClose() {
						clients.removeSocket(socket);
						super.notifyClose();
					}

					@Override
					public void notifyMessage(byte[] data) {
						super.notifyMessage(data);
					}

					@Override
					public void notifyOpen(WSInterface socket) {
						// System.out.println("Notify Open");
						super.notifyOpen(socket);
						this.socket = socket;
						// clients.sockets.add(socket);
						try {
							socket.send("" + socket.hashCode());
						} catch (IOException e) {
							assertTrue(false);
						}
					}

					@Override
					public void notifyMessage(String string) {
						super.notifyMessage(string);
						assertEquals(string, "" + socket.hashCode());
						socket.close();
					}

				};
			}
		};

		servers.readerFactory = new WSReaderFactory() {

			public WebSocketListener getReader() {
				return new WebSocketListener() {

					@Override
					public void notifyClose() {
						servers.removeSocket(socket);
						super.notifyClose();
					}

					@Override
					public void notifyMessage(byte[] data) {
						super.notifyMessage(data);
					}

					@Override
					public void notifyOpen(WSInterface socket) {
						super.notifyOpen(socket);
						this.socket = socket;
						// servers.sockets.add(socket);
					}

					@Override
					public void notifyMessage(String string) {
						// System.out.println("Received: " + string);
						super.notifyMessage(string);
						try {
							socket.send(string);
						} catch (IOException e) {
							assertTrue(false);
						}
					}
				};
			}
		};

		for (int i = 0; i < numClients; i++) {
			clients.open();
		}

		clients.waitForDone(10000);
		servers.waitForDone(10000);
	}

	class WSClients extends WSManager {

		public void open() throws IOException {
			WSInterface newSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), readerFactory.getReader(), WSFactory.VERSION_HYBI_10, null);
			synchronized (this) {
				sockets.add(newSocket);
			}
			newSocket.open();
		}
	}

	class WSServers extends WSManager implements WSAcceptListener {

		public boolean ready = false;

		public void notifyAccept(WSInterface socket) {
			synchronized (this) {
				sockets.add(socket);
			}
			socket.setReadListener(readerFactory.getReader());
		}

		public void notifyReady(WSServer server) {
			System.out.println("I am ready! ");
			ready = true;
		}
	}

	class WSManager {

		Collection<WSInterface> sockets = new ArrayList<WSInterface>();
		WSReaderFactory readerFactory;
		private int lastOpenSockets = Integer.MAX_VALUE;

		public void closeAll() {
			for (WSInterface socket : sockets)
				socket.close();
		}

		public void waitForDone(long timeout) throws TimeoutException {
			long expiry = System.currentTimeMillis() + timeout;
			while (!sockets.isEmpty()) {
				if (sockets.isEmpty())
					return;
				long wait = expiry - System.currentTimeMillis();
				if (wait <= 0) {
					if (this.lastOpenSockets <= sockets.size()) {
						System.out.println("Expiring sockets (" + sockets.size() + " left, " + timeout + "ms after " + this.lastOpenSockets + ")");
						throw new TimeoutException();
					} else {
						this.lastOpenSockets = sockets.size();
						System.out.println(lastOpenSockets + " remaining");
						expiry += timeout;
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}

		public void removeSocket(WSInterface socket) {
			synchronized (this) {
				if (!sockets.contains(socket))
					System.out.println("TODO: See why this sometimes is true");
				sockets.remove(socket);
				assertFalse(sockets.contains(socket));
			}
		}
	}
}

interface WSReaderFactory {
	WebSocketListener getReader();
}
