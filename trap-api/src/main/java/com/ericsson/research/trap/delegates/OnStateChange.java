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
import com.ericsson.research.trap.TrapState;

/**
 * Implement this interface to receive callbacks when the Trap Endpoint changes its state. Note that this delegate will
 * be called <b>in addition to</b> the more specific delegates (as opposed to instead of).
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface OnStateChange extends TrapDelegate
{
    /**
     * Called when Trap changes state. Includes both the new state, and the previous one.
     * 
     * @param newState
     *            The new state of the TrapEndpoint <code>endpoint</code>.
     * @param oldState
     *            The previous state (the one right before the change). The combination of new/old state yields useful
     *            information on what the context is of the endpoint.
     * @param endpoint
     *            The TrapEndpoint instance that changed its state. Useful for when a single delegate manages multiple
     *            endpoints.
     * @param context
     *            The caller-specific context that was provided when the delegate was registered. Will be <i>null</i> if
     *            no context was supplied.
     */
    public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context);
}
