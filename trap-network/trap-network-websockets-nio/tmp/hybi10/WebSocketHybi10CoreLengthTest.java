package com.ericsson.research.transport.ws.hybi10;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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
public class WebSocketHybi10CoreLengthTest extends TestCase {

	private static final int WAIT = 10000;

	// Short length test (125 characters which is the upper limit) string for
	// testing send with alternative one for specifying length (7 bits length field in frame)
	public static final String SHORT_TEXT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua..";
	
	// Medium length test (446 characters) string for testing send with alternative two for specifying length (2 bytes length field in frame)
	public static final String MEDIUM_TEXT_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

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
	
	//////////////////////// Base tests
	
	public void testSendSmallTextClientMessage() throws Exception {
		clientSocket.send(SHORT_TEXT_MESSAGE);
		assertEquals(SHORT_TEXT_MESSAGE, serverListener.waitForString(WAIT));
	}

	public void testSendSmallTextServerMessage() throws Exception {
		serverSocket.send(SHORT_TEXT_MESSAGE);
		assertEquals(SHORT_TEXT_MESSAGE, clientListener.waitForString(WAIT));
	}

	public void testSendMediumTextClientMessage() throws Exception {
		clientSocket.send(MEDIUM_TEXT_MESSAGE);
		assertEquals(MEDIUM_TEXT_MESSAGE, serverListener.waitForString(WAIT));
	}

	public void testSendMediumTextServerMessage() throws Exception {
		serverSocket.send(MEDIUM_TEXT_MESSAGE);
		assertEquals(MEDIUM_TEXT_MESSAGE, clientListener.waitForString(WAIT));
	}
	
	public void testSendLargeTextClientMessage() throws Exception {
		String longTextMessage = createLongTextMessage(120);
		
		clientSocket.send(longTextMessage);
		assertEquals(longTextMessage, serverListener.waitForString(WAIT));
	}

	public void testSendLargeTextServerMessage() throws Exception {
		String longTextMessage = createLongTextMessage(120);

		serverSocket.send(longTextMessage);
		assertEquals(longTextMessage, clientListener.waitForString(WAIT));
	}
	
	public void testSendSmallBinaryClientMessage() throws Exception {
		byte[] crap = new byte[] { 'H', 'e', 'l', 'l', 'o' };
		clientSocket.send(crap);
		assertTrue(Arrays.equals(crap, serverListener.waitForBytes(1000)));
	}

	public void testSendSmallBinaryServerMessage() throws Exception {
		byte[] crap = new byte[] { 'H', 'e', 'l', 'l', 'o' };
		serverSocket.send(crap);
		assertTrue(Arrays.equals(crap, clientListener.waitForBytes(1000)));
	}

	public void testSendMediumBinaryClientMessage() throws Exception {
		byte[] logo = readFile("src/test/resources/1000px-Ericsson_logo.svg.png");
		clientSocket.send(logo);
		byte[] logo2 = serverListener.waitForBytes(1000);
		assertTrue(Arrays.equals(logo, logo2));
//		writeToFile("src/test/resources/1000px-Ericsson_logo2.svg.png", logo2);
	}

	public void testSendMediumBinaryServerMessage() throws Exception {
		byte[] logo = readFile("src/test/resources/1000px-Ericsson_logo.svg.png");
		serverSocket.send(logo);
		byte[] logo2 = clientListener.waitForBytes(1000);
		assertTrue(Arrays.equals(logo, logo2));
//		writeToFile("src/test/resources/1000px-Ericsson_logo2.svg.png", logo2);
	}

	public void testSendLargeBinaryClientMessage() throws Exception {
		byte[] logo = readFile("src/test/resources/TestMovie.avi");
		clientSocket.send(logo);
		byte[] logo2 = serverListener.waitForBytes(2000);
		assertTrue(Arrays.equals(logo, logo2));
//		writeToFile("src/test/resources/TestMovie2.avi", logo2);
	}

	public void testSendLargeBinaryServerMessage() throws Exception {
		byte[] logo = readFile("src/test/resources/TestMovie.avi");
		serverSocket.send(logo);
		byte[] logo2 = clientListener.waitForBytes(2000);
		assertTrue(Arrays.equals(logo, logo2));
//		writeToFile("src/test/resources/TestMovie2.avi", logo2);
	}


	////////////////////////// End base tests
	
	public void testRequestResponse() throws Exception {
		testSendSmallTextClientMessage();
		testSendSmallTextServerMessage();
	}

	public void testSendSmallTextMessageFollowedByBinaryMessage() throws Exception {
		testSendSmallTextClientMessage();
		testSendSmallBinaryClientMessage();
	}

	public void testSendManySmallTextClientMessages() throws Exception {
		for (int i = 0; i < 1000; i++) {
			testSendSmallTextClientMessage();
		}
	}

	public void testSendManySmallBinaryClientMessages() throws Exception {
		for (int i = 0; i < 1000; i++) {
			testSendSmallBinaryClientMessage();
		}
	}

	public void testSendManyMediumTextClientMessages() throws Exception {
		for (int i = 0; i < 1000; i++) {
			testSendMediumTextClientMessage();
		}
	}

	public void testSendManyMediumBinaryClientMessages() throws Exception {
		byte[] logo = readFile("src/test/resources/1000px-Ericsson_logo.svg.png");

		for (int i = 0; i < 1000; i++) {
			clientSocket.send(logo);
			byte[] logo2 = serverListener.waitForBytes(1000);
			assertTrue(Arrays.equals(logo, logo2));
		}
	}

	public void _testSendManyLargeTextClientMessages() throws Exception {
		String longTextMessage = createLongTextMessage(120);

		for (int i = 0; i < 1000; i++) {
			clientSocket.send(longTextMessage);
			assertEquals(longTextMessage, serverListener.waitForString(WAIT));
		}
	}

	public void _testSendManyLargeBinaryClientMessages() throws Exception {
		byte[] logo = readFile("src/test/resources/TestMovie.avi");

		for (int i = 0; i < 10; i++) {
			clientSocket.send(logo);
			byte[] logo2 = serverListener.waitForBytes(1000);
			assertTrue(Arrays.equals(logo, logo2));
		}
	}

	private String createLongTextMessage(int num) {
		String s = MEDIUM_TEXT_MESSAGE;
		
		for (int i = 0; i < num; i++) {
			s += MEDIUM_TEXT_MESSAGE;
		}
		
		return s;
	}
	
	private byte[] readFile(String fileName) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		InputStream is = null;
		try {
			is = new FileInputStream(new File(fileName));
			
			int read;
			while((read = is.read()) != -1){
			   buffer.write(read);
			}
			           
		} finally {
			if (is != null) {
				is.close();
			}
		}
		
		return buffer.toByteArray();
	}
	
	@SuppressWarnings("unused")
	private void writeToFile(String fileName, byte[] data) throws IOException {
		FileOutputStream fileoutputstream = new FileOutputStream(fileName);

        for (int i = 0; i < data.length; i++) {
            fileoutputstream.write(data[i]);
        }

        fileoutputstream.close();
	}

}
