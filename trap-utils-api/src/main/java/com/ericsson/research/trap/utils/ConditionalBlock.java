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

import java.lang.reflect.Constructor;

/**
 * Blocks execution until unblocked. If unblocked, never blocks. Adds a condition that must be matched for the block to
 * be unblocked.
 * 
 * @author Vladimir Katardjiev
 */
public abstract class ConditionalBlock
{
    
    /**
     * Block on the supplied condition. The thread will be unblocked iff the exact same data is supplied to unblock.
     * 
     * @param condition
     *            The data to unblock on.
     */
    public abstract void block(byte[] condition);
    
    /**
     * Attempts to unblock a blocked thread.
     * 
     * @param condition
     *            The condition data to unblock with.
     * @return <i>true</i> if the thread was unblocked, <i>false</i> if the data did not match.
     */
    public abstract boolean unblock(byte[] condition);
    
    /**
     * Resets this block.
     */
    public abstract void reset();
    
    /**
     * Gets the current timeout.
     * 
     * @return The timeout in milliseconds
     */
    public abstract long getTimeout();
    
    /**
     * Sets a new timeout. The timeout is the maximum number of milliseconds a block will remain blocked.
     * 
     * @param timeout
     *            The new timeout, in msec.
     */
    public abstract void setTimeout(long timeout);
    
    /**
     * Sets the unblock condition. Should only be used for debugging or in extreme emergencies.
     * 
     * @param condition
     *            The new condition data
     */
    public abstract void setCondition(byte[] condition);
    
    private static Class<?>       cBlockClass       = null;
    private static Constructor<?> cBlockConstructor = null;
    
    /**
     * Creates a new ConditionalBlock
     * 
     * @param name
     *            The name of the new block
     * @return A new ConditionalBlock instance, ready to use.
     */
    public static ConditionalBlock create(String name)
    {
        try
        {
            
            if (cBlockClass == null)
                cBlockClass = Class.forName(ConditionalBlock.class.getName() + "Impl");
            
            if (cBlockConstructor == null)
                cBlockConstructor = cBlockClass.getConstructor(new Class[] { String.class });
            
            return (ConditionalBlock) cBlockConstructor.newInstance(new Object[] { name });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    // Prevent javadoc
    ConditionalBlock()
    {
    }
}
