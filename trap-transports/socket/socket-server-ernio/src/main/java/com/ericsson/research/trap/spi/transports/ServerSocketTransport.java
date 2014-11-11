package com.ericsson.research.trap.spi.transports;

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;

import javax.net.ssl.SSLContext;

import com.ericsson.research.transport.ManagedServerSocket;
import com.ericsson.research.transport.ManagedServerSocketClient;
import com.ericsson.research.transport.ManagedSocket;
import com.ericsson.research.transport.ssl.SSLServerSocket;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;

public class ServerSocketTransport extends AbstractListenerTransport implements ListenerTrapTransport, ManagedServerSocketClient
{
    
    private ManagedServerSocket           serverSocket;
    private ListenerTrapTransportDelegate listenerDelegate;
    private Object                        listenerContext;
    boolean                               defaultHost = false;
    private boolean                       secure      = false;
    
    public ServerSocketTransport()
    {
        this.transportPriority = TrapTransportPriority.SOCKET_ERNIO;
    }
    
    public String getTransportName()
    {
        return "socket";
    }
    
    public String getProtocolName()
    {
        return TrapTransportProtocol.TCP;
    }
    
    /**
     * Listen for incoming connections.
     */
    public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
    {
        // Remember the delegate and context. We need them for callbacks
        this.listenerDelegate = listener;
        this.listenerContext = context;
        this.delegate = new TrapTransportDelegate() {
            
            public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
        };
        
        // Start a socket
        ManagedServerSocket ss;
        
        SSLContext sslc = null;
        
        if (this.getOption(CERT_USE_INSECURE_TEST) != null)
        {
            sslc = SSLUtil.getContext(new SSLMaterial("jks", "trapserver.jks", "Ericsson"), new SSLMaterial("jks", "trapserver.jks", "Ericsson"));
            this.logger.warn("Using insecure SSL context");
        }
        else
        {
            try
            {
                String keyType = this.getOption(CERT_KEYSTORE_TYPE);
                String keyName = this.getOption(CERT_KEYSTORE_NAME);
                String keyPass = this.getOption(CERT_KEYSTORE_PASS);
                
                String trustType = this.getOption(CERT_TRUSTSTORE_TYPE);
                String trustName = this.getOption(CERT_TRUSTSTORE_NAME);
                String trustPass = this.getOption(CERT_TRUSTSTORE_PASS);
                
                if (keyName != null)
                {
                    sslc = SSLUtil.getContext(new SSLMaterial(keyType, keyName, keyPass), new SSLMaterial(trustType, trustName, trustPass));
                    this.logger.info("Using provided SSL context. Keystore [{}], Truststore [{}]", keyName, trustName);
                }
                
            }
            catch (Exception e)
            {
                this.logger.warn("Not using SSL due to exception {}", e);
            }
        }
        
        if (sslc == null)
        {
            ss = new ManagedServerSocket();
        }
        else
        {
            
            ss = new SSLServerSocket(sslc);
            this.secure = true;
        }
        
        // Register for callbacks on the socket (we're using async sockets)
        ss.registerClient(this);
        try
        {
            // Listen on port 0
            int port = 0;
            try
            {
                port = Integer.parseInt(this.getOption(SocketConstants.CONFIG_PORT));
            }
            catch (Throwable t)
            {
            }
            
            String host = null;
            
            try
            {
                host = this.getOption(SocketConstants.CONFIG_HOST);
            }
            catch (Throwable t)
            {
            }
            
            if ((host == null) || (host.trim().length() == 0))
            {
                this.defaultHost = true;
                host = "0.0.0.0";
            }
            
            ss.listen(InetAddress.getByName(host), port);
            
            // We'll receive a callback when the socket is bound
        }
        catch (IOException e)
        {
            throw new TrapException(e);
        }
    }
    
    /**
     * Called when the Server Socket is ready to accept incoming connections
     */
    public void notifyBound(ManagedServerSocket socket)
    {
        // Assign the socket to a field, so we can access it later.
        this.serverSocket = socket;
        this.setState(TrapTransportState.CONNECTED);
    }
    
    /**
     * Called when we want to configure a client to connect to us. See AsynchronousTransportTest for an example on when
     * it is called.
     */
    public void getClientConfiguration(TrapConfiguration destination, String defaultName)
    {
        // This function may be called before we're ready on the server socket side. This loop will prevent us
        // from accessing a non-ready server socket.
        while ((this.serverSocket == null) && (this.getState() != TrapTransportState.ERROR) && (this.getState() != TrapTransportState.DISCONNECTED))
            try
            {
                System.out.println("Waiting...");
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        
        // If we still don't have a server socket, we're not going to be ready (state == ERROR or interrupted).
        if (this.serverSocket == null)
            throw new IllegalStateException("Could not initialise Server Socket");
        
        // Insert the appropriate values into the TrapConfiguration. These are the same values we expect on the client side.
        // The exact values are not defined by Trap but are Transport-specific. Each transport can and will have a different
        // set of values they want transferred.
        InetSocketAddress inetAddress = this.serverSocket.getInetAddress();
        
        // Check for pre-existing port
        String port = this.getOption("autoconfig.port");
        
        if (port == null)
            port = Integer.toString(inetAddress.getPort());
        
        String hostName = this.getOption("autoconfig.host");
        
        if (hostName == null)
            hostName = defaultName;
        
        if (hostName == null)
            hostName = this.getHostName(inetAddress.getAddress(), this.defaultHost, true);
        
        destination.setOption(this.prefix, SocketConstants.CONFIG_HOST, hostName);
        destination.setOption(this.prefix, SocketConstants.CONFIG_PORT, port);
        destination.setOption(this.prefix, SocketConstants.CONFIG_SECURE, "" + this.secure);
        
    }
    
    /*
     * This function will be called by the async socket API to notify of an incoming connection (but no data yet). We need to create
     * a corresponding TrapTransport object and send it to the Trap Core for processing.
     *
     * (non-Javadoc)
     * @see com.ericsson.research.transport.ManagedServerSocketClient#notifyAccept(com.ericsson.research.transport.ManagedSocket)
     */
    public void notifyAccept(ManagedSocket socket)
    {
        // Create a SocketTransport that wraps around the socket
        TrapTransport transport = new SocketTransport(socket);
        
        // Notify the listener of a new connection
        this.listenerDelegate.ttsIncomingConnection(transport, this, this.listenerContext);
    }
    
    // Called when we want to close the server socket (=stop listening).
    @Override
    protected void internalDisconnect()
    {
        this.serverSocket.close();
    }
    
    // Called by the async socket interface when a failure occurs. We'll mostly just propagate it by setState.
    public void notifyError(Exception e)
    {
        this.setState(TrapTransportState.ERROR);
    }
    
    @Override
    public void flushTransport()
    {
        
    }
    
}
