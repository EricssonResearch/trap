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
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;

/**
 * Interface to be implemented by the object responsible for dealing with new incoming Trap connections. This delegate
 * will only be called for new incoming connections; reconnects to existing transports will be handled automatically by
 * the Trap library.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface OnAccept extends TrapDelegate
{
    
    /**
     * Called on an incoming Trap connection. The new endpoint is in {@link TrapState#OPEN}, and can immediately be used
     * to send messages. Messages will not begin to be dispatched to this endpoint until after this method has returned.
     * Like most other callbacks, this should execute and return quickly.
     * <p>
     * The endpoint received using this method is only <b>weakly referenced</b> by Trap, and is thus eligible for
     * garbage collection. Delegates <b>must store a strong reference</b> to the endpoint if they wish to use it.
     * <p>
     * While the endpoint is ready-to-use, it is blocked until the method returns. Consequently, all
     * potentially-destructive operations on the endpoint, such as
     * {@link TrapEndpoint#setQueue(com.ericsson.research.trap.spi.MessageQueue)} can safely be performed at this time.
     * These methods should not be called after this method has returned, however.
     * 
     * @param endpoint
     *            The new TrapEndpoint representing the created connection.
     * @param listener
     *            The TrapListener that called this delegate. Useful for when a delegate manages multiple listeners.
     * @param context
     *            Caller-specific context that was supplied when the delegate was registered.
     */
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context);
}
