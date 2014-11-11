package com.ericsson.research.transport.ws.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSURI;
import com.ericsson.research.transport.ws.WebSocketListener;

public class WSImplementationHybi10Test extends TestCase {

	public void testConsumeCompleteFrames_Client_SingleCompleteFrame() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new WSURI("ws://banana"), new WebSocketListener());

		byte[] remainder = hybi10WS.consumeCompleteFrames(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(true));
		assertTrue(remainder.length == 0);

		assertEquals(1, hybi10WS.numCalls);
	}

	public void testConsumeCompleteFrames_Server_SingleCompleteFrame() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new ManagedSocket(), false);
		hybi10WS.setReadListener(new WebSocketListener()); // Not used in test, but needs to be non-null

		byte[] remainder = hybi10WS.consumeCompleteFrames(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(false));
		assertTrue(remainder.length == 0);

		assertEquals(1, hybi10WS.numCalls);
	}

	public void testConsumeCompleteFrames_Client_TwoCompleteFrames() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new WSURI("ws://banana"), new WebSocketListener());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(true));
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello2".getBytes("UTF-8")).toByteArray(true));

		byte[] remainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(remainder.length == 0);

		assertEquals(2, hybi10WS.numCalls);
	}

	public void testConsumeCompleteFrames_Server_TwoCompleteFrames() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new ManagedSocket(), false);
		hybi10WS.setReadListener(new WebSocketListener()); // Not used in test, but needs to be non-null

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(false));
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello2".getBytes("UTF-8")).toByteArray(false));

		byte[] remainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(remainder.length == 0);

		assertEquals(2, hybi10WS.numCalls);

	}

	public void testSingleCompleteFrame_Client_WithIncompleteRemainder() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new WSURI("ws://banana"), new WebSocketListener());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(true));
		byte[] expectedRemainder = new byte[] { WSAbstractFrame.SINGLE_FRAME_BINARY_MESSAGE };
		baos.write(expectedRemainder);

		byte[] actualRemainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(Arrays.equals(expectedRemainder, actualRemainder));

		assertEquals(1, hybi10WS.numCalls);
	}

	public void testSingleCompleteFrame_Server_WithIncompleteRemainder() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new ManagedSocket(), false);
		hybi10WS.setReadListener(new WebSocketListener()); // Not used in test, but needs to be non-null

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(false));
		byte[] expectedRemainder = new byte[] { WSAbstractFrame.SINGLE_FRAME_BINARY_MESSAGE };
		baos.write(expectedRemainder);

		byte[] actualRemainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(Arrays.equals(expectedRemainder, actualRemainder));

		assertEquals(1, hybi10WS.numCalls);
	}

	public void testTwoCompleteFrames_Client_WithIncompleteRemainder() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new WSURI("ws://banana"), new WebSocketListener());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(true));
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello2".getBytes("UTF-8")).toByteArray(true));
		byte[] expectedRemainder = new byte[] { WSAbstractFrame.SINGLE_FRAME_BINARY_MESSAGE };
		baos.write(expectedRemainder);

		byte[] actualRemainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(Arrays.equals(expectedRemainder, actualRemainder));

		assertEquals(2, hybi10WS.numCalls);
	}

	public void testTwoCompleteFrames_Server_WithIncompleteRemainder() throws Exception {
		// Will construct a client
		TestTarget hybi10WS = new TestTarget(new ManagedSocket(), false);
		hybi10WS.setReadListener(new WebSocketListener()); // Not used in test, but needs to be non-null

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello".getBytes("UTF-8")).toByteArray(false));
		baos.write(new WSAbstractFrame(WSAbstractFrame.SINGLE_FRAME_TEXT_MESSAGE, "Hello2".getBytes("UTF-8")).toByteArray(true));
		byte[] expectedRemainder = new byte[] { WSAbstractFrame.SINGLE_FRAME_BINARY_MESSAGE };
		baos.write(expectedRemainder);

		byte[] actualRemainder = hybi10WS.consumeCompleteFrames(baos.toByteArray());
		assertTrue(Arrays.equals(expectedRemainder, actualRemainder));

		assertEquals(2, hybi10WS.numCalls);
	}
	
	class TestTarget extends WSImplementationHybi10 {
		// Note, this would be cleaner with a mock object, but since the WS implementation class handles an internal state, no helper class for frame
		// handling has been introduced (yet).
		// If the implementation class get too messy, we should break that up into smaller and more easily testable pieces. A FrameHandler per message
		// type for example.
		// Basically, we just track the number of complete frames parsed to make sure the tail recursion loop in consumeCompleteFrames() works.
		public int numCalls = 0;

		public TestTarget(ManagedSocket socket, boolean secure) {
			super(socket, secure);
		}

		public TestTarget(WSURI uri, WSListener listener) throws IOException {
			super(uri, listener, null);
		}

		@Override
		protected void handleCompleteFrame(ByteArrayInputStream bais) throws WSException, UnsupportedEncodingException {
			super.handleCompleteFrame(bais);
			numCalls++;
		}
	}

}
