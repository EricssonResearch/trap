package com.ericsson.research.trap.delegates;

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



import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapObject;

/**
 * Optional interface to be implemented by applications that wish to use the optimised loopback transport. This feature
 * allows the application to send and receive objects directly when possible. Such usage completely eliminates the
 * serialisation and deserialisation step of objects, providing a significant performance increase for applications.
 * <p>
 * Trap will use the callback method in this function when it receives a TrapObject from the remote side. This is
 * generally only applicable within the same JVM, although an RMI transport is possible. Trap does not have any logic to
 * deserialise objects; as such, if a TrapObject needs to be serialized as part of the transfer from the sending to
 * receiving endpoint, it will be received on the {@link OnData} delegate.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface OnObject extends TrapDelegate
{
    /**
     * Called when the Trap endpoint has received an object (i.e. non-serialized data) from a loopback or other form of
     * function call.
     * <p>
     * Only one of {@link OnData#trapData(byte[], int, TrapEndpoint, Object)} and
     * {@link #trapObject(TrapObject, int, TrapEndpoint, Object)} will be called by Trap for each incoming message.
     * Thus:
     * <ul>
     * <li>If the message received contains a TrapObject, this method will be called; else
     * <li>If the message received contains binary data, trapData will be called.
     * </ul>
     * 
     * @param object
     *            The object sent from the other endpoint.
     * @param endpoint
     *            The TrapEndpoint object that received the object
     * @param context
     *            An application-supplied context for this listener.
     * @param channel
     *            The channel ID on which this object was received.
     */
    public void trapObject(TrapObject object, int channel, TrapEndpoint endpoint, Object context);
    
}
