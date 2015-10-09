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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.transport.ws.spi.WSAbstractProtocolWrapper;

public class WebSocketStressTest {
	
	private WSClients clients;
	private WSServers servers;
	private WSServer ss;
	
	@Before
	public void setUp() throws Exception {
		servers = new WSServers();
		clients = new WSClients();
		ss = WSFactory.createWebSocketServer("localhost", 0, servers, null);
		while(!servers.ready)
			Thread.sleep(100);
	}
	
	@After
	public void tearDown() {
		servers.closeAll();
		clients.closeAll();
		ss.close();
		System.gc();
	}

	@Test
	public void test1Clients() throws Exception {
		doNumClientTest(1);
	}
	
	@Test
	public void test10Clients() throws Exception {
		doNumClientTest(10);
	}
	
	@Test
	public void test100Clients() throws Exception {
		doNumClientTest(100);
	}
	
	@Test
	@Ignore
	public void test1000Clients() throws Exception {
		doNumClientTest(1000);
	}
	
	@Test
	@Ignore
	public void test10000Clients() throws Exception {
		doNumClientTest(10000);
	}
	
	private void doNumClientTest(int numClients) throws Exception {
		for (int i = 0; i < numClients; i++)
			clients.open();
		servers.waitForDone(1000);
		clients.waitForDone(1000);
	}
	
	class WSClients extends WSManager {
		
		public WSClients() {
			readerFactory = new WSReaderFactory() {
				public WSDataListener getReader() {
					return new WSDataListener() {
						
						private String message;

						public void notifyClose() {
							synchronized(this) {
								ncloses++;
							}
							clients.removeSocket(socket);
							super.notifyClose();
						}
						
						public void notifyError(Throwable t) {
							synchronized(this) {
								nerrors++;
							}
							clients.removeSocket(socket);
							super.notifyError(t);
						}
						
						public void notifyOpen(WSInterface socket) {
							synchronized(this) {
								nopens++;
							}
							super.notifyOpen(socket);
							message = ""+socket.hashCode();
							try {
								socket.send(message);
							} catch (IOException e) {
								Assert.fail(e.getMessage());
							}
						}
						
						public void notifyMessage(byte[] data) {
							Assert.fail();
						}
						
						public void notifyMessage(String string) {
							synchronized(this) {
								nmsg++;
							}
							super.notifyMessage(string);
							Assert.assertEquals(message, string);
							socket.close();
							synchronized(this) {
								closes++;
							}
						}
					};
				}
			};
		}
		
		public void open() throws Exception {
			WSInterface connectWebSocketClient = WSFactory.createWebSocketClient(ss.getURI(), readerFactory.getReader(), WSFactory.VERSION_HIXIE_75, null);
			sockets.add(connectWebSocketClient);
			connectWebSocketClient.open();
		}
		
	}
	
	class WSServers extends WSManager implements WSAcceptListener {
		
		boolean ready = false;
		
		public WSServers() {
			readerFactory = new WSReaderFactory() {
				public WSDataListener getReader() {
					return new WSDataListener() {
						
						public void notifyClose() {
							synchronized(this) {
								ncloses++;
							}
							servers.removeSocket(socket);
							super.notifyClose();
						}
						
						public void notifyError(Throwable t) {
							synchronized(this) {
								nerrors++;
							}
							servers.removeSocket(socket);
							super.notifyError(t);
						}
						
						public void notifyMessage(byte[] data) {
							Assert.fail();
						}
						
						public void notifyMessage(String string) {
							synchronized(this) {
								nmsg++;
							}
							super.notifyMessage(string);
							try {
								socket.send(string);
							} catch (IOException e) {
								Assert.fail(e.getMessage());
							}
						}
					};
				}
			};
		}
		
		public void notifyAccept(WSInterface socket) {
			sockets.add(socket);
			socket.setReadListener(readerFactory.getReader());
		}

		public void notifyReady(WSServer server) {
			ready = true;
		}
		
		public void notifyError(Throwable t) {
			t.printStackTrace();
		}
		
	}
	
	class WSManager {
		
		int closes = 0;
		int nopens = 0;
		int ncloses = 0;
		int nerrors = 0;
		int nmsg = 0;
		
		final Collection<WSInterface> sockets = Collections.synchronizedCollection(new ArrayList<WSInterface>());
		WSReaderFactory readerFactory;
		private int	lastOpenSockets = Integer.MAX_VALUE;
		
		public void waitForDone(long timeout) {
			long expiry = System.currentTimeMillis() + timeout;
			while (!sockets.isEmpty()) {
				if (sockets.isEmpty())
					return;
				long wait = expiry - System.currentTimeMillis();
				if (wait <= 0) {
					if (lastOpenSockets  <= sockets.size()) {
						try {
							Thread.sleep(timeout);
						} catch (InterruptedException e) {}
						if(!sockets.isEmpty()) {
							StringBuffer out = new StringBuffer("ERROR: "+sockets.size() + " sockets left, " + timeout + "ms after " + lastOpenSockets + " in "+this.getClass().getSimpleName());
							for(WSInterface e : sockets) {
								out.append("\n state = ");
								out.append(((WSAbstractProtocolWrapper)e).getProtocol().getState());
							}
							System.out.println(out);
							Assert.fail(out.toString());
						}
					} else {
						lastOpenSockets = sockets.size();
						System.out.println(lastOpenSockets + " remaining");
						expiry += timeout;
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}
		
		public void removeSocket(WSInterface socket) {
			sockets.remove(socket);
			Assert.assertFalse(sockets.contains(socket));
		}
		
		public void closeAll() {
			String n = getClass().getSimpleName();
			System.out.println(n+" nopens = "+nopens);
			System.out.println(n+" ncloses = "+ncloses);
			System.out.println(n+" nerrors = "+nerrors);
			System.out.println(n+" closes = "+closes);
			System.out.println(n+" nmsg = "+nmsg);
			WSInterface[] remain = sockets.toArray(new WSInterface[sockets.size()]);
			for(int i=0;i<remain.length;i++)
				remain[i].close();
		}
		
	}
}

interface WSReaderFactory {
	WSDataListener getReader();
}
