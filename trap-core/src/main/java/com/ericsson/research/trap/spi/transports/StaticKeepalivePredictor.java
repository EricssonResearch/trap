package com.ericsson.research.trap.spi.transports;

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
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.research.trap.TrapKeepalivePolicy;
import com.ericsson.research.trap.spi.TrapKeepaliveDelegate;
import com.ericsson.research.trap.spi.TrapKeepalivePredictor;
import com.ericsson.research.trap.utils.Future;
import com.ericsson.research.trap.utils.HexDump;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.UUID;

public class StaticKeepalivePredictor implements TrapKeepalivePredictor, Runnable
{
    
    protected int                                keepaliveInterval   = TrapKeepalivePolicy.DISABLED;
    
    // Keepalive engine stuff
    /**
     * The current keepalive interval
     */
    protected int                                mKeepaliveInterval  = 5 * 60;
    
    /**
     * Number of seconds to wait at least between keepalives
     */
    protected int                                minKeepalive        = 1;
    
    /**
     * Number of seconds to wait at most between keepalives
     */
    protected int                                maxKeepalive        = 999999;
    
    // Automatic keepalive interval optimisation
    
    protected int                                lastInterval        = this.mKeepaliveInterval;
    
    protected int                                growthStep          = 0;
    
    protected int                                nextInterval        = this.mKeepaliveInterval + this.growthStep;
    
    /**
     * The minimum keepalive value that the automatic keepalive algorithm is allowed to decrease the keepalive to. The
     * auto keepalive algorithm is only active on transports that can connect (i.e. reconnect) if it fails them.
     */
    protected int                                minAutoKeepalive    = 1;
    
    /**
     * The minimum keepalive value that the automatic keepalive algorithm is allowed to decrease the keepalive to. The
     * auto keepalive algorithm is only active on transports that can connect (i.e. reconnect) if it fails them. In
     * addition, if keepalivepolicy != default, the automatic algorithm is disabled, as well as when min/max auto
     * keepalives are negative numbers.
     */
    protected int                                maxAutoKeepalive    = 28 * 60;
    
    /**
     * Timestamp of last recorded keepalive received
     */
    protected long                               lastDataReceived    = 0;
    protected long                               lastDataSent        = 0;
    protected long                               lastSentKeepalive   = 0;
    
    protected Future                             keepaliveTask       = null;
    protected long                               keepaliveTaskTime   = 0;
    protected long                               keepaliveExpiryMsec = 5000;
    
    /**
     * Byte array containing the most recently sent keepalive message from this predictor.
     */
    private byte[]                               keepaliveData       = null;
    
    boolean                                      started             = false;
    
    private WeakReference<TrapKeepaliveDelegate> delegate;
    
    protected Logger                       logger              = LoggerFactory.getLogger(this.getClass());
    
    public void setMinKeepalive(int min)
    {
        this.minKeepalive = min;
    }
    
    public void setMaxKeepalive(int max)
    {
        this.maxKeepalive = max;
    }
    
    public void setMinAutoKeepalive(int min)
    {
        this.minAutoKeepalive = min;
    }
    
    public void setMaxAutoKeepalive(int max)
    {
        this.maxAutoKeepalive = max;
    }
    
    public synchronized void setKeepaliveInterval(int interval)
    {
        this.keepaliveInterval = interval;
        
        if (interval == TrapKeepalivePolicy.DEFAULT)
            this.nextInterval = this.mKeepaliveInterval;
        else if (interval == TrapKeepalivePolicy.DISABLED)
            this.nextInterval = -1;
        else
        {
            // Basically, ensure that the interval is within the allowed range
            if ((interval > this.maxKeepalive) || (interval < this.minKeepalive))
                this.nextInterval = this.mKeepaliveInterval;
            else
                this.nextInterval = interval;
        }
        
        // Reschedule if we are started, to take into account the new interval.
        if (this.started)
            this.schedule();
        
    }
    
    public synchronized int getKeepaliveInterval()
    {
        return this.keepaliveInterval;
    }
    
    public synchronized long getNextKeepaliveSend()
    {
        // This is the oldest timestamp of data received or sent. In the case of an asynchronous transfer
        // (e.g. only receiving data), we must even it out both ways to ensure the connection is both-ways-alive.
        long earliestTime = Math.min(this.lastDataReceived, this.lastDataSent);
        long expected = earliestTime + (this.nextInterval * 1000);
        long actual = System.currentTimeMillis();
        return expected - actual;
    }
    
