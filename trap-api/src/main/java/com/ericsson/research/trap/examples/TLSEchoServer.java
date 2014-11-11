package com.ericsson.research.trap.examples;

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



import java.util.HashSet;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * This example provides a simple echo server. Any message sent will be echoed back to the client (and the console on
 * the server). Messages are assumed strings.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/TLSEchoServer.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1.4
 */
//BEGIN_INCLUDE(sample)
public class TLSEchoServer implements OnAccept, OnData, OnClose
{
    
    /*
     * This class implements three interfaces:
     * 
     * OnAccept - This will be called by TrapListener endpoints to notify that a new connection is established
     * OnData - TrapEndpoint objects will use this to notify our server that they have received data.
     * OnClose - TrapEndpoint objects will use this to tell us they closed.
     * 
     * The delegate interfaces are optional (though some are recommended, such as OnData). Implement only those
     * that generate events the delegate is interested in.
     */
    
    static TrapListener          echoServer;
    final static HashSet<TrapEndpoint> clients      = new HashSet<TrapEndpoint>();
    static String                clientConfig = null;
    
    public static void main(String[] args) throws Throwable
    {
        // Create a new listener
        echoServer = TrapFactory.createListener();
        
        // Configure the server to use a specific cerver certificate.
        // The file MUST be a pkcs12 or jks file containing exactly one certificate with its corresponding
        // private key. The certificate SHOULD be properly signed, but MAY be self-signed.
        // If self-signed, the TrustStore must be configured too (see below)
        
        // The type of the keystore. jks or pkcs12 are supported
        echoServer.setOption(TrapTransport.CERT_KEYSTORE_TYPE, "jks");
        
        // The path to the keystore file. This may be a filesystem path (absolute, or relative to cwd) or a name of a file to be found in the classpath
        echoServer.setOption(TrapTransport.CERT_KEYSTORE_NAME, "trapserver.jks");
        
        // Password to decrypt the keystore file.
        echoServer.setOption(TrapTransport.CERT_KEYSTORE_PASS, "Ericsson");
        
        // If using a self-signed certificate, or using a custom root CA, the trust store needs to be extended to tell the TLS context
        // that the certificate is valid. To do that, a TrustStore is needed. A TrustStore is configured in the same way as a keystore,
        // Except it ONLY supports jks. Use keytool to create one. The TrustStore must contain all certificates of Root CAs that the
        // server should recognise as valid.
        echoServer.setOption(TrapTransport.CERT_TRUSTSTORE_TYPE, "jks");
        echoServer.setOption(TrapTransport.CERT_TRUSTSTORE_NAME, "trapserver.jks");
        echoServer.setOption(TrapTransport.CERT_TRUSTSTORE_PASS, "Ericsson");
        
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        echoServer.listen(new TLSEchoServer());
        
        // Now to tell clients how to connect. Since we elected no configuration, Trap will allocate random ports.
        clientConfig = echoServer.getClientConfiguration();
        System.out.println("New clients should connect to the following configuration:");
        System.out.println(echoServer.getClientConfiguration());
    }
    
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Endpoints received by this method are NOT strongly referenced, and may be GC'd. Thus, we must retain
        clients.add(endpoint);
        
        // We also want feedback from the endpoint.
        endpoint.setDelegate(this, true);
    }
    
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        // Log!
        System.out.println("Echo Server Got message: [" + StringUtil.toUtfString(data) + "] of length " + data.length);
        
        try
        {
            // Echo the data back with the same parameters.
            endpoint.send(data, channel, false);
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
    }
    
    public void trapClose(TrapEndpoint endpoint, Object context)
    {
        // Remove the strong reference to allow garbage collection
        clients.remove(endpoint);
    }
    
}
//END_INCLUDE(sample)
