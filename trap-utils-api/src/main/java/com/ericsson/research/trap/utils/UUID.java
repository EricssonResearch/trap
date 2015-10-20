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
 * Wrapper class for UUID generation. Allows UUIDs to be generated before Java introduced UUIDs. UUIDs generated contain
 * a high degree of entropy and are generally considered unique enough to be usable between different machines.
 * <p>
 * The following is a sample of UUID output
 * 
 * <pre>
 * 35713e9d-0000-4918-b8b0-d997c3990907
 * 297f62d8-cc67-43bb-9b95-4f0c7e771eb4
 * 638fda98-df5e-466e-81c0-ac764a65e0bc
 * c4eca222-66dc-42f6-82eb-bdee1d8b17c3
 * fe7ae1ed-79bb-4940-9d09-7629c4fe6970
 * ef3d7cc2-991e-40be-ab72-dd9481841485
 * efa882f6-b87e-48d7-8b40-554f84739a06
 * 20c4eef3-3691-4d76-b8fc-77a91dfe7137
 * 42b2b123-5c88-475a-b0f2-ac7ab0fb4c9a
 * 9edbead9-72ca-411f-87cf-4396306a3634
 * </pre>
 * 
 * @author Vladimir Katardjiev
 */
public abstract class UUID
{
    private static UUID instance;
    
    static
    {
        try
        {
            String name = UUID.class.getName() + "Impl";
            Class<?> c = Class.forName(name);
            instance = (UUID) c.newInstance();
        }
        catch (Throwable t)
        {
            System.err.println("Could not initialise UUIDImpl");
        }
    }
    
    /**
     * Creates a new random UUID and returns the value.
     * 
     * @return A new UUID.
     */
    public static String randomUUID()
    {
        return instance.doRandomUUID();
    }
    
    abstract String doRandomUUID();
    
    UUID()
    {
    }
}
