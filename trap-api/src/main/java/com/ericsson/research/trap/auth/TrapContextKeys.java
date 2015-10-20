package com.ericsson.research.trap.auth;

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



import com.ericsson.research.trap.spi.TrapTransportProtocol;

/**
 * Enum for the pre-defined keys that can be used for authentication context map requests. Not all keys are always
 * available for every transport, and authentication providers must take that into account.
 * 
 * @author Vladimir Katardjiev
 */
public class TrapContextKeys
{
    /**
     * Boolean value, whether the last message had any authentication attached to it.
     */
    public static final String LastMessageWasAuthenticated     = "LastMessageWasAuthenticated";
    
    /**
     * Boolean value, whether the last incoming message succeeded with its verifyAuthentication stage.
     */
    public static final String LastMessageAuthenticationFailed = "LastMessageAuthenticationFailed";
    
    /**
     * The remote IP of the other endpoint/transport.
     */
    public static final String RemoteIP                        = "RemoteIP";
    
    /**
     * The local IP of the transport.
     */
    public static final String LocalIP                         = "LocalIP";
    
    /**
     * The remote port of the transport.
     */
    public static final String RemotePort                      = "RemotePort";
    
    /**
     * The local port of the transport.
     */
    public static final String LocalPort                       = "LocalPort";
    
    /**
     * Requests the enclosing protocol used to transport the message is included in the context. The enclosing protocol
     * can be (non-exhaustive list) items such as <code>tcp</code>, <code>http</code>, <code>ws</code>,
     * <code>function</code>, etc.
     * 
     * @see TrapTransportProtocol
     */
    public static final String Protocol                        = "Protocol";
    
    /**
     * Requests that the transport itself be included in the context map, using the Transport key.
     */
    public static final String Transport                       = "Transport";
    
    /**
     * The transport's configuration, as represented by a string, will be added to the context.
     */
    public static final String Configuration                   = "Configuration";
    
    /**
     * Represents the transport's current state at the time of the context setup.
     */
    public static final String State                           = "State";
    
    /**
     * LastAlive is a representation of the last (successful) message sent or received by the transport, as a Java
     * timestamp.
     */
    public static final String LastAlive                       = "LastAlive";
    
    /**
     * A number representing the transport priority relative to other transports.
     */
    public static final String Priority                        = "Priority";
    
    /**
     * The message format used by the transport. Can be used to detect invalid message body content.
     */
    public static final String Format                          = "Format";
}
