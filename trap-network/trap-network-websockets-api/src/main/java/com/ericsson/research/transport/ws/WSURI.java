package com.ericsson.research.transport.ws;

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


public class WSURI
{
    
    private static final String ERROR = "Wrong WebSocket URI: ";
    
    private final String        host;
    private final int           port;
    private final String        scheme;
    private final String        path;
    
    public WSURI(String uri) throws IllegalArgumentException
    {
        int p = uri.indexOf("://");
        if (p == -1)
            throw new IllegalArgumentException(ERROR + uri);
        this.scheme = uri.substring(0, p);
        if (!"ws".equals(this.scheme) && !"wss".equals(this.scheme))
            throw new IllegalArgumentException(ERROR + uri);
        int q = uri.indexOf("/", p + 3);
        if (q == -1)
            q = uri.length();
        String hostandport = uri.substring(p + 3, q);
        int r = hostandport.lastIndexOf(":");
        if (r != -1)
        {
            this.host = hostandport.substring(0, r);
            try
            {
                this.port = Integer.parseInt(hostandport.substring(r + 1));
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException(ERROR + uri);
            }
        }
        else
        {
            this.host = hostandport;
            if ("ws".equals(this.scheme))
                this.port = 80;
            else
                this.port = 443;
        }
        if (q < uri.length())
            this.path = uri.substring(q);
        else
            this.path = "/";
    }
    
    public String getHost()
    {
        return this.host;
    }
    
    public int getPort()
    {
        return this.port;
    }
    
    public String getScheme()
    {
        return this.scheme;
    }
    
    public String getPath()
    {
        return this.path;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.scheme);
        sb.append("://");
        sb.append(this.host);
        if (!(this.port == 80 && "ws".equals(this.scheme)) && !(this.port == 443 && "wss".equals(this.scheme)))
        {
            sb.append(":");
            sb.append(Integer.toString(this.port));
        }
        sb.append(this.path);
        return sb.toString();
    }
    
}
