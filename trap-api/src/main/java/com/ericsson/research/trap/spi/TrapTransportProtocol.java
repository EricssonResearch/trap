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
 * Defines a non-exhaustive list of protocols that transports can employ. These constants represent all of the default
 * transports available for Trap, and may not cover custom transports.
 * <p>
 * The exact protocol used by a transport is generally not too interesting, except for authentication providers, who may
 * need to know the protocol details in order to authenticate it properly.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public class TrapTransportProtocol
{
    private TrapTransportProtocol()
    {
    }
    
    /**
     * Raw TCP socket, with no other protocol on top. (Save for Trap, of course!)
     */
    public static final String TCP              = "tcp";
    
    /**
     * Raw UDP socket.
     */
    public static final String UDP              = "udp";
    
    /**
     * The WebSocket protocol is used to transport the messages. WebSocket provides framed messaging and browser access,
     * and is the preferred transport from within a browser.
     */
    public static final String WEBSOCKET        = "ws";
    
    /**
     * Denotes the secure websocket protocol, i.e. WebSocket over TLS
     */
    public static final String WEBSOCKET_SECURE = "wss";
    
    /**
     * HTTP is used to send messages. This category makes no mention of how (long polling, POST, etc) or how many
     * messages per HTTP entity body. It does, however, note that there are HTTP headers available in the
     * authentication's context.
     */
    public static final String HTTP             = "http";
    
    /**
     * Denotes the HTTP transport, used over TLS/SSL.
     */
    public static final String HTTPS            = "https";
    
    /**
     * Function call-based transport. Capable of transferring TrapObjects. This type of transport is already capable of
     * calling functions within the JVM so rarely needs additional protection.
     */
    public static final String FUNCTION         = "function";
}
