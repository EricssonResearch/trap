package com.ericsson.research.transport.ws.hybi10;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSServerImpl;

/**
 * Tried to write a simpler test to provoke the errors we see in the WebSocketStressTest for all implementations (HIXIE-75, HIXIE-76 and HyBi-10).
 * Hmm. Typically works repeatedly for up to 50 clients.
 * However, it seems to sometimes work fine for up to 300 clients ONCE. But subsequent runs seem to fail unless one waits(!) about a minute until rerunning the test.
 * My guess is that something on OS level is not cleaned up until some timeout (such as underlying nio channels mapped to TCP sockets etc).
 * Could be the OS-settings for number of allowed TCP connections etc.
 * 
 * The tests sets up a server socket listener for each server socket that gets instantiated through the notifyAccept() that gets called on the client's open().
 * The listeners collects the received messages in a list 'receivedMessages' and the clients collect the sent messages as they are sent.
 * The final test assertion is then to compare all the sent messages with the received ones.
 * 
 * @author emicmra
 *
 */
public class WebSocketHybi10StressTest extends TestCase {
	
	private static final int MAX_TOTAL_TEST_TIME = 20000;
	private static final int WAIT = 5000;
	private static final int NUM_CLIENTS = 10;

	WSServerImpl ss;

	boolean serverWebSocketReady = false;

	List<String> sentMessages = Collections.synchronizedList(new ArrayList<String>());
	List<String> receivedMessages = Collections.synchronizedList(new ArrayList<String>());

	boolean abortTest = false;

	@Override
	protected void setUp() throws Exception {

		this.ss = new WSServerImpl("localhost", 0, new WSAcceptListener() {

			public void notifyAccept(WSInterface socket) {
				socket.setReadListener(new WebSocketListener() {
					@Override
					public void notifyMessage(String incomingMessage) {
						super.notifyMessage(incomingMessage);
						receivedMessages.add(incomingMessage);
					}
				});
			}

			public void notifyReady(WSServer server) {
				serverWebSocketReady = true;
			}
		}, null);

		while (!serverWebSocketReady) {
			Thread.sleep(100);
		}

	}

	@Override
	protected void tearDown() {
		ss.close();
	}

	
	public void testHyBi10() throws Exception {
		runWithManyClients(WSFactory.VERSION_HYBI_10);
	}

	public void testHixie76() throws Exception {
		runWithManyClients(WSFactory.VERSION_HIXIE_76);
	}
	
	public void testHixie75() throws Exception {
		runWithManyClients(WSFactory.VERSION_HIXIE_75);
	}




	private void runWithManyClients(int webSocketVersion) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS);
		for (int i = 0; i < NUM_CLIENTS; i++) {
			executor.execute(new MyRunnable(i, webSocketVersion));
		}
		// This will make the executor accept no new threads and finish all existing threads in the queue
		executor.shutdown();
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				abortTest = true;
				fail("Test aborted. Only allowed " + MAX_TOTAL_TEST_TIME + "ms to run test.");
			}
		}, MAX_TOTAL_TEST_TIME);

		// Wait until all threads are finished sending
		while (!abortTest && !executor.isTerminated()) {
			Thread.sleep(100);			
		}

		// Wait until all messages are received
		while (!abortTest && receivedMessages.size() < NUM_CLIENTS) {
			Thread.sleep(100);
		}

		assertTrue("All sent messages were not received.", receivedMessages.containsAll(sentMessages));

		if (abortTest) {
			fail("Test aborted, check console for errors.");
		} else {
			System.out.println("DONE! Num clients: " + NUM_CLIENTS);
		}
	}

	class MyRunnable implements Runnable {

		private final int i;
		private final int webSocketVersion;

		public MyRunnable(int i, int webSocketVersion) {
			this.i = i;
			this.webSocketVersion = webSocketVersion;
		}

		public void run() {
			WebSocketListener clientListener = new WebSocketListener();
			try {
				WSInterface clientSocket = WSFactory.getInstance().createWebSocketClient(ss.getURI(), clientListener, webSocketVersion, null);
				clientSocket.open();
				try {
					clientListener.waitForOpen(WAIT);					
				} catch (TimeoutException te) {
					abortTest = true;
					fail("Client timed out when trying to open connection. " + (te.getMessage() != null ? te.getMessage() : ""));
				}
				String message = "Message#" + i;
				clientSocket.send(message);
				sentMessages.add(message);
				
				clientSocket.close();
				try {
					clientListener.waitForClose(WAIT);
					System.out.println("Got close, version: " + webSocketVersion);
				} catch (TimeoutException te) {
					abortTest = true;
					fail("Client timed out when trying to close connection. " + (te.getMessage() != null ? te.getMessage() : ""));
				}
			} catch (Exception e) {
				abortTest = true;
				fail("Client#" + i + " failed. " + (e.getMessage() != null ? e.getMessage() : ""));
			}
		}
	}

}
