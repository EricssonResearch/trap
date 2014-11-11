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



import java.util.Collection;

import com.ericsson.research.trap.TrapEndpoint;

/**
 * Implement this interface to receive notifications when Trap has failed sending some data.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface OnFailedSending extends TrapDelegate
{
    
    /**
     * Called when a Trap Endpoint knows it has failed to send some messages. This can occur when the Trap Endpoint is
     * killed forcibly, loses all its transports while still having an outgoing buffer, or fails to wake up a client
     * that has disconnected all its transports normally.
     * <p>
     * Note that there are conditions when Trap may unwittingly lose data (such as data sent during a switch from
     * unauthenticated -> authenticated session, when the authentication is triggered from the remote side), so the sum
     * of data received by the other end, and called on this method, may be different. Additionally, there may be
     * instances where data supplied by this method was received on the other side, but the successful transfer was not
     * acknowledged.
     * <p>
     * In most cases, the objects in <code>datas</code> have most probably failed to be sent properly, and are supplied
     * back to the application. The application may then inspect to see what failed, or discard it all.
     * 
     * @param datas
     *            A collection of transportable objects that failed sending. Usually byte arrays, but may contain
     *            TrapObject instances.
     * @param endpoint
     *            The TrapEndpoint that failed sending.
     * @param context
     *            An application-supplied context for this listener. Will be <i>null</i> if no context was supplied.
     */
    public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context);
}
