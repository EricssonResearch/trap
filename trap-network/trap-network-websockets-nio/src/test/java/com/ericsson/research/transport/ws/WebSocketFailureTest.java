package com.ericsson.research.transport.ws;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */

import java.io.IOException;
import java.net.Socket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ericsson.research.transport.ws.spi.WSConstants;

public class WebSocketFailureTest extends TestCase
{
	private WSDataListener wsReader;
	private WSInterface	serverSocket;
	private WSServer ss;
	private boolean	ready;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.wsReader = new WSDataListener();

		this.ready = false;
		
		ss = WSFactory.createWebSocketServer("localhost", 0, new WSAcceptListener() {
			
			public void notifyAccept(WSInterface socket)
			{
				serverSocket = socket;
				socket.setReadListener(wsReader);
			}

			public void notifyReady(WSServer server)
			{
				ready = true;
			}
			
			public void notifyError(Throwable t) {
				t.printStackTrace();
			}
		}, null);
		
		while(!ready)
		{
			Thread.sleep(100);
		}
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
		while(serverSocket == null && System.currentTimeMillis() < time + 1000)
            Thread.sleep(10);
		socket.getOutputStream().write((WSConstants.GET+" / ").getBytes("UTF-8")); // Should succeed
		socket.getOutputStream().write('F'); // Should fail
		wsReader.waitForClose(1000);
	}
}

