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
 * Contstants representing the default values for transport priorities. These priorities are the defaults assigned to
 * the transports, which determines the order they are used. Can also be overridden to provide specific backing
 * implementations. Caution should be exercised when changing these values, as opposed to using addTransport() on an
 * individual basis.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public class TrapTransportPriority
{
    /**
     * Base priority for HTTP. Set this for an HTTP-level transport that should override the built-in ones.
     */
    public static final int HTTP            = 1100;
    
    /**
     * Servlet HTTP-based transport. Preferred over the Sun implementation.
     */
    public static final int HTTP_SERVLET    = HTTP + 10;
    
    /**
     * Sun HTTPServer-backed transport. JVM-provided, so no additional overhead, but not very performant. Not always
     * available.
     */
    public static final int HTTP_SUN        = HTTP_SERVLET + 10;
    
    public static final int WEBSOCKET       = 0;
    public static final int WEBSOCKET_ERNIO = WEBSOCKET + 10;
    public static final int WEBSOCKET_NETTY = WEBSOCKET_ERNIO + 10;
    
    public static final int SOCKET          = -100;
    public static final int SOCKET_ERNIO    = SOCKET + 10;
    
    public static final int LOOPBACK        = -1000;
    
    public static final int ASYNCLOOPBACK   = -1100;
    
}
