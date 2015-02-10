package com.ericsson.research.trap.spi.queues;

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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.TrapMessageImpl;
import com.ericsson.research.trap.impl.queues.LinkedMessageQueue;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.utils.PackageScanner;
import com.ericsson.research.trap.utils.ThreadPool;

@RunWith(Parameterized.class)
public class MessageQueueTest
{
    private Class<?>                  testClass;
    private MessageQueue              mq   = null;
    private boolean                   isByteQueue;
    private long                      msgLength;
    private static ThreadPoolExecutor exec = null;
    
    class TerminationFlag
    {
        boolean terminate = false;
    }
    
    @BeforeClass
    public static void beforeClass()
    {
        exec = (ThreadPoolExecutor) Executors.newCachedThreadPool(); //new ThreadPoolExecutor(100, 1000, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(5000));
        exec.setMaximumPoolSize(2000);
    }
    
    @AfterClass
    public static void afterClass()
    {
        exec.shutdownNow();
        exec = null;
    }
    
    private TerminationFlag tf;
    
    @Parameters
    public static List<Object[]> data() throws IOException
    {
        LinkedList<Object[]> rv = new LinkedList<Object[]>();
        for (Class<?> c : PackageScanner.scan(LinkedMessageQueue.class.getPackage().getName()))
            if (MessageQueue.class.isAssignableFrom(c) && !c.isInterface())
                rv.add(new Object[] { c });
        return rv;
    }
    
    public MessageQueueTest(Object testClass) throws Exception
    {
        System.out.println("Now testing " + testClass);
        this.testClass = (Class<?>) testClass;
        
        this.mq = (MessageQueue) this.testClass.getConstructor(Integer.TYPE).newInstance(10);
        this.mq.put(new TrapMessageImpl().setMessageId(1));
        this.msgLength = new TrapMessageImpl().length();
        this.isByteQueue = this.mq.length() == this.msgLength;
        
    }
    
    private void assertMessagesInQueue(int msgs)
    {
        if (this.isByteQueue)
            msgs *= this.msgLength;
        Assert.assertEquals(msgs, this.mq.length());
    }
    
    @Before
    public void setUp() throws Exception
    {
        this.mq = this.newMQ(10);
        this.tf = new TerminationFlag();
    }
    
    private MessageQueue newMQ(int i) throws Exception
    {
        return (MessageQueue) this.testClass.getConstructor(Integer.TYPE).newInstance(i * (this.isByteQueue ? 16 : 1));
    }
    
    @After
    public void cleanUp() throws Exception
    {
        this.mq = null;
        this.tf.terminate = true;
        this.tf = null;
    }
    
    @Test
    public void basicPutGet() throws Exception
    {
        this.assertMessagesInQueue(0);
        this.mq.put(new TrapMessageImpl().setMessageId(1));
        this.assertMessagesInQueue(1);
        
        TrapMessage tm = this.mq.peek();
        Assert.assertNotNull(tm);
        this.assertMessagesInQueue(1);
        
        tm = this.mq.pop();
        Assert.assertNotNull(tm);
        this.assertMessagesInQueue(0);
        
    }
    
    @Test
    public void testToString() throws Exception
    {
        // Just ensure toString has an output
        String str = this.mq.toString();
        Assert.assertFalse(str.length() == 0);
    }
    
    @Test
    public void testEmpty() throws Exception
    {
        // Ensure an empty message queue cannot throw stuff at us.
        Assert.assertNull(this.mq.peek());
        Assert.assertNull(this.mq.pop());
    }
    
    @Test
    public void testClone() throws Exception
    {
        MessageQueue newQueue = this.mq.createNewQueue();
        Assert.assertEquals(this.mq.toString(), newQueue.toString());
    }
    
    @Test
    public void testEmptyInit() throws Exception
    {
        // Requirement: Every MQ must have a no-arg constructor too.
        this.testClass.newInstance();
    }
    
