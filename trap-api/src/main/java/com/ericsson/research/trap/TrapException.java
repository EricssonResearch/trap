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
 * Exception type thrown by Trap. Each exception will have a descriptive message that explains the nature of the error.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public class TrapException extends Exception
{
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new exception with the supplied message.
     * 
     * @param message
     *            A description of the exception. Localization is not supported.
     */
    public TrapException(String message)
    {
        super(message);
    }
    
    /**
     * Constructs a new exception with the supplied cause.
     * 
     * @param e
     *            The cause of this exception.
     */
    public TrapException(Exception e)
    {
        super(e);
    }
    
    /**
     * Constructs a new exception with a message and cause.
     * 
     * @param string
     *            A message describing why this exception occurred.
     * @param e
     *            A cause of the exception.
     */
    public TrapException(String string, Exception e)
    {
        super(string, e);
    }
    
}
