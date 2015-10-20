package com.ericsson.research.trap;

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

/**
 * This interface has been superseded by the corresponding delegate in the <i>com.ericsson.research.trap.delegates</i>
 * package. The trapData signature has also been renamed. Code should be updated correspondingly.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 * @deprecated Renamed to {@link com.ericsson.research.trap.delegates.TrapEndpointDelegate}
 */
public interface TrapEndpointDelegate
{
    /**
     * Called when the Trap endpoint has received byte data from the other end.
     * This method executes in a Trap thread, so it should only perform minimal
     * operations before returning, in order to allow for maximum throughput.
     * <p>
     * If asynchronous, this method may be called concurrently for each channel,
     * but it may only be called simultaneously by one thread per channel. If
     * the application does not explicitly utilise channels when sending
     * messages, this function will only be called by one thread at a time.
     * 
     * @param data
     *            The data received. The ownership of this array is transferred
     *            to the receiver. It will not be overwritten by Trap, and Trap
     *            will not read any changes off of it.
     * @param endpoint
     *            The TrapEndpoint that received the data.
     * @param context
     *            The original context object supplied to the TrapEndpoint for
     *            this delegate.
     * @param channel
     *            The Channel ID on which this data was received.
     */
    public void trapData(byte[] data, TrapEndpoint endpoint, Object context);
    
    /**
     * Called when Trap changes state. Includes both the new state, and the
     * previous one.
     * 
     * @param newState
     *            The new state of the TrapEndpoint <code>endpoint</code>.
     * @param oldState
     *            The previous state (the one right before the change). The
     *            combination of new/old state yields useful information on what
     *            the context is of the endpoint.
     * @param endpoint
     *            The TrapEndpoint instance that changed its state. Useful for
     *            when a single delegate manages multiple endpoints.
     * @param context
     *            The caller-specific context that was provided when the
     *            delegate was registered.
     */
    public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context);
    
    /**
     * Called when a Trap Endpoint knows it has failed to send some messages.
     * This can occur when the Trap Endpoint is killed forcibly, loses all its
     * transports while still having an outgoing buffer, or fails to wake up a
     * client that has disconnected all its transports normally.
     * <p>
     * Note that there are conditions when Trap may unwittingly lose data (such
     * as data sent during a switch from unauthenticated -> authenticated
     * session, when the authentication is triggered from the remote side), so
     * the sum of data received by the other end, and called on this method, may
     * be different. Additionally, there may be instances where data supplied by
     * this method was received on the other side, but the successful transfer
     * was not acknowledged.
     * <p>
     * In most cases, the objects in <code>datas</code> have most probably
     * failed to be sent properly, and are supplied back to the application. The
     * application may then inspect to see what failed, or discard it all.
     * 
     * @param datas
     *            A collection of transportable objects that failed sending.
     *            Usually byte arrays, but may contain TrapObject instances.
     * @param endpoint
     *            The TrapEndpoint that failed sending.
     * @param context
     *            An application-supplied context for this listener.
     */
    public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context);

}
