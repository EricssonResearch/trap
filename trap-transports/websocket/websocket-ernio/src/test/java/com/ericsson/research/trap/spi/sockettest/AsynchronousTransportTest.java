package com.ericsson.research.trap.spi.sockettest;

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

import static com.ericsson.research.trap.auth.TrapContextKeys.Format;
import static com.ericsson.research.trap.auth.TrapContextKeys.LocalIP;
import static com.ericsson.research.trap.auth.TrapContextKeys.LocalPort;
import static com.ericsson.research.trap.auth.TrapContextKeys.Protocol;
import static com.ericsson.research.trap.auth.TrapContextKeys.RemoteIP;
import static com.ericsson.research.trap.auth.TrapContextKeys.RemotePort;
import static com.ericsson.research.trap.auth.TrapContextKeys.State;
import static com.ericsson.research.trap.auth.TrapContextKeys.Transport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.impl.queues.LinkedBlockingMessageQueue;
import com.ericsson.research.trap.impl.queues.LinkedByteBlockingMessageQueue;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.ThreadPool;

public class AsynchronousTransportTest implements OnAccept, OnData
{
    
    TrapEndpoint                  incomingEP;
    TrapListener                  listener;
    TrapClient                    c;
    TrapEndpoint                  s;
    
    ConcurrentLinkedQueue<byte[]> receipts       = new ConcurrentLinkedQueue<byte[]>();
    AtomicInteger                 receivingCount = new AtomicInteger(0);
    AtomicInteger                 processed      = new AtomicInteger(0);
    AtomicInteger                 receiving;
    int                           messages;
    
