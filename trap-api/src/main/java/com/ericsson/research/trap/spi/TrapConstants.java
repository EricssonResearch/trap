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



import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.spi.TrapMessage.Format;

/**
 * Contains a number of constants for Trap. These constants are all 
 * 
 * @author Vladimir Katardjiev
 */
public final class TrapConstants
{
    
    /**
     * The default message format. Unless otherwise specified, Trap will attempt to use this format.
     */
    public static final Format  MESSAGE_FORMAT_DEFAULT;
    
    /**
     * The default queue type to be used by endpoints by default.
     */
    public static final String  ENDPOINT_QUEUE_DEFAULT;
    
    /**
     * Whether or not transports are automatically enabled.
     */
    public static final boolean TRANSPORT_ENABLED_DEFAULT;
    
    /**
     * A key for tokens used during a connection stage.
     */
    public static final String  CONNECTION_TOKEN;
    
    /**
     * The default chunk size, used if no other config is set.
     */
    public static final int     DEFAULT_CHUNK_SIZE;
    
    /**
     * Whether compression is enabled by default.
     */
    public static final boolean COMPRESSION_ENABLED_DEFAULT;
    
    /**
     * The endpoint ID (alternatively named trapId) of the endpoint
     */
    public static final String ENDPOINT_ID = "trap.endpoint-id";
    
    /**
     * Constant representing a TrapEndpoint that has yet to be initialised
     */
    public static final String ENDPOINT_ID_UNDEFINED = "UNDEFINED";
    
    /**
     * Constant representing a TrapEndpoint that expects to receive an ID from the server
     */
    public static final String ENDPOINT_ID_CLIENT = "NEW";
    
    static
    {
        DEFAULT_CHUNK_SIZE = 16 * 1024;
        MESSAGE_FORMAT_DEFAULT = Format.REGULAR;
        ENDPOINT_QUEUE_DEFAULT = TrapEndpoint.REGULAR_BYTE_QUEUE;
        TRANSPORT_ENABLED_DEFAULT = true;
        CONNECTION_TOKEN = "trap.connection-token";
        COMPRESSION_ENABLED_DEFAULT = true;
    }
    
    TrapConstants()
    {
    }
}