    public void keepaliveReceived(boolean isPing, char pingType, int timer, byte[] data)
    {
        if (this.logger.isTraceEnabled())
            this.logger.trace("Received new keepalive; boolean: {}, pingType: {}, timer: {}, data: {}", new Object[] { isPing, pingType, timer, HexDump.makeStringifyable(data, 0, 40) });
        if (!isPing)
        {
            synchronized (this)
            {
                // Check if this is a PING we have sent
                if (!Arrays.equals(data, this.keepaliveData))
                {
                    if (this.keepaliveData == null)
                    {
                        this.logger.trace("No PING sent; ignoring keepalive");
                        return;
                    }
                    
                    if (data == null)
                    {
                        throw new NullPointerException();
                    }
                    return;
                }
                
                this.keepaliveData = null;
                
                switch (pingType)
                {
                // Keepalives disabled
                
                    case '1':
                        break; // Do nothing; we will not auto-adjust
                        
                    case '2':
                        this.setKeepaliveInterval(timer); // Manual adjustment
                        break;
                    
                    case '3': // Manually triggered keepalive
                        break;
                    
                    default: // no-error
                }
            }
            
            // Now reschedule ourselves. The received keepalive will already have been recorded as per dataReceived()
            this.schedule();
        }
        else
        {
            TrapKeepaliveDelegate dObj = this.delegate.get();
            
            if (pingType == '2')
                this.setKeepaliveInterval(timer);
            
            if (dObj != null)
                dObj.shouldSendKeepalive(false, this.getPingType(), timer, data);
        }
    }
    
    public synchronized long nextKeepaliveReceivedDelta()
    {
        if (this.keepaliveData == null)
            return Long.MAX_VALUE; // NEVER! We haven't sent a keepalive so we don't expect one. DUH.
            
        long expected = this.lastSentKeepalive;
        long actual = System.currentTimeMillis();
        return expected - actual;
        
    }
    
    public void setDelegate(TrapKeepaliveDelegate delegate)
    {
        this.delegate = new WeakReference<TrapKeepaliveDelegate>(delegate);
    }
    
    public void setKeepaliveExpiry(long msec)
    {
        this.keepaliveExpiryMsec = msec;
        this.schedule();
    }
    
    public long getKeepaliveExpiry()
    {
        return this.keepaliveExpiryMsec;
    }
    
    public synchronized void start()
    {
        if (this.started)
        {
            return;
        }
        
        if (this.keepaliveInterval == TrapKeepalivePolicy.DISABLED)
            return; // Don't start
            
        if (this.logger.isTraceEnabled())
            this.logger.trace("Starting keepalive task for transport " + this.delegate.get() + "with predictor " + this + " and policy " + this.keepaliveInterval);
        
        this.keepaliveData = null;
        this.lastSentKeepalive = Long.MAX_VALUE;
        this.keepaliveTaskTime = 0;
        this.lastDataReceived = this.lastDataSent = System.currentTimeMillis();
        
        this.started = true;
        
        this.schedule();
        
    }
    
    public synchronized void stop()
    {
        
        if (!this.started)
            return;
        
        if (this.keepaliveTask != null)
        {
            this.keepaliveTask.cancel();
            this.keepaliveTask = null;
        }
        
        this.started = false;
    }
    
