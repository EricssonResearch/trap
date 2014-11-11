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



import com.ericsson.research.trap.delegates.TrapDelegate;

/**
 * A TrapClient is a TrapEndpoint capable of opening an outgoing connection, commonly to a TrapListener. It is able to
 * reconnect transports at will, unlike a ServerTrapEndpoint. Create a TrapClient using
 * {@link TrapFactory#createClient(String, boolean)} as follows.
 * <p>
 * 
 * <pre>
 * TrapClient c = TrapFactory.createClient("http://trap.server.com/");
 * c.setDelegate(...)
 * c.open();
 * </pre>
 * <p>
 * TrapClient should have {@link #setDelegate(TrapDelegate, boolean)} called in order to properly work. The delegate
 * will handle state changes and incoming messages. It is recommended the delegate is set before open; doing it later
 * may lose events.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public interface TrapClient extends TrapEndpoint
{
    /**
     * Instructs the TrapClient to begin outgoing connection attempts. This will make exactly one attempt to connect on
     * each available transport. If any connection attempt succeeds, the entire endpoint will move to
     * {@link TrapState#OPEN}, else it will move to {@link TrapState#ERROR}
     * 
     * @throws TrapException
     *             If a fatal configuration error prevents the endpoint from opening. Generally this would be no
     *             available/configured transports.
     */
    public void open() throws TrapException;
}
