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

import javax.net.ssl.SSLContext;

public class WSSecurityContext
{
    
    private String     keyStoreType;
    private String     keyStoreFilename;
    private String     keyStorePassphrase;
    private String     trustStoreType;
    private String     trustStoreFilename;
    private String     trustStorePassphrase;
    private SSLContext sslContext;
    
    public WSSecurityContext(String keyStoreType, String keyStoreFilename, String keyStorePassphrase, String trustStoreType, String trustStoreFilename, String trustStorePassphrase)
    {
        this.setKeyStoreType(keyStoreType);
        this.setKeyStoreFilename(keyStoreFilename);
        this.setKeyStorePassphrase(keyStorePassphrase);
        this.setTrustStoreType(trustStoreType);
        this.setTrustStoreFilename(trustStoreFilename);
        this.setTrustStorePassphrase(trustStorePassphrase);
    }
    
    public WSSecurityContext(SSLContext sslc)
    {
        this.sslContext = sslc;
    }
    
    public String getKeyStoreType()
    {
        return this.keyStoreType;
    }
    
    public void setKeyStoreType(String keyStoreType)
    {
        this.keyStoreType = keyStoreType;
    }
    
    public String getKeyStoreFilename()
    {
        return this.keyStoreFilename;
    }
    
    public void setKeyStoreFilename(String keyStoreFilename)
    {
        this.keyStoreFilename = keyStoreFilename;
    }
    
    public String getKeyStorePassphrase()
    {
        return this.keyStorePassphrase;
    }
    
    public void setKeyStorePassphrase(String keyStorePassphrase)
    {
        this.keyStorePassphrase = keyStorePassphrase;
    }
    
    public String getTrustStoreType()
    {
        return this.trustStoreType;
    }
    
    public void setTrustStoreType(String trustStoreType)
    {
        this.trustStoreType = trustStoreType;
    }
    
    public String getTrustStoreFilename()
    {
        return this.trustStoreFilename;
    }
    
    public void setTrustStoreFilename(String trustStoreFilename)
    {
        this.trustStoreFilename = trustStoreFilename;
    }
    
    public String getTrustStorePassphrase()
    {
        return this.trustStorePassphrase;
    }
    
    public void setTrustStorePassphrase(String trustStorePassphrase)
    {
        this.trustStorePassphrase = trustStorePassphrase;
    }
    
    public SSLContext getSSLContext()
    {
        return this.sslContext;
    }
    
    public void setSSLContext(SSLContext sslContext)
    {
        this.sslContext = sslContext;
    }
    
}