    protected void schedule()
    {
        
        synchronized (this)
        {
            if (this.getKeepaliveInterval() == TrapKeepalivePolicy.DISABLED)
            {
                this.logger.trace("Keepalive timer is disabled...");
                return;
            }
            
            if (!this.started)
            {
                this.logger.trace("Keepalive not started");
                return;
            }
            
            // Next send should auto-disable if there is an outstanding ping/pong waiting
            long nextSend = (this.keepaliveData == null ? this.getNextKeepaliveSend() : Long.MAX_VALUE);
            long nextReceive = this.nextKeepaliveReceivedDelta() + (this.keepaliveData == null ? 0 : this.keepaliveExpiryMsec);
            long msec = Math.min(nextSend, nextReceive);
            
            // Stop the immediate run loop. This stack overflowed anyway...
            if (msec <= 500)
            {
                msec = 501;
            }
            
            long scheduledTime = msec + System.currentTimeMillis();
            
            // no-op: we want to schedule for longer time than the current expiry (expiry will re-schedule)
            // cancel: we want to schedule for shorter time than the current expiry
            if (this.keepaliveTask != null)
            {
                
                // Already happened
                if (this.keepaliveTaskTime >= System.currentTimeMillis())
                {
                    
                    // Ensure we don't schedule if a task is going to happen closer, but in the future
                    // Allow a small absolute time shift to prevent tasks from being pre-empted too often.
                    if ((this.keepaliveTaskTime <= (scheduledTime + 250)) && (this.keepaliveTaskTime > System.currentTimeMillis()))
                    {
                        if (this.logger.isTraceEnabled())
                            this.logger.trace(this + " (cached) Next keepalive due at " + new Date(this.keepaliveTaskTime));
                        return;
                    }
                    this.logger.trace("Cancelling existing task...");
                    
                    this.keepaliveTask.cancel();
                }
            }
            
            this.keepaliveTaskTime = scheduledTime;
            this.keepaliveTask = ThreadPool.executeAt(new WeakRunnable(this), scheduledTime);
            
            if (this.logger.isTraceEnabled())
                this.logger.trace(this + " Next keepalive wakeup at " + new Date(this.keepaliveTaskTime));
        }
    }
    
    public synchronized void run()
    {
        TrapKeepaliveDelegate delegate = this.delegate.get();
        this.logger.trace("Keepalive task timer expired. Now checking keepalives for delegate {} and policy {}.", new Object[] { delegate, this.getKeepaliveInterval() });
        
        if (delegate == null)
        {
            this.stop();
            return; // Delegate garbage collected; nothing to keep notifying about
        }
        
        // Check if we have been disabled...
        if (this.getKeepaliveInterval() == TrapKeepalivePolicy.DISABLED)
        {
            this.stop();
            return;
        }
        
        // Now check for timeout
        // This is for RECEIVING.
        long msec = this.nextKeepaliveReceivedDelta();
        this.logger.trace("Receive MSEC calculation: {}", Long.valueOf(msec));
        
        if ((msec <= 500) && (-msec > this.keepaliveExpiryMsec))
        {
            this.logger.trace("Notifying delegate that predicted keepalive has expired.");
            delegate.predictedKeepaliveExpired(this, -msec);
            this.stop();
            return;
        }
        
        // Is it time to send a keepalive?
        
        msec = this.getNextKeepaliveSend();
        this.logger.trace("Send MSEC calculation: {}", Long.valueOf(msec));
        if (msec <= 500)
        {
            
            if (this.keepaliveData != null)
            {
                // OOps?
                this.logger.trace("EXPERIMENTAL: keepalive data != null when expired timer... Dropping sending a keepalive.");
            }
            else
            {
                this.keepaliveData = StringUtil.toUtfBytes(UUID.randomUUID());
                this.lastSentKeepalive = System.currentTimeMillis();
                this.lastInterval = this.nextInterval;
                delegate.shouldSendKeepalive(true, this.getPingType(), this.nextInterval, this.keepaliveData);
                
            }
        }
        
        // reschedule ourselves for a default time
        this.schedule();
    }
    
    private char getPingType()
    {
        char type = '1';
        
        if (this.getKeepaliveInterval() > 0)
            type = '2';
        
        return type;
    }
    
    protected void finalize()
    {
        this.logger.trace("Keepalive predictor {} was garbage collected and will not predict anything else...", this);
    }
    
    public String toString()
    {
        return "StaticKeepalivePredictor@" + this.hashCode();
    }
    
    public synchronized void dataReceived()
    {
        this.lastDataReceived = System.currentTimeMillis();
    }
    
    public synchronized void dataSent()
    {
        this.lastDataSent = System.currentTimeMillis();
    }
}

class WeakRunnable implements Runnable
{
    
    private final WeakReference<Runnable> runnable;
    
    public WeakRunnable(Runnable r)
    {
        this.runnable = new WeakReference<Runnable>(r);
    }
    
    public void run()
    {
        Runnable r = this.runnable.get();
        
        if (r != null)
            r.run();
    }
    
}