    @Test(timeout = 10000)
    public void testBlocking() throws Exception
    {
        final int numMsgs = 20;
        if (!(this.mq instanceof BlockingMessageQueue))
            return;
        
        MessageQueueTest.exec.submit(this.createWriter(0, numMsgs));
        
        while (this.mq.size() != this.mq.length())
        {
            Thread.sleep(10);
        }
        
        for (int i = 0; i < numMsgs; i++)
        {
            Assert.assertNotNull("Failed popping " + i + " with queue " + this.mq, this.mq.pop());
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testBlockingShortRecovery() throws Exception
    {
        final int numMsgs = 20;
        if (!(this.mq instanceof BlockingMessageQueue))
            return;
        ((BlockingMessageQueue) this.mq).setBlockingTimeout(1000);
        
        MessageQueueTest.exec.submit(this.createWriter(0, numMsgs));
        
        while (this.mq.size() != this.mq.length())
        {
            Thread.sleep(10);
        }
        
        for (int i = 0; i < numMsgs; i++)
        {
            Assert.assertNotNull("Failed popping " + i + " with queue " + this.mq, this.mq.pop());
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000, expected = TrapException.class)
    public void testOverflow() throws Exception
    {
        final int numMsgs = 20;
        if ((this.mq instanceof BlockingMessageQueue))
            ((BlockingMessageQueue) this.mq).setBlockingTimeout(100);
        
        for (int i = 0; i < numMsgs; i++)
            this.mq.put(new TrapMessageImpl().setMessageId(i).setOp(Operation.MESSAGE));
    }
    
    @Test(timeout = 10000)
    public void testMIMO() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        this.mq = this.newMQ(10000);
        
        for (int i = 0; i < 50; i++)
        {
            MessageQueueTest.exec.submit(this.createWriter(0, 50));
            MessageQueueTest.exec.submit(this.createPopper(50));
            MessageQueueTest.exec.submit(this.createPeeker());
        }
        
        while (this.mq.peek() != null)
        {
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testSIMO() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        this.mq = this.newMQ(10000);
        
        MessageQueueTest.exec.submit(this.createWriter(0, 2000));
        for (int i = 0; i < 100; i++)
        {
            MessageQueueTest.exec.submit(this.createPopper(20));
        }
        
        while (this.mq.peek() != null)
        {
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testMISO() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        this.mq = this.newMQ(10000);
        
        for (int i = 0; i < 100; i++)
            MessageQueueTest.exec.submit(this.createWriter(0, 20));
        MessageQueueTest.exec.submit(this.createPopper(2000));
        
        while (this.mq.peek() != null)
        {
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testBlockingMISO() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        if (!(this.mq instanceof BlockingMessageQueue))
            return;
        
        for (int i = 0; i < 100; i++)
            MessageQueueTest.exec.submit(this.createWriter(0, 20));
        MessageQueueTest.exec.submit(this.createPopper(2000));
        
        while (this.mq.peek() != null)
        {
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testBlockingMIMO() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        if (!(this.mq instanceof BlockingMessageQueue))
            return;
        
        for (int i = 0; i < 100; i++)
            MessageQueueTest.exec.submit(this.createWriter(0, 20));
        for (int i = 0; i < 100; i++)
            MessageQueueTest.exec.submit(this.createPopper(20));
        for (int i = 0; i < 100; i++)
            MessageQueueTest.exec.submit(this.createPeeker());
        
        while (this.mq.peek() != null)
        {
            Thread.sleep(10);
        }
    }
    
    @Test(timeout = 10000)
    public void testBasicAPI() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        this.mq = this.newMQ(10000);
        
        Assert.assertEquals(0, this.mq.length());
        Assert.assertEquals(10000 * (this.isByteQueue ? 16 : 1), this.mq.size());
        Assert.assertNotNull(this.mq.toString());
        Assert.assertNotNull(this.mq.getQueueType());
        Assert.assertEquals(false, this.mq.hasMoreThanOne());
        this.mq.put(new TrapMessageImpl());
        Assert.assertEquals(false, this.mq.hasMoreThanOne());
        this.mq.put(new TrapMessageImpl());
        Assert.assertEquals(true, this.mq.hasMoreThanOne());
        
    }
    
    @Test(timeout = 10000)
    public void testResize() throws Exception
    {
        if (!(this.mq instanceof ResizableMessageQueue))
            return;
        
        ResizableMessageQueue rmq = (ResizableMessageQueue) this.mq;
        
        if ((this.mq instanceof BlockingMessageQueue))
            ((BlockingMessageQueue) this.mq).setBlockingTimeout(1);
        
        rmq.resize(1 * (this.isByteQueue ? 16 : 1));
        Assert.assertEquals(1 * (this.isByteQueue ? 16 : 1), rmq.size());
        
        // Now insert one item (no problem)
        this.mq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
        
        // Now insert one more item (breaks)
        try
        {
            this.mq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
            Assert.fail("Insertion succeeded where it should have failed. Offending queue: " + this.mq);
        }
        catch (TrapException e)
        {
            // Success
        }
        
        // Now resize to two
        rmq.resize(2 * (this.isByteQueue ? 16 : 1));
        
        // Now insert one item (no problem)
        this.mq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
        
    }
    
    @Test(timeout = 10000)
    public void testBlockingTimeout() throws Exception
    {
        // Uncontented test on multiple readers and writers, stresses concurrency support.
        if (!(this.mq instanceof BlockingMessageQueue))
            return;
        
        // Create really small queue
        final BlockingMessageQueue bmq = (BlockingMessageQueue) this.newMQ(1);
        
        // With a decent timeout
        bmq.setBlockingTimeout(20);
        Assert.assertEquals(20, bmq.blockingTimeout());
        
        // And fill it
        bmq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
        
        ThreadPool.executeAfter(new Runnable() {
            
            public void run()
            {
                bmq.pop();
            }
        }, 10);
        
        // Put one more message (should block and release)
        bmq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
        
        // Next removal will be after an excessively long time
        ThreadPool.executeAfter(new Runnable() {
            
            public void run()
            {
                bmq.pop();
            }
        }, 60);
        
        try
        {
            // Put one more message (should block and fail)
            bmq.put(new TrapMessageImpl().setOp(Operation.MESSAGE));
            Assert.fail("Insertion succeeded where it should have failed on: " + this.mq + " with class " + this.mq.getClass());
        }
        catch (TrapException e)
        {
            // Success
        }
    }
    
    private Runnable createWriter(final int start, final int numMsgs)
    {
        final TerminationFlag mtf = this.tf;
        final MessageQueue mmq = this.mq;
        return new Runnable() {
            
            public void run()
            {
                for (int i = 0; i < numMsgs; i++)
                    try
                    {
                        if (mtf.terminate)
                            return;
                        
                        mmq.put(new TrapMessageImpl().setMessageId(i + start).setOp(Operation.MESSAGE));
                    }
                catch (TrapException e)
                {
                }
                catch (Exception e)
                {
                }
                
            }
        };
    }
    
    private Runnable createPeeker()
    {
        final TerminationFlag mtf = this.tf;
        final MessageQueue mmq = this.mq;
        return new Runnable() {
            
            public void run()
            {
                while (!mtf.terminate)
                    mmq.peek();
            }
        };
    }
    
    private Runnable createPopper(final int numMsgs)
    {
        final TerminationFlag mtf = this.tf;
        final MessageQueue mmq = this.mq;
        return new Runnable() {
            
            public void run()
            {
                for (int i = 0; i < numMsgs; i++)
                {
                    if (mtf.terminate)
                        return;
                    if (mmq.pop() == null)
                        i--;
                }
            }
        };
    }
    
}
