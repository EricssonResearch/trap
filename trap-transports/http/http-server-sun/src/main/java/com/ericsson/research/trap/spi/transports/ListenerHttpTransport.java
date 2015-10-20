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
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLContext;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapHostingTransport;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.WeakMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

@SuppressWarnings("restriction")
public class ListenerHttpTransport extends AbstractListenerTransport implements HttpHandler, ListenerTrapTransport, TrapHostingTransport
{
    
    static int                            listenerNum = 1;
    static int                            num         = 0;
    
    int                                   mNum        = listenerNum++;
    
    HttpServer                            server;
    ExecutorService                       exec        = Executors.newFixedThreadPool(25, new ThreadFactory() {
                                                          
                                                          @Override
                                                          public Thread newThread(Runnable arg0)
                                                          {
                                                              Thread t = new Thread(arg0);
                                                              t.setName("http-" + ListenerHttpTransport.this.mNum + "-thread-" + num++);
                                                              return t;
                                                          }
                                                      });
    private ListenerTrapTransportDelegate serverListener;
    private Object                        context;
    boolean                               defaultHost = true;
    boolean                               secure      = false;
    
    public ListenerHttpTransport() throws IOException
    {
        this.delegate = new TrapTransportDelegate() {
            
            @Override
            public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
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
        this.transportPriority = TrapTransportPriority.HTTP_SUN;
    }
    
    @Override
    public void handle(HttpExchange http) throws IOException
    {
        try
        {
            this.logger.trace("Got new HTTP exchange request on root...");
            URI requestURI = http.getRequestURI();
            String path = requestURI.getPath();
            
            Headers requestHeaders = http.getRequestHeaders();
            Headers responseHeaders = http.getResponseHeaders();
            
            TrapHostable hostable = this.hostedObjects.get(path.substring(1));
            
            if (hostable != null)
            {
                
                CORSUtil.setCors(requestHeaders, responseHeaders);
                responseHeaders.add("Content-Type", hostable.getContentType());
                
                try
                {
                    
                    byte[] bs = hostable.getBytes();
                    http.sendResponseHeaders(200, bs.length);
                    http.getResponseBody().write(bs);
                    http.getResponseBody().close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    http.sendResponseHeaders(500, 0);
                }
                http.close();
                
                return;
            }
            
            if (path.length() > 2)
            {
                // We'll need to set CORS headers to prevent the error message.
                CORSUtil.setCors(requestHeaders, responseHeaders);
                http.sendResponseHeaders(404, 0);
                http.close();
                return;
            }
            
            // Create a listener for the requests
            ServerHttpTransport t = new ServerHttpTransport(this);
            t.setHttpContext(this.server.createContext(t.getPath(), t));
            
            this.serverListener.ttsIncomingConnection(t, this, this.context);
            
            CORSUtil.setCors(requestHeaders, responseHeaders);
            
            // Return the URL to the newly minted listener. The client will need this.
            http.sendResponseHeaders(200, 0);
            
            byte[] uriBytes = StringUtil.toUtfBytes(t.getPath().substring(1));
            http.getResponseBody().write(uriBytes);
            http.getResponseBody().close();
            http.close();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public String getTransportName()
    {
        return "http";
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.HTTP;
    }
    
    @Override
    public void fillAuthenticationKeys(@SuppressWarnings("rawtypes") HashSet keys)
    {
        
    }
    
    @Override
    public void fillContext(Map<String, Object> context, Collection<String> filter)
    {
        // N/A
    }
    
    @Override
    protected void internalDisconnect()
    {
        
        Collection<TrapHostable> values = this.hostedObjects.values();
        
        for (TrapHostable obj : values)
            obj.notifyRemoved();
        
        ListenerHttpTransport.this.server.stop(1);
        ListenerHttpTransport.this.exec.shutdownNow();
        ListenerHttpTransport.this.exec = null;
        ListenerHttpTransport.this.server = null;
    }
    
    @Override
    protected boolean isServerConfigured()
    {
        // The only way to really find out is to bind. Oh well...
        return true;
    }
    
    @Override
    public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
    {
        this.serverListener = listener;
        this.context = context;
        
        String host = this.getOption("host");
        int port = 0;
        
        try
        {
            port = Integer.parseInt(this.getOption("port"));
        }
        catch (Exception e)
        {
        }
        
        InetSocketAddress is = null;
        
        if (host != null)
        {
            this.defaultHost = false;
            is = new InetSocketAddress(host, port);
        }
        else
        {
            is = new InetSocketAddress(port);
        }
        try
        {
            
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
                    
                    sslc = SSLUtil.getContext(new SSLMaterial(keyType, keyName, keyPass), new SSLMaterial(trustType, trustName, trustPass));
                    
                    this.logger.info("Using provided SSL context. Keystore [{}], Truststore [{}]", keyName, trustName);
                    
                }
                catch (Exception e)
                {
                }
            }
            
            if (sslc == null)
            {
                this.server = HttpServer.create();
            }
            else
            {
                this.server = HttpsServer.create();
                ((HttpsServer) this.server).setHttpsConfigurator(new HttpsConfigurator(sslc));
                this.secure = true;
            }
            
            this.server.createContext("/", this);
            this.server.setExecutor(this.exec);
            this.server.bind(is, 0);
            this.server.start();
            this.setState(TrapTransportState.CONNECTED);
        }
        catch (IOException e)
        {
            throw new TrapException(e);
        }
        
    }
    
    @Override
    public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
    {
        destination.setOption(this.prefix, "url", this.getUrl(defaultHost));
    }
    
    private String getUrl(String defaultHost)
    {
        InetSocketAddress address = this.server.getAddress();
        
        // Check for pre-existing port
        String port = this.getOption("autoconfig.port");
        
        if (port == null)
            port = Integer.toString(address.getPort());
        
        String hostName = this.getOption("autoconfig.host");
        
        if (hostName == null)
            hostName = defaultHost;
        
        if (hostName == null)
            hostName = this.getHostName(address.getAddress(), this.defaultHost, true);
        
        return "http" + (this.secure ? "s" : "") + "://" + hostName + ":" + port + "/";
    }
    
    public void unregister(ServerHttpTransport serverHttpTransport, HttpContext httpContext)
    {
        try
        {
            this.server.removeContext(httpContext);
        }
        catch (Exception e)
        {
            try
            {
                this.server.removeContext(serverHttpTransport.getPath());
            }
            catch (Exception e1)
            {
                
            }
        }
    }
    
    WeakMap<String, TrapHostable> hostedObjects = new WeakMap<String, TrapHostingTransport.TrapHostable>();
    
    @Override
    public URI addHostedObject(TrapHostable hosted, String preferredPath)
    {
        if (preferredPath == null || this.hostedObjects.containsKey(preferredPath))
            preferredPath = UID.randomUID();
        preferredPath = "static/" + preferredPath;
        this.hostedObjects.put(preferredPath, hosted);
        URI uri = URI.create(this.getUrl("localhost") + preferredPath);
        hosted.setURI(uri);
        return uri;
    }
    
    @Override
    public void flushTransport()
    {
        
    }
}
