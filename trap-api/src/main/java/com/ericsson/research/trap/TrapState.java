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



/**
 * TrapState defines the set of states that TrapEndpoints can have. Uses a class as opposed to an enum in order to allow
 * compatibility with Java versions that lack enums. In all other aspects, acts as an enum.
 * 
 * @author Vladimir Katardjiev
 */
public enum TrapState
{
    
    /**
     * The Trap Endpoint is closed. Client and Listener Trap Endpoints can be opened from this state; any other endpoint
     * cannot and this is its final state.
     */
    CLOSED,
    
    /**
     * The endpoint is in the process of establishing a connection with a remote endpoint.
     */
    OPENING,
    
    /**
     * The Trap Session is open and, to the best of the Endpoint's knowledge, underlying transports are available. The
     * application can use this Endpoint to send data freely.
     */
    OPEN,
    
    /**
     * When the TrapState is SLEEPING, the TrapEndpoint does not currently have an active underlying transport to the
     * other endpoint. Within its configuration, however, it has reason to believe that this session can be resumed.
     * Sleeping will occur in one of the following cases:
     * <ul>
     * <li>The trap endpoint has lost all current transports to the other trap endpoint, but believes it can
     * re-establish them. If it is a server endpoint, it will attempt to wake up the client and/or wait for the client
     * to reconnect within the given time intervals in its configuration.
     * <li>The Trap Endpoint has negotiated a connectionless state with the remote peer to save power. The endpoint will
     * attempt to re-establish the connection once there is any data to be sent or received, but until then remain idle.
     * </ul>
     * While SLEEPING, the application can still use the TrapEndpoint's API and queue messages for sending. Those
     * messages will be sent once the Endpoint is OPEN.
     */
    SLEEPING,
    
    /**
     * An error caused the Trap Endpoint to close abnormally. This endpoint can no longer be re-used, but it can be
     * queried to ascertain why it failed.
     */
    ERROR,
    
    /**
     * The Trap Endpoint is attempting to close the Trap session.
     */
    CLOSING;
}
