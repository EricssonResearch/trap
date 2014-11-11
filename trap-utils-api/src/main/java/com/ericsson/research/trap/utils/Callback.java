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
 * Provides a generic callback mechanism to handle asynchronous callbacks, while allowing synchronous accesses of them.
 * 
 * @author Vladimir Katardjiev
 * @param <T>
 *            The type of object being provided by the callback
 */
public interface Callback<T>
{
    /**
     * Asynchronous callback interface. Listeners should implement this interface (usually in an anonymous inner class)
     * to receive the results of the callback.
     * 
     * @author Vladimir Katardjiev
     * @param <T>
     *            The type of object being provided by the callback
     */
    public interface SingleArgumentCallback<T>
    {
        /**
         * Called to receive the callback value.
         * 
         * @param result
         *            The callback value
         */
        public void receiveSingleArgumentCallback(T result);
    }
    
    /**
     * Synchronous accessor for the callback. Should not be used together with
     * {@link #setCallback(SingleArgumentCallback)}.
     * 
     * @return The callback value, synchronously
     * @throws InterruptedException
     *             If the thread is interrupted while waiting for the callback.
     */
    public T get() throws InterruptedException;
    
    /**
     * Like {@link #get()} but with a limited timer.
     * 
     * @param timeout
     *            The maximum time to wait.
     * @return The callback value, synchronously.
     * @throws InterruptedException
     *             If the thread is interrupted while waiting for the callback.
     */
    public T get(long timeout) throws InterruptedException;
    
    /**
     * Sets the function that will receive the callback. The function will be called as soon as the callback value is
     * ready.
     * 
     * @param callback
     *            The function to call
     */
    public void setCallback(SingleArgumentCallback<T> callback);
}
