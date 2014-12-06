package com.ericsson.research.trap.utils.impl;

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

import com.ericsson.research.trap.utils.Callback;

public class SingleCallback<T> implements Callback<T>
{
    
    private com.ericsson.research.trap.utils.Callback.SingleArgumentCallback<T> callback;
    private T                                                                   value = null;
	private boolean	               												called	= false;
    
    public SingleCallback<T> callback(T value)
    {
        SingleArgumentCallback<T> cb;
        synchronized (this)
        {
            if (called)
                return this; // Do nothing if we already were called
                
            this.value = value;
            called = true;
            
            if (this.callback == null)
            {
                this.notifyAll();
                return this;
            }
            
            cb = this.callback;
        }
        cb.receiveSingleArgumentCallback(value);
        return this;
    }
    
    public T get() throws InterruptedException
    {
        return this.get(Long.MAX_VALUE);
    }
    
    public T get(long timeout) throws InterruptedException
    {
        long start = System.currentTimeMillis();
        long elapsed;
        
        synchronized (this)
        {
            while (!called)
            {
                elapsed = System.currentTimeMillis() - start;
                long remaining = timeout - elapsed;
                
                if (remaining <= 0)
                    break;
                
                this.wait(remaining);
            }
            
            return this.value;
        }
        
    }
    
    public void setCallback(com.ericsson.research.trap.utils.Callback.SingleArgumentCallback<T> callback)
    {
        T val;
        SingleArgumentCallback<T> cb;
        synchronized (this)
        {
            this.callback = callback;
            
            if (!called)
                return;
            
            // Retrieve the results outside of synchronized to prevent deadlocks.
            val = this.value;
            cb = callback;
        }
        cb.receiveSingleArgumentCallback(val);
    }

    public boolean isDone()
    {
	    return called;
    }
    
}
