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



import java.util.Collection;

/**
 * Interface to be implemented by objects that wish to receive TrapTransport callbacks. Generally, this means
 * TrapEndpoints. It is not necessary to implement this interface to use Trap.
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapTransportDelegate
{
    /**
     * Called when a Trap Transport has received a TrapMessage.
     * 
     * @param message
     *            The message received
     * @param transport
     *            The transport the message was received on
     * @param context
     *            A context object supplied earlier
     */
    public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context);
    
    /**
     * Called when the Trap Transport changes state.
     * 
     * @param newState
     *            The new state of the transport
     * @param oldState
     *            The old state of the transport
     * @param context
     *            A context object supplied prior
     * @param transport
     *            The transport that changed states
     */
    public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context);
    
    /**
     * Called when the Trap Transport knows that it has failed to send message(s)
     * 
     * @param messages
     *            A collection of {@link TrapMessage} objects that were not successfully sent.
     * @param transport
     *            The transport that failed sending.
     * @param context
     *            A context object supplied beforehand.
     */
    public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context);
    
    /**
     * Callback when the Trap transport knows that it has managed to send the message (Acknowledgement received from the
     * remote side)
     * 
     * @param message
     *            The message sent
     * @param transport
     *            The transport it was sent on.
     * @param context
     *            The context object supplied.
     */
    public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context);
    
    /**
     * Callback when the endpoint needs to transport a message for the transport (i.e. peer to peer transports)
     * @param message The message to transport
     * @param transport The transport that needs it
     * @param context The context object supplied
     */
    public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context);
    
}