    @BeforeClass
    public static void setLoggerLevel()
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO);
    }
    
    @Before
    public void setUp() throws Throwable
    {

        JDKLoggerConfig.initForPrefixes(Level.INFO);
        this.listener = TrapFactory.createListener();
        
        this.listener.listen(this);
        
        String cfg = this.listener.getClientConfiguration();
        this.c = TrapFactory.createClient(cfg, true);
        this.c.setDelegate(this, true);
        this.c.open();
        
        // Accept
        
        this.s = this.accept();
        
        while (this.c.getState() != TrapState.OPEN)
            Thread.sleep(10);
    }

    @Test(timeout = 10000)
    public void testContext() throws Exception
    {
        
        this.testContext(this.c);
        this.testContext(this.s);
        
    }
    
    private void testContext(TrapEndpoint ep) throws TrapException
    {
        TrapTransport t = ep.getTransport("websocket");
        Collection<String> keys = t.getAuthenticationKeys();
        
        Assert.assertTrue(keys.containsAll(Arrays.asList(Transport, Protocol, LocalIP, LocalPort, RemoteIP, RemotePort, State, Format)));
        
        Map<String, Map<String, Object>> cx = ep.getTransportAuthenticationContexts();
        
        Assert.assertTrue(cx.containsKey("websocket"));
        
        Map<String, Object> props = cx.get("websocket");
        
        Set<String> returnedKeys = props.keySet();
        
        for (String key : keys)
            Assert.assertTrue("Key: " + key, returnedKeys.contains(key));        
    }

    @Test(timeout = 10000)
    public void testNormal() throws Exception
    {
        this.performMessageTests(1000);
    }
    
    @Test(timeout = 20000)
    public void testBlocking() throws Exception
    {
        LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
        q.setBlockingTimeout(1000);
        q.resize(10);
        this.s.setQueue(q);
        
        this.performMessageTests(1000);
    }
    
    @Test(timeout = 20000)
    public void testAlwaysBlocking() throws Exception
    {
        
        LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
        q.setBlockingTimeout(1000);
        q.resize(2);
        this.s.setQueue(q);
        
        this.performMessageTests(1000);
    }
    
    @Test(timeout = 10000)
    public void testByte() throws Exception
    {
        
        this.s.setQueueType(TrapEndpoint.REGULAR_BYTE_QUEUE);
        this.performMessageTests(1000);
    }
    
    @Test(timeout = 10000)
    public void testByteBlocking() throws Exception
    {
        LinkedByteBlockingMessageQueue q = new LinkedByteBlockingMessageQueue();
        q.setBlockingTimeout(1000);
        q.resize(128);
        this.s.setQueue(q);
        
        this.performMessageTests(1000);
    }
    
    @Test(timeout = 10000)
    public void testIndefiniteBlocking() throws Exception
    {
        
        LinkedBlockingMessageQueue q = new LinkedBlockingMessageQueue();
        q.setBlockingTimeout(Long.MAX_VALUE);
        q.resize(1);
        this.s.setQueue(q);
        
        this.performMessageTests(1000);
    }
    
    public void performMessageTests(final int messages) throws Exception
    {
        
        final byte[] bytes = "Helloes".getBytes();
        
        this.receiving = new AtomicInteger(4);
        this.receipts = new ConcurrentLinkedQueue<byte[]>();
        this.receivingCount = new AtomicInteger(0);
        this.processed = new AtomicInteger(0);
        this.messages = messages;
        
        for (int k = 0; k < this.receiving.get(); k++)
        {
            ThreadPool.executeFixed(new Runnable() {
                
                public void run()
                {
                    for (int i = 0; i < (messages / AsynchronousTransportTest.this.receiving.get()); i++)
                        try
                        {
                            byte[] b = AsynchronousTransportTest.this.receive();
                            if (b == null)
                                continue;
                            Assert.assertArrayEquals(b, bytes);
                            AsynchronousTransportTest.this.processed.incrementAndGet();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    AsynchronousTransportTest.this.receiving.decrementAndGet();
                }
            });
        }
        
        ThreadPool.executeCached(new Runnable() {
            
            public void run()
            {
                
                try
                {
                    AsynchronousTransportTest.this.s.send(bytes);
                    Thread.sleep(200);
                }
                catch (TrapException e1)
                {
                    e1.printStackTrace();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                for (int i = 1; i < messages; i++)
                {
                    try
                    {
                        AsynchronousTransportTest.this.s.send(bytes);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
                
            }
            
        });
        
        while (this.receiving.get() != 0)
            Thread.sleep(100);
        
    }
    
    @Test
    public void testLiveness() throws Exception
    {
        
        this.s.send("Hello".getBytes());
        
        // Now check liveness
        Assert.assertTrue(this.s.isAlive(100, true, false, 0).get());
        Assert.assertTrue(this.s.isAlive(1000, false, false, 0).get());
        
        Thread.sleep(10);
        // This should return false; we're basically asking if there was a message in the last 0 milliseconds, after explicitly sleeping
        Assert.assertFalse(this.s.isAlive(0, false, false, 0).get());
        
        Thread.sleep(10);
        this.c.send("Hello".getBytes());
        Thread.sleep(25);
        
        // This should succeed; the server has received a message within the last 25 ms.
        Assert.assertTrue(this.s.isAlive(45, false, false, 0).get());
        
        // This should fail; the client has not.
        Assert.assertFalse(this.c.isAlive(5, false, false, 0).get());
    }
    
    protected synchronized TrapEndpoint accept() throws InterruptedException
    {
        try
        {
            while (this.incomingEP == null)
                this.wait();
            
            return this.incomingEP;
        }
        finally
        {
            this.incomingEP = null;
        }
    }
    
    private byte[] receive() throws Exception
    {
        byte[] b = null;
        while (((b = this.receipts.poll()) == null) && (this.processed.get() != this.messages))
        {
            Thread.sleep(1);
        }
        return b;
    }
    
    public synchronized void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        //System.out.println("Incoming Connection");
        this.incomingEP = endpoint;
        endpoint.setDelegate(this, true);
        this.notify();
    }
    
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        //System.out.println(new String(data));
        this.receivingCount.incrementAndGet();
        this.receipts.add(data);
        
        if (this.receipts.size() > 10000)
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
    }
}
