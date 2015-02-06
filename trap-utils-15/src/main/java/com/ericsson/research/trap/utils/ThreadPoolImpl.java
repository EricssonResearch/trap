package com.ericsson.research.trap.utils;

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

import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolImpl extends ThreadPool
{
    
    protected int                         CACHED_THREADS_MIN     = 50;
    protected int                         CACHED_THREADS_MAX     = 100;
    protected int                         CACHED_THREADS_TIMEOUT = 60 * 1000;
    
    protected int                         FIXED_THREADS          = 10;
    
    protected int                         SCHEDULED_THREADS      = 10;
    
    protected ThreadPoolExecutor          cachedPool             = new ThreadPoolExecutor(this.CACHED_THREADS_MIN, this.CACHED_THREADS_MAX, this.CACHED_THREADS_TIMEOUT, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(5000));
    protected ThreadPoolExecutor          fixedPool              = new ThreadPoolExecutor(this.FIXED_THREADS, this.FIXED_THREADS, Long.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    protected ScheduledThreadPoolExecutor scheduledPool          = new ScheduledThreadPoolExecutor(this.SCHEDULED_THREADS);
    
    protected DelayQueue<WeakDelay>       weakFutureTasks        = new DelayQueue<WeakDelay>();
    protected Runnable                    weakFutureExecutor     = null;
    
    static ThreadPoolImpl                 lastInstance           = null;
    
    public ThreadPoolImpl()
    {
        this.fixedPool.prestartAllCoreThreads();
        this.scheduledPool.prestartAllCoreThreads();
        this.cachedPool.setMaximumPoolSize(this.CACHED_THREADS_MAX);
        this.cachedPool.allowCoreThreadTimeOut(true);
        lastInstance = this;
    }
    
    public static String describeState()
    {
        if (lastInstance == null)
            return "";
        String cachedStatus = "cachedPool: " + describeState(lastInstance.cachedPool);
        String fixedStatus = "fixedPool: " + describeState(lastInstance.fixedPool);
        String scheduledStatus = "scheduledPool: " + describeState(lastInstance.scheduledPool);
        
        return cachedStatus + "\n" + fixedStatus + "\n" + scheduledStatus;
    }
    
    private static String describeState(ThreadPoolExecutor pool)
    {
        return pool.getActiveCount() + " active, " + pool.getPoolSize() + " pooled, " + pool.getMaximumPoolSize() + " max. " + pool.getQueue().size() + " queued";
    }
    
    @Override
    protected Future performSchedule(Runnable task, long delay)
    {
        return new FutureImpl(this.scheduledPool.schedule(task, delay, TimeUnit.MILLISECONDS));
    }
    
    @Override
    protected void performExecuteFixed(Runnable task)
    {
        try
        {
            // Yes, this function has ONE line of non-exception handling code.
            this.fixedPool.submit(task);
        }
        catch (OutOfMemoryError e)
        {
            // The fixed pool uses an unbounded queue
            // This kind of execution is implying we're exceeding the memory bounds significantly
            // which may be unrecoverable. We'll introduce a short delay on this level and hope
            // this allows the system to recover.
            try
            {
                System.out.println("Out Of Memory");
                // Trigger gc
                System.gc();
                Thread.sleep(10);
                
                // Run in the same thread. This throttles incoming requests naturally.
                // This may throw an oom error itself, but that's not our concern
                task.run();
                return;
            }
            catch (InterruptedException ie)
            {
                throw e;
            }
            catch (RejectedExecutionException e2)
            {
                // Do nothing
            }
            throw e;
        }
    }
    
    @Override
    protected void performExecuteCached(Runnable task)
    {
        try
        {
            // Yes, this function has ONE line of non-exception handling code.
            this.cachedPool.submit(task);
        }
        catch (RejectedExecutionException e)
        {
            // The cached thread pool uses a bounded queue of size 1000; we can try to submit a few times. Failing that, we'll throw.
            for (int i = 1; i < 3; i++)
            {
                
                try
                {
                    Thread.yield();
                    Thread.sleep(i);
                    Thread.yield();
                    this.cachedPool.submit(task);
                    return;
                }
                catch (InterruptedException ie)
                {
                    throw e;
                }
                catch (RejectedExecutionException e2)
                {
                    // Do nothing
                    System.out.println("Execution rejected for task; running it in-thread. PoolSize is: " + this.cachedPool.getQueue().size());
                }
            }
            task.run();
        }
        catch (OutOfMemoryError e)
        {
            System.gc();
            // We've run out of memory. Where?
            if ((this.cachedPool.getActiveCount()) < this.CACHED_THREADS_MAX)
            {
                // This may be a cause... We're definitely running lower than the max limit. We should set a lower limit.
                try
                {
                    /*
                     * Reduces the pool maximum size. Note the +1 on the if statement and -1 on the cached statement. This ensures we'll
                     * aggressively downsize on out of memory error, and we'll error out of this if we start a thread downsizing loop.
                     * The values are never equal (always strictly less) and there's a minimum lower bound.
                     */
                    this.setCachedMaxThreads(this.cachedPool.getActiveCount() - 1);
                }
                catch (IllegalArgumentException e1)
                {
                    // The new pool size was outside the allowed bounds. Clearly, we're extremely low on memory so we'll just rethrow e.
                    throw e;
                }
                
                // We've reduced the pool size to acceptable limits (hopefully). We can try to reschedule.
                try
                {
                    // Give us some time to execute
                    System.gc();
                    Thread.sleep(100);
                    
                    // Retry the task.
                    this.performExecuteCached(task);
                }
                catch (InterruptedException e1)
                {
                }
            }
            else
            {
                // The queue ran out of memory while trying to grow. The queue size is not dynamically bounded, so
                // we have no choice but to throw this error.
                throw e;
            }
            
            //TODO: A more paranoid version may choose here to reallocate the spareSpace variable.
            // I won't.
        }
    }
    
    protected void setCachedMaxThreads(int newThreads)
    {
        this.CACHED_THREADS_MAX = newThreads;
        this.cachedPool.setMaximumPoolSize(newThreads);
        System.out.println("Setting new thread size: " + newThreads);
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        System.out.println("Final...");
        super.finalize();
        this.cachedPool.shutdownNow();
        this.fixedPool.shutdownNow();
        this.scheduledPool.shutdownNow();
        System.gc();
    }
    
    @Override
    protected Future performWeakExecuteAfter(Runnable task, long delay)
    {
        RunnableFuture future = new RunnableFuture(task);
        WeakDelay delayed = new WeakDelay(future, delay);
        
        this.weakFutureTasks.add(delayed);
        synchronized (this)
        {
            
            if (this.weakFutureExecutor == null)
            {
                this.weakFutureExecutor = new Runnable() {
                    
                    public void run()
                    {
                        try
                        {
                            while (!ThreadPoolImpl.this.weakFutureTasks.isEmpty())
                            {
                                WeakDelay delayed = ThreadPoolImpl.this.weakFutureTasks.poll(1, TimeUnit.HOURS);
                                executeFixed(delayed);
                            }
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        finally
                        {
                            synchronized (ThreadPoolImpl.this)
                            {
                                ThreadPoolImpl.this.weakFutureExecutor = null;
                            }
                        }
                    }
                };
                this.performExecuteCached(this.weakFutureExecutor);
            }
            
        }
        
        return future;
    }
    
}
