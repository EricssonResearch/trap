package com.ericsson.research.transport.ws2;

import java.io.IOException;
import java.net.Socket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.transport.ws.WebSocketListener;
import com.ericsson.research.transport.ws.spi.WSConstants;
import com.ericsson.research.transport.ws.spi.WSServerImpl;


public class WebSocketFailureTest extends TestCase
{
	private WebSocketListener wsReader;
	private WSInterface	serverSocket;
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
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		if (serverSocket != null) {
			serverSocket.close();
		}
		
		if (ss != null) {
			ss.close();
		}

		super.tearDown();
	}
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public WebSocketFailureTest( String testName ) throws IOException
	{
		super( testName );
	}
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( WebSocketFailureTest.class );
	}
	
	public void testErroneousSocket() throws Exception
	{
		Socket socket = new Socket(ss.getAddress().getHostName(), ss.getAddress().getPort());
		long time = System.currentTimeMillis();
		while(serverSocket == null && System.currentTimeMillis() < time + 1000);
		socket.getOutputStream().write((WSConstants.GET+" / ").getBytes("UTF-8")); // Should succeed
		socket.getOutputStream().write('F'); // Should fail
		wsReader.waitForClose(1000);
	}
}

