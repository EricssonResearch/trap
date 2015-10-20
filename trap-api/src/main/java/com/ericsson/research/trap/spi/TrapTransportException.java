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
import java.util.LinkedList;

import com.ericsson.research.trap.TrapException;

/**
 * Denotes an exception that occurred within the transport, generally when trying to send messages.
 * 
 * @author Vladimir Katardjiev
 */
public class TrapTransportException extends TrapException
{
    
    private static final long            serialVersionUID = 1L;
    
    /**
     * The state of the transport at the time of the exception. The transport may change state after the exception is
     * thrown, making this field critical in determining why the transport did not send the messages.
     */
    public final TrapTransportState      state;
    
    /**
     * The messages that failed sending due to this exception, if any. May be empty. May not be null.
     */
    public final Collection<TrapMessage> failedMessages;
    
    /**
     * Creates a transport exception with a message
     * 
     * @param message
     *            The message that failed sending.
     * @param state
     *            The state of the transport at the time of failure.
     */
    public TrapTransportException(TrapMessage message, TrapTransportState state)
    {
        super("An error occurred while trying to use the transport");
        this.failedMessages = new LinkedList<TrapMessage>();
        
        if (message != null)
            this.failedMessages.add(message);
        
        this.state = state;
    }
    
    /**
     * Creates a transport exception with a collection of messages
     * 
     * @param failedMessages
     *            The messages that failed to send.
     * @param state
     *            The state of the transport at the time of failure
     */
    public TrapTransportException(Collection<TrapMessage> failedMessages, TrapTransportState state)
    {
        super("An error occurred while trying to use the transport");
        
        if (failedMessages != null)
            this.failedMessages = failedMessages;
        else
            this.failedMessages = new LinkedList<TrapMessage>();
        
        this.state = state;
    }
    
}
