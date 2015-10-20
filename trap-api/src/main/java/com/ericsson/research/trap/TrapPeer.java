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
 * Interface used to maintain peer to peer Trap connections. In this case, the caller provides an out-of-band channel
 * for Trap to handshake on.
 * 
 * @author Vladimir Katardjiev
 * @since 1.2
 */
public interface TrapPeer extends TrapEndpoint
{
    /**
     * Callback interface to be implemented by the caller. TrapPeers will call this method when they must send
     * out-of-band data to their remote peer. This channel must be established for the duration of the session, as it
     * will be used to re-establish a lost connection.
     * 
     * @author Vladimir Katardjiev
     */
    public interface TrapPeerChannel
    {
        /**
         * Called by Trap to ask for the transfer of the given arbitrary data to the remote peer.
         * 
         * @param data
         *            The data to transfer
         */
        public void sendToRemote(byte[] data);
    }
    
    /**
     * Provide data from the remote peer to this one.
     * 
     * @param data
     *            The data, as received from the remote peer.
     */
    public void receive(byte[] data);
    
    /**
     * Opens the peer to peer connection, expecting connections to go over the specific channel <i>channel</i>.
     * 
     * @param channel
     *            The external communications channel. Trap will use this to communicate out-of-band messages necessary
     *            to establish the connection.
     * @throws TrapException
     *             If an error occurs in allocating the resources needed to establish the peer connection.
     */
    public void open(TrapPeerChannel channel) throws TrapException;
}
