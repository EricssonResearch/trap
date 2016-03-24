package com.ericsson.research.trap.spi.transports;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.nhttpd.CookieHandler;
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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ListenerHttpTransport extends AbstractListenerTransport implements ListenerTrapTransport, TrapHostingTransport, RequestHandler
{
    
    private static final String REGISTER_RESOURCE = "_connectTrap";
    static int                  listenerNum       = 1;
    static int                  num               = 0;
    
    int mNum = listenerNum++;
    
    Channel                               ch;
    private ListenerTrapTransportDelegate serverListener;
    private Object                        context;
    boolean                               defaultHost = true;
    boolean                               secure      = false;
    private RequestHandler                registerHandler;
    
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
            public void handleRequest(com.ericsson.research.trap.nhttpd.Request request, com.ericsson.research.trap.nhttpd.Response response)
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
    
    HTTPChannelAdapter       adapter   = new HTTPChannelAdapter();
    ServerWebSocketTransport webSocket = null;
    
    @Override
    public String getTransportName()
    {
        return TrapTransportProtocol.HTTP;
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
                
        ChannelFuture close = ch.close();
        try
        {
            close.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ch = null;
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
        
        SslContext sslc = null;
        
        try
        {
            
            if ("true".equals(this.getOption(TrapTransport.CERT_USE_INSECURE_TEST)))
            {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslc = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                this.secure = true;
            }
            
            String keyType = this.getOption(CERT_KEYSTORE_TYPE);
            String keyName = this.getOption(CERT_KEYSTORE_NAME);
            String keyPass = this.getOption(CERT_KEYSTORE_PASS);
            
            String trustType = this.getOption(CERT_TRUSTSTORE_TYPE);
            String trustName = this.getOption(CERT_TRUSTSTORE_NAME);
            String trustPass = this.getOption(CERT_TRUSTSTORE_PASS);
            
            sslc = SslContextBuilder.forServer(SSLUtil.getKeyManagerFactory(new SSLMaterial(keyType, keyName, keyPass)))
                .trustManager(SSLUtil.getTrustManagerFactory(new SSLMaterial(trustType, trustName, trustPass)))
                .build();
            this.logger.info("Using provided SSL context. Keystore [{}], Truststore [{}]", keyName, trustName);
            this.secure = true;
            
        }
        catch (Exception e)
        {
        }
        
        SslContext finalSslCtx = sslc;
        
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    
                    @Override
                    protected void initChannel(Channel ch) throws Exception
                    {
                        ChannelPipeline p = ch.pipeline();
                        if (finalSslCtx != null)
                        {
                            p.addLast(finalSslCtx.newHandler(ch.alloc()));
                        }
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(256 * 1024));
                        p.addLast(adapter);
                    }
                });
            ChannelFuture future;
            
            if (host != null)
                future = b.bind(host, port);
            else
                future = b.bind(port);
                
            ch = future.sync().channel();
            
            this.setState(TrapTransportState.CONNECTED);
        }
        catch (InterruptedException e)
        {
            throw new TrapException(e);
        }
        
    }
    
    @Override
    public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
    {
        if (ch != null)
            destination.setOption(this.prefix, "url", this.getUrl(defaultHost));
    }
    
    private String getUrl(String defaultHost)
    {
        
        InetSocketAddress address = (InetSocketAddress) this.ch.localAddress();
        
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
    
    /**
     * Converts a Netty HTTP channel into the Request/Response interface for Trap
     * 
     * @author vladi
     */
    @Sharable
    class HTTPChannelAdapter extends SimpleChannelInboundHandler<FullHttpRequest>
    {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            super.channelRead(ctx, msg);
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception
        {
            
            // In the WebSocket case, we will just punt the request over there
            if (msg.uri().startsWith("/" + ServerWebSocketTransport.REGISTER_RESOURCE))
            {
                ctx.channel().pipeline().remove(this);
                ChannelHandler handler = webSocket.createHandler(ctx, msg);
                ctx.channel().pipeline().addLast(handler);
                return;
            }
            
            // In all other cases, we map it to the NHTTPD API and proceed using the classic code base.
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(msg.uri());
            
            Request req = new Request() {
                
                HashMap<String, String> params = null;
                Map<String, String> headers = null;
                
                @Override
                public String getUri()
                {
                    return msg.uri();
                }
                
                @Override
                public String getQueryParameterString()
                {
                    String uri = queryStringDecoder.uri();
                    int offset = queryStringDecoder.path().length() + 1;
                    
                    if (uri.length() > offset)
                        return uri.substring(offset);
                    return null;
                }
                
                @Override
                public Map<String, String> getParms()
                {
                    
                    if (params == null)
                    {
                        params = new HashMap<>();
                        queryStringDecoder.parameters().forEach((key, list) -> {
                            params.put(key, list.get(0));
                        });
                    }
                    
                    return params;
                }
                
                @Override
                public Method getMethod()
                {
                    return Method.lookup(msg.method().name().toUpperCase());
                }
                
                @Override
                public InputStream getInputStream()
                {
                    ByteBuf content = msg.content();
                    return new ByteBufInputStream(content);
                }
                
                @Override
                public Map<String, String> getHeaders()
                {
                    if (headers == null)
                    {
                        headers = new HashMap<>();
                        msg.headers().forEach(e -> {
                            headers.put(e.getKey(), e.getValue());
                        });
                    }
                    return headers;
                }
                
                @Override
                public CookieHandler getCookies()
                {
                    return new CookieHandler(getHeaders());
                }
            };
            
            DefaultFullHttpResponse httpResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            AtomicBoolean async = new AtomicBoolean(false);
            httpResp.retain();
            
            Response resp = new Response() {
                
                private InputStream data;
                boolean sent = false;
                
                @Override
                public Response setStatus(int status)
                {
                    httpResp.setStatus(HttpResponseStatus.valueOf(status));
                    return this;
                }
                
                @Override
                public Response setMimeType(String mimeType)
                {
                    httpResp.headers().add(HttpHeaderNames.CONTENT_TYPE, mimeType);
                    return this;
                }
                
                @Override
                public Response setData(String data)
                {
                    httpResp.content().clear();
                    try (ByteBufOutputStream os = new ByteBufOutputStream(httpResp.content()))
                    {
                        os.write(StringUtil.toUtfBytes(data));
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                    return this;
                }
                
                @Override
                public Response setData(byte[] data)
                {
                    httpResp.content().clear();
                    try (ByteBufOutputStream os = new ByteBufOutputStream(httpResp.content()))
                    {
                        os.write(data);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
                
                @Override
                public Response setData(InputStream data)
                {
                    this.data = data;
                    return this;
                }
                
                @Override
                public Response setChunkedTransfer(boolean chunkedTransfer)
                {
                    return this;
                }
                
                @Override
                public Response setAsync(boolean newAsync)
                {
                    async.set(newAsync);
                    return this;
                }
                
                @Override
                public synchronized void sendAsyncResponse()
                {
                    if (sent)
                        return;
                    sent = true;
                    
                    if (data != null)
                    {
                        httpResp.content().clear();
                        try (ByteBufOutputStream os = new ByteBufOutputStream(httpResp.content()))
                        {
                            int read = 0;
                            byte[] buf = new byte[4096];
                            while ((read = data.read(buf)) > -1)
                                os.write(buf, 0, read);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                    int contentLength = httpResp.content().readableBytes();
                    httpResp.headers().add(HttpHeaderNames.CONTENT_LENGTH, contentLength);
                    ctx.writeAndFlush(httpResp);
                    httpResp.release();
                }
                
                @Override
                public int getStatus()
                {
                    return httpResp.status().code();
                }
                
                @Override
                public String getMimeType()
                {
                    return null;
                }
                
                @SuppressWarnings("deprecation")
                @Override
                public String getHeader(String name)
                {
                    return httpResp.headers().get(name);
                }
                
                @Override
                public boolean getAsync()
                {
                    return async.get();
                }
                
                @SuppressWarnings("deprecation")
                @Override
                public Response addHeader(String name, String value)
                {
                    httpResp.headers().add(name, value);
                    return this;
                }
            };
            
            ListenerHttpTransport.this.handleRequest(req, resp);
            
            if (!async.get())
                resp.sendAsyncResponse();
                
        }
        
    }
}
