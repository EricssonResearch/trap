package com.ericsson.research.trap.utils;

/*
 * ##_BEGIN_LICENSE_## Transport Abstraction Package (trap) ---------- Copyright (C) 2014 Ericsson AB ---------- Redistribution
 * and use in source and binary forms, with or without modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. ##_END_LICENSE_##
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLUtil
{
    
    public static class SSLMaterial
    {
        
        private String keyMaterialType;
        private String keyMaterialFilename;
        private String keyMaterialPassphrase;
        
        public static final String PKCS12_TYPE = "pkcs12";
        public static final String JKS_TYPE    = "jks";
        
        public SSLMaterial()
        {
        }
        
        public SSLMaterial(String keyMaterialType, String keyMaterialFilename, String keyMaterialPassphrase)
        {
            this.setKeyMaterialType(keyMaterialType);
            this.setKeyMaterialFilename(keyMaterialFilename);
            this.setKeyMaterialPassphrase(keyMaterialPassphrase);
        }
        
        public String getKeyMaterialType()
        {
            return this.keyMaterialType;
        }
        
        public void setKeyMaterialType(String keyMaterialType)
        {
            this.keyMaterialType = keyMaterialType != null ? keyMaterialType : SSLMaterial.JKS_TYPE;
        }
        
        public InputStream getKeyMaterialInputStream()
        {
            InputStream rv = null;
            if (this.keyMaterialFilename != null)
            {
                try
                {
                    rv = new FileInputStream(this.keyMaterialFilename);
                }
                catch (FileNotFoundException e)
                {
                }
            }
            
            if (rv == null)
                rv = this.getClass().getClassLoader().getResourceAsStream(this.keyMaterialFilename);
                
            return rv;
        }
        
        public void setKeyMaterialFilename(String keyMaterialFilename)
        {
            this.keyMaterialFilename = keyMaterialFilename;
        }
        
        public char[] getKeyMaterialPassphrase()
        {
            return this.keyMaterialPassphrase.toCharArray();
        }
        
        public void setKeyMaterialPassphrase(String keyMaterialPassphrase)
        {
            this.keyMaterialPassphrase = keyMaterialPassphrase;
        }
        
        public int hashCode()
        {
            return this.keyMaterialType.hashCode() + this.keyMaterialFilename.hashCode() + this.keyMaterialPassphrase.hashCode();
        }
        
    }
    
    private final static Map<SSLMaterial, KeyManagerFactory>   keyManagerFactories   = new Hashtable<SSLMaterial, KeyManagerFactory>();
    private final static Map<SSLMaterial, TrustManagerFactory> trustManagerFactories = new Hashtable<SSLMaterial, TrustManagerFactory>();
    
    public static KeyManagerFactory getKeyManagerFactory(SSLMaterial keyStore) throws NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyStoreException
    {
        KeyManagerFactory kmf ;
        KeyStore ksKeys = KeyStore.getInstance(keyStore.getKeyMaterialType());
        ksKeys.load(keyStore.getKeyMaterialInputStream(), keyStore.getKeyMaterialPassphrase());
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ksKeys, keyStore.getKeyMaterialPassphrase());
        return kmf;
    }
    
    public static TrustManagerFactory getTrustManagerFactory(SSLMaterial trustStore) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        if (trustStore != null)
        {
            KeyStore ksTrust = KeyStore.getInstance(trustStore.getKeyMaterialType());
            ksTrust.load(trustStore.getKeyMaterialInputStream(), trustStore.getKeyMaterialPassphrase());
            tmf.init(ksTrust);
        }
        else
            tmf.init((KeyStore) null);
        return tmf;
        
    }
    
    public static SSLContext getContext(SSLMaterial keyStore, SSLMaterial trustStore)
    {
        KeyManagerFactory kmf = keyManagerFactories.get(keyStore);
        TrustManagerFactory tmf = trustManagerFactories.get(trustStore);
        SSLContext sslc;
        try
        {
            if (kmf == null)
            {
                kmf = getKeyManagerFactory(keyStore);
                keyManagerFactories.put(keyStore, kmf);
            }
            if (tmf == null)
            {
                tmf = getTrustManagerFactory(trustStore);
                trustManagerFactories.put(trustStore, tmf);
            }
            sslc = SSLContext.getInstance("TLS");
            sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
        return sslc;
    }
    
    public static void purgeKeyStore(SSLMaterial keyStore)
    {
        keyManagerFactories.remove(keyStore);
    }
    
    public static void purgeTrustStore(SSLMaterial trustStore)
    {
        trustManagerFactories.remove(trustStore);
    }
    
    public static SSLContext getInsecure() throws NoSuchAlgorithmException, KeyManagementException
    {
        SSLContext sslc = SSLContext.getInstance("TLS");
        sslc.init(null, new TrustManager[] { new X509TrustManager() {
            
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
            {
            
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
            {
            
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[] {};
            }
            
        } }, null);
        return sslc;
    }
}
