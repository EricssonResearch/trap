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
 * Acts as an enum for the possible states of a transport.
 * 
 * @author Vladimir Katardjiev
 */
public final class TrapTransportState
{
    /**
     * Not connected
     */
    public static final TrapTransportState DISCONNECTED  = new TrapTransportState("DISCONNECTED");
    
    /**
     * Attempting to connect
     */
    public static final TrapTransportState CONNECTING    = new TrapTransportState("CONNECTING");
    
    // Transport is CONNECTED but is not AVAILABLE (= needs OPEN)
    /**
     * Connected to remote endpoint, but is not available. AN OPEN message must be sent.
     */
    public static final TrapTransportState CONNECTED     = new TrapTransportState("CONNECTED");
    
    /**
     * The transport is available for sending.
     */
    public static final TrapTransportState AVAILABLE     = new TrapTransportState("AVAILABLE");
    
    /**
     * The transport is temporarily unavailable for sending.
     */
    public static final TrapTransportState UNAVAILABLE   = new TrapTransportState("UNAVAILABLE");
    
    /**
     * The transport is disconnecting.
     */
    public static final TrapTransportState DISCONNECTING = new TrapTransportState("DISCONNECTING");
    
    /**
     * An error occurred, and this transport has ceased execution.
     */
    public static final TrapTransportState ERROR         = new TrapTransportState("ERROR");
    
    private final String                   string;
    
    /**
     * Provides a string representation of the state; the state's name. This is used primarily for logging and debugging
     * purposes, as it serves no other purpose.
     * 
     * @return The name of the state.
     */
    public final String toString()
    {
        return this.string;
    }
    
    private TrapTransportState(String string)
    {
        this.string = string;
    }
    
}
