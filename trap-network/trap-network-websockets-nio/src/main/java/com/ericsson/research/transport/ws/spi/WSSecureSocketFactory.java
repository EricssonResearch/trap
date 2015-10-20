package com.ericsson.research.transport.ws.spi;

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

import java.io.IOException;

import javax.net.ssl.SSLContext;

import com.ericsson.research.transport.ws.WSSecurityContext;
import com.ericsson.research.trap.nio.Nio;
import com.ericsson.research.trap.nio.ServerSocket;
import com.ericsson.research.trap.nio.ServerSocket.ServerSocketHandler;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;

public class WSSecureSocketFactory
{
    
    private static SSLContext getSecureSocketHelper(WSSecurityContext sc) throws IOException
    {
        try
        {
            SSLContext sslContext = sc.getSSLContext();
            
            if (sslContext == null)
                sslContext = SSLUtil.getContext(new SSLMaterial(sc.getKeyStoreType(), sc.getKeyStoreFilename(), sc.getKeyStorePassphrase()), new SSLMaterial(sc.getTrustStoreType(), sc.getTrustStoreFilename(), sc.getTrustStorePassphrase()));
            return sslContext;
        }
        catch (Exception e)
        {
            Throwable t = e;
            while (t.getCause() != null)
                t = t.getCause();
            throw new IOException("Could not instantiate secure web socket: " + t.getMessage());
        }
    }
    
    public static Socket getSecureSocket(WSSecurityContext sc) throws IOException
    {
        return Nio.factory().sslClient(getSecureSocketHelper(sc));
    }
    
    public static ServerSocket getSecureServerSocket(WSSecurityContext sc, ServerSocketHandler handler) throws IOException
    {
    	return Nio.factory().sslServer(getSecureSocketHelper(sc), handler);
    }
    
}
