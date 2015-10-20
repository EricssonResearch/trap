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

/**
 * Port of the Java Lock class to allow 1.4 and earlier compatibility.
 * 
 * @author Vladimir Katardjiev
 */
public abstract class Lock
{
    
    /**
     * Creates a new Lock using the most applicable implementation.
     * 
     * @return A newly instantiated Lock.
     */
    public static Lock createLock()
    {
        try
        {
            Class<?> c = Class.forName(Lock.class.getName() + "Impl");
            return (Lock) c.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Attempts to lock the lock, blocking execution if someone else already owns it.
     */
    public abstract void lock();
    
    /**
     * Attempts to lock, allowing InterruptedExceptions to occur.
     * 
     * @throws InterruptedException
     */
    public abstract void lockInterruptibly() throws InterruptedException;
    
    /**
     * Attempts to lock. Does not block execution.
     * 
     * @return <i>true</i> if a lock was acquired, <i>false</i> otherwise.
     */
    public abstract boolean tryLock();
    
    /**
     * Attempts to lock, waiting up to <i>waitTime</i> msec.
     * 
     * @param waitTime
     *            The number of msec to wait max for a lock.
     * @return <i>true</i> if a lock was acquired, <i>false</i> otherwise.
     * @throws InterruptedException
     *             If the thread was interrupted during the wait.
     */
    public abstract boolean tryLock(long waitTime) throws InterruptedException;
    
    /**
     * Unlocks a currently owned lock.
     */
    public abstract void unlock();
    
    Lock()
    {
    }
}
