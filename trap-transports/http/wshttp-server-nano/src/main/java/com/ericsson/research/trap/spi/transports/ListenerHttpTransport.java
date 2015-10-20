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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.nhttpd.HTTPD;
import com.ericsson.research.trap.nhttpd.Method;
import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.StatusCodes;
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
import com.ericsson.research.trap.spi.nhttp.CORSUtil;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.WeakMap;

public class ListenerHttpTransport extends AbstractListenerTransport implements ListenerTrapTransport, TrapHostingTransport, RequestHandler
{
    
    private static final String           REGISTER_RESOURCE = "_connectTrap";
    static int                            listenerNum       = 1;
    static int                            num               = 0;
    
    int                                   mNum              = listenerNum++;
    
    HTTPD                             server;
    private ListenerTrapTransportDelegate serverListener;
    private Object                        context;
    boolean                               defaultHost       = true;
    boolean                               secure            = false;
    private RequestHandler            registerHandler;
    
    public ListenerHttpTransport() throws IOException
    {
        this.delegate = new TrapTransportDelegate() {
            
            @Override
            public void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
            {
            }
            
            @Override
            public void ttMessageReceived(TrapMessage message, TrapTransport transport, Object context)
            {
            }
            
            @Override
            public void ttMessageSent(TrapMessage message, TrapTransport transport, Object context)
            {
            }
            
            @Override
            public void ttMessagesFailedSending(Collection<TrapMessage> messages, TrapTransport transport, Object context)
            {
            }
            
            @Override
            public void ttNeedTransport(TrapMessage message, TrapTransport transport, Object context)
            {
            }
        };
        this.transportPriority = TrapTransportPriority.HTTP_SUN;
        this.registerHandler = new RequestHandler() {
            
            @Override
            public void handleRequest(Request request, Response response)
            {
                
                if (request.getUri().length() > REGISTER_RESOURCE.length() + 1)
                {
                    String res = request.getUri().substring(REGISTER_RESOURCE.length() + 2);
                    
                    RequestHandler handler = hostedObjects.get(res);
                    if (handler != null)
                    {
                        handler.handleRequest(request, response);
                        return;
                    }
                    else
                    {
                        response.setStatus(StatusCodes.NOT_FOUND);
                        return;
                    }
                }
                
                CORSUtil.setCors(request, response);
                
                if (request.getMethod() == Method.OPTIONS)
                {
                    response.setStatus(StatusCodes.OK);
                    return;
                }
                
                if (request.getMethod() != Method.GET)
                {
                    response.setStatus(StatusCodes.METHOD_NOT_ALLOWED);
                    return;
                }
                
                // Create a listener for the requests
                ServerHttpTransport t = new ServerHttpTransport(ListenerHttpTransport.this);
                hostedObjects.put(t.getPath().substring(1), t);
                
                serverListener.ttsIncomingConnection(t, ListenerHttpTransport.this, context);
                
                // Return the URL to the newly minted listener. The client will
                // need
                // this.
                
                response.setStatus(StatusCodes.OK);
                
                byte[] uriBytes = StringUtil.toUtfBytes(t.getPath().substring(1));
                response.setData(uriBytes).setStatus(200);
                
            }
        };
        this.hostedObjects.put(REGISTER_RESOURCE, this.registerHandler);
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
        
        Collection<RequestHandler> values = this.hostedObjects.values();
        
        for (RequestHandler obj : values)
            if (obj instanceof TrapHostable)
                ((TrapHostable) obj).notifyRemoved();
        
        ListenerHttpTransport.this.server.stop();
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
        
        if (host != null)
        {
            this.defaultHost = false;
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
            
            this.server = new HTTPD(host, port);
            
            if (sslc != null)
            {
                this.server.setSslc(sslc);
                this.secure = true;
            }
            server.setHandler(this);
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
        if (server != null)
            destination.setOption(this.prefix, "url", this.getUrl(defaultHost));
    }
    
    private String getUrl(String defaultHost)
    {
        InetSocketAddress address;
        try
        {
            address = this.server.getAddress();
        }
        catch (IOException e)
        {
            address = new InetSocketAddress(0);
        }
        
        // Check for pre-existing port
        String port = this.getOption("autoconfig.port");
        
        if (port == null)
            port = Integer.toString(address.getPort());
        
        String hostName = this.getOption("autoconfig.host");
        
        if (hostName == null)
            hostName = defaultHost;
        
        if (hostName == null)
            hostName = this.getHostName(address.getAddress(), this.defaultHost, true);
        
        return "http" + (this.secure ? "s" : "") + "://" + hostName + ":" + port + "/" + REGISTER_RESOURCE;
    }
    
    @Override
    public void handleRequest(Request request, Response response)
    {
        try
        {
            this.logger.trace("Incoming request on root: {} {}", request.getMethod(), request.getUri());
            String path = request.getUri();
            String[] parts = path.split("/");
            String base = parts.length >= 2 ? parts[1] : "";
            
            CORSUtil.setCors(request, response);
            
            RequestHandler handler = this.hostedObjects.get(base);
            
            if (handler != null)
            {
                handler.handleRequest(request, response);
                return;
            }
            
            response.setStatus(StatusCodes.NOT_FOUND);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            response.setStatus(StatusCodes.INTERNAL_SERVER_ERROR).setData(e.getMessage());
        }
    }
    
    public void unregister(ServerHttpTransport serverHttpTransport)
    {
        hostedObjects.remove(serverHttpTransport.getPath().substring(1));
    }
    
    WeakMap<String, RequestHandler> hostedObjects = new WeakMap<String, RequestHandler>();
    
    @Override
    public URI addHostedObject(final TrapHostable hosted, String preferredPath)
    {
        if (preferredPath == null || this.hostedObjects.containsKey(preferredPath))
            preferredPath = UID.randomUID();
        
        RequestHandler handler = null;
        
        if (hosted instanceof RequestHandler)
        {
            handler = (RequestHandler) hosted;
        }
        else
        {
            handler = new RequestHandler() {
                
                @Override
                public void handleRequest(Request request, Response response)
                {
                    response.addHeader("Content-Type", hosted.getContentType());
                    
                    byte[] bs = hosted.getBytes();
                    response.setStatus(StatusCodes.OK);
                    response.setData(new ByteArrayInputStream(bs));
                    
                    return;
                }
                
            };
        }
        
        this.hostedObjects.put(preferredPath, handler);
        URI uri = URI.create(this.getUrl(null) + "/" + preferredPath);
        hosted.setURI(uri);
        return uri;
    }
    
    @Override
    public void flushTransport()
    {
        
    }
}
