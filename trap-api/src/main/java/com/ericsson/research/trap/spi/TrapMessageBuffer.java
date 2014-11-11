package com.ericsson.research.trap.spi;

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
 * A TrapMessageBuffer is a circular buffer with reordering capabilities for {@link TrapMessage} objects. Messages are
 * inserted using put() in any random order, and accessed using fetch() in sequential order.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface TrapMessageBuffer
{
    
    /**
     * Accessor for the number of objects available for reading.
     * 
     * @return The number of objects in the buffer at this point in time.
     */
    public abstract int available();
    
    /**
     * Inserts a message into the buffer. The insertion is such that it is in the correct order in the buffer as per the
     * message ID. Multiple invocations for the same message ID are discarded silently.
     * 
     * @param m
     *            The message to insert.
     * @param t
     *            The transport it was transported on.
     * @throws IllegalArgumentException
     *             If the message doesn't fit in the buffer.
     */
    public abstract void put(TrapMessage m, TrapTransport t) throws IllegalArgumentException;
    
    /**
     * Fetches the next available message. For performance reasons, this method requires a target to fetch into. This is
     * an atomic operation. <b>The fetched object is removed from the buffer</b>
     * 
     * @param target
     *            The target object to fetch into. The <i>m</i> and </i>t</i> properties will be overwritten.
     * @return <i>true</i> if a message was fetched, <i>false</i> otherwise.
     */
    public abstract boolean fetch(TrapEndpointMessage target);
    
}
