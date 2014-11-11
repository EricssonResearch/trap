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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WebSocketMultiplexTest {
	
	private WSInterface client;
	private WSInterface server;
	private WSServer ss;
	private boolean ready;
    private WSDataListener serverListener;
	private final ExecutorService executor = Executors.newFixedThreadPool(100);
	private static final Set<String> sent = Collections.synchronizedSet(new HashSet<String>());
	private AtomicLong read;
    private AtomicLong run;
    private AtomicLong received;
    private AtomicLong sentt;
	
	@Before
	public void setUp() throws Exception {
		read = new AtomicLong(0);
		run = new AtomicLong(0);
		received = new AtomicLong(0);
		sentt = new AtomicLong(0);
		
		serverListener = new WSDataListener() {
			public synchronized void notifyMessage(String string) {
				try {
					received.incrementAndGet();
					socket.send(string);
				} catch (IOException e) {
					notifyError(e);
				}
			}
			//public void notifyError(Throwable t) {}
		};
		
		ss = WSFactory.createWebSocketServer("localhost", 0, new WSAcceptListener() {

			public void notifyAccept(WSInterface socket) {
				server = socket;
				socket.setReadListener(serverListener);
			}

			public void notifyReady(WSServer server) {
				ready = true;
			}
			
			public void notifyError(Throwable t) {
				t.printStackTrace();
			}
		}, null);

		while(!ready)
			Thread.sleep(100);


        WSDataListener clientListener = new WSDataListener() {
            public synchronized void notifyMessage(String string) {
                read.incrementAndGet();
                sent.remove(string);
            }
            //public void notifyError(Throwable t) {}
        };
		client = WSFactory.createWebSocketClient(ss.getURI(), clientListener, WSFactory.VERSION_HIXIE_75, null);
		client.open();
		serverListener.waitForOpen(1000);
		clientListener.waitForOpen(1000);
	}
	
	@After
	public void tearDown() throws Exception {
		client.close();
		server.close();
		ss.close();
	}
	
	class SendTask implements Runnable {
		
		final String s;
		
		public SendTask(int i) {
			/*
			if(i%125==0)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				*/
			s = "msg#"+i;
			sent.add(s);
		}
		
		public void run() {
			try {
				run.incrementAndGet();
				client.send(s);
				sentt.incrementAndGet();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testSend() throws InterruptedException {
		long start = System.currentTimeMillis();
		for(int i=1;i<=200000;i++) {
			try {
				executor.execute(new SendTask(i));
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
		long expiry = System.currentTimeMillis() + 100000;
		while(!sent.isEmpty() && expiry > System.currentTimeMillis()) {
			Thread.sleep(1000);
			System.err.println("started="+run+", sent="+sentt+", remaining="+sent.size()+", roundtrips="+read.longValue()+", received="+received+", speed="+((int)(((double)read.longValue()*2/(double)(System.currentTimeMillis()-start))*1000))+" msg/sec");
		}
		System.err.println("-----");
		if(!sent.isEmpty())
			Assert.fail(sent.size()+" messages failed to send: "+sent);
		if(System.currentTimeMillis()>start)
			System.out.println("Speed: "+((int)(((double)read.longValue()*2/(double)(System.currentTimeMillis()-start))*1000))+" msg/sec, time = "+(System.currentTimeMillis()-start)/(double)1000+" sec");
	}

}