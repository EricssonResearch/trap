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



import com.ericsson.research.trap.TrapException;

/**
 * An interface for a Trap Transport capable of listening for incoming connections.
 * 
 * @author Vladimir Katardjiev
 */
public interface ListenerTrapTransport extends TrapTransport
{
    
    /**
     * Signals to the transport that it should listen for incoming connections. The specifics of how it should listen
     * (e.g. protocol, port, ....) are communicated in the transport configuration.
     * 
     * @param listener
     *            The object that will listen to incoming connections
     * @param context
     *            An optional object that will be passed to the listener on each callback
     * @throws TrapException
     *             If an error occurred during the initialization stage of the listeners.
     */
    public abstract void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException;
    
    /**
     * Asks this transport to generate the configuration string needed for a client to successfully connect to it. The
     * configuration is generated and automatically inserted into the TrapConfiguration object. 
     * 
     * @param destination
     *            The instance where the new configuration should be appended.
     * @param hostname
     *            The hostname to use for this host. This hostname will be used for any transport that has not been
     *            explicitly configured.
     * @throws RuntimeException
     *             (Actual AutomaticConfigurationDisabledException) when the IP is unreachable and
     *             hostname==null.
     * @since 1.2.0
     */
    public abstract void getClientConfiguration(TrapConfiguration destination, String hostname);
}
