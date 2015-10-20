package com.ericsson.research.trap.utils;

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
 * Generator for unique identifiers. Unlike UUID, this provides short identifiers that are unique to the session
 * (ClassLoader to be specific). This provides more compact strings. Note that UIDs provide much less entropy than
 * UUIDs, and should not be used as such.
 * <p>
 * Generated UIDs look like the example below.
 * 
 * <pre>
 * 65d5b2dd70
 * 1bfc5c0662
 * cbc871b672
 * c2868c1be1
 * 969a1beab4
 * a6ab294171
 * 6b78ddae1b
 * bf764e6cf0
 * f1916d7da9
 * 7746ebdde5
 * </pre>
 * 
 * @author Vladimir Katardjiev
 */
public abstract class UID
{
    
    private static UID instance;
    
    static
    {
        try
        {
            String name = UID.class.getName() + "Impl";
            Class<?> c = Class.forName(name);
            instance = (UID) c.newInstance();
        }
        catch (Throwable t)
        {
            System.err.println("Could not initialise UIDImpl");
        }
    }
    
    UID()
    {
    }
    
    public static String randomUID()
    {
        return instance.doRandomUID();
    }
    
    abstract String doRandomUID();
    
}
