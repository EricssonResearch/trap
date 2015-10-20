package com.ericsson.research.trap.impl;

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

import java.lang.ref.WeakReference;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.research.trap.utils.ThreadPoolImpl;

public class TrapImplDebugPrinter
{
    
    static HashSet<HashedWeakReference<TrapEndpointImpl>> endpoints        = new HashSet<HashedWeakReference<TrapEndpointImpl>>();
    static Thread                                         runThread        = null;
    static final Logger                                   l                = LoggerFactory.getLogger(TrapImplDebugPrinter.class);
    
    private static long                                    printoutInterval = 5000;
    
    public static void addEndpoint(TrapEndpointImpl ep)
    {
        synchronized (endpoints)
        {
            endpoints.add(new HashedWeakReference<TrapEndpointImpl>(ep));
        }
    }
    
    public static void removeEndpoint(TrapEndpointImpl ep)
    {
        synchronized (endpoints)
        {
            endpoints.remove(new HashedWeakReference<TrapEndpointImpl>(ep));
        }
    }
    
    public static String describe(TrapEndpointImpl src)
    {
        
        String ep = src.toString();
        String mq = src.messageQueue.toString();
        String available = src.availableTransports.toString();
        String all = src.transports.toString();
        
        return ep + "\n" + mq + "\n" + available + "\n" + all;
    }
    
    public synchronized static void start()
    {
        
        if (runThread != null)
            return;
        
        runThread = new Thread(new Runnable() {
            
            public void run()
            {
                for (;;)
                {
                    l.info("Dump Beginning");
                    synchronized (endpoints)
                    {
                        l.info("Threading status: ");
                        l.info(ThreadPoolImpl.describeState());
                        
                        for (WeakReference<TrapEndpointImpl> epr : endpoints)
                        {
                            TrapEndpointImpl ep = epr.get();
                            
                            if (ep != null)
                                l.info(describe(ep));
                        }
                    }
                    l.info("Dump completed. Next in " + getPrintoutInterval() + " ms");
                    
                    try
                    {
                        Thread.sleep(getPrintoutInterval());
                    }
                    catch (InterruptedException e)
                    {
                        l.info("Dump thread " + Thread.currentThread() + " exiting as it was interrupted.");
                        return;
                    }
                    
                    if (runThread != Thread.currentThread())
                    {
                        l.info("Dump thread " + Thread.currentThread() + " exiting as the new run thread is " + runThread);
                        return;
                    }
                }
            }
            
        });
        
        runThread.start();
    }
    
    public synchronized static void stop()
    {
        runThread = null;
    }

    /**
     * @return the printoutInterval
     */
    public static long getPrintoutInterval()
    {
        return printoutInterval;
    }

    /**
     * @param printoutInterval the printoutInterval to set
     */
    public static void setPrintoutInterval(long printoutInterval)
    {
        TrapImplDebugPrinter.printoutInterval = printoutInterval;
    }
}

class HashedWeakReference<T> extends WeakReference<T>
{
    
    private int hash;
    
    public HashedWeakReference(T referent)
    {
        super(referent);
        this.hash = referent.hashCode();
    }
    
    @Override
    public int hashCode()
    {
        return this.hash;
    }
    
}
