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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLException;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.nhttp.WebSocketConstants;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class WebSocketTransport extends AbstractTransport
{
    
    private static final String            OPTION_BINARY = "binary";
    private static final NioEventLoopGroup nioGroup      = new NioEventLoopGroup();
    private boolean                        binary        = true;
    private boolean                        ssl           = false;
                                                         
    // Create a new SocketTransport for connecting (=client)
    public WebSocketTransport()
    {
        this.transportPriority = 0;
    }
    
    public boolean canConnect()
    {
        return true;
    }
    
    public String getTransportName()
    {
        return "websocket";
    }
    
    @Override
    public void init()
    {
        super.init();
        if (this.ctx != null)
            this.ctx.close();
        this.ctx = null;
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.WEBSOCKET;
    }
    
    ByteArrayOutputStream         outBuf = new ByteArrayOutputStream();
    private ChannelHandlerContext ctx;
                                  
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        
        if (this.logger.isTraceEnabled())
            this.logger.trace("Now sending message ({}) with id [{}]", message.getOp().toString(), message.getMessageId());
            
        try
        {
            if (ctx == null)
                throw new TrapTransportException(message, this.getState());
                
            byte[] raw = message.serialize();
            synchronized (ctx)
            {
                this.performSend(raw);
                
                if (!expectMore)
                    this.flushTransport();
            }
        }
        catch (IOException | TrapTransportException e)
        {
            this.logger.debug(e.toString());
            // Move to state ERROR and clean up
            this.setState(TrapTransportState.ERROR);
            if (this.ctx != null)
                this.ctx.close();
            throw new TrapTransportException(message, this.state);
        }
        
        catch (Throwable t)
        {
            this.logger.error(t.toString(), t);
            this.setState(TrapTransportState.ERROR);
            throw new TrapTransportException(message, this.state);
        }
    }
    
    private void performSend(byte[] raw) throws IOException
    {
        if (this.outBuf != null)
        {
            this.outBuf.write(raw);
            raw = this.outBuf.toByteArray();
            this.outBuf = null;
        }
        //char[] encoded = Base64.encode(raw);
        
        if (this.binary)
        {
            this.ctx.channel().write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(raw)));
        }
        else
        {
            this.ctx.channel().write(new TextWebSocketFrame(StringUtil.toUtfString(raw)));
        }
        
    }
    
    long    lastFlush      = 0;
    long    flushInterval  = 2;
    boolean flushScheduled = false;
                           
    @Override
    public void flushTransport()
    {
        synchronized (WebSocketTransport.this.ctx)
        {
            if ((lastFlush + flushInterval) > System.currentTimeMillis())
            {
                if (flushScheduled)
                    return;
                    
                ThreadPool.executeAfter(this::doScheduledFlush, flushInterval + 1);
                flushScheduled = true;
                return;
            }
            ctx.channel().flush();
            lastFlush = System.currentTimeMillis();
        }
    }
    
    void doScheduledFlush()
    {
        synchronized (WebSocketTransport.this.ctx)
        {
            flushScheduled = false;
            flushTransport();
        }
    }
    
    @Override
    protected boolean isClientConfigured()
    {
        String uriStr = this.getOption(WebSocketConstants.CONFIG_URI);
        
        if (uriStr == null)
            return false;
            
        return uriStr.startsWith("ws://") || uriStr.startsWith("wss://");
    }
    
    @Override
    protected void internalConnect() throws TrapException
    {
        synchronized (this)
        {
            URI uri = URI.create(this.getOption(WebSocketConstants.CONFIG_URI));
            SslContext sslc;
            try
            {
                if ("wss".equalsIgnoreCase(uri.getScheme()))
                {
                    sslc = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                }
                else
                {
                    sslc = null;
                }
                
                WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                    .newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
                    
                final MyChannelHandler webSocketHandler = new MyChannelHandler((ctx, frame) -> {
                    handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
                });
                
                final SimpleChannelInboundHandler<FullHttpResponse> httpHandler = new SimpleChannelInboundHandler<FullHttpResponse>() {
                    
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception
                    {
                        handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                        notifyOpen();
                    }
                    
                    @Override
                    public void channelActive(ChannelHandlerContext ctx)
                    {
                        handshaker.handshake(ctx.channel());
                    }
                    
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx)
                    {
                        notifyClose();
                    }
                };
                
                Bootstrap b = new Bootstrap();
                b.group(nioGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)
                    {
                        ChannelPipeline p = ch.pipeline();
                        if (sslc != null)
                        {
                            p.addLast(sslc.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                        }
                        p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), webSocketHandler, httpHandler);
                    }
                });
                
                Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
                ctx = ch.pipeline().lastContext();
            }
            catch (SSLException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected void internalDisconnect()
    {
        if (this.ctx == null)
            return;
            
        synchronized (this)
        {
            if ((this.getState() != TrapTransportState.DISCONNECTED) && (this.getState() != TrapTransportState.DISCONNECTED)
                && (this.getState() != TrapTransportState.ERROR))
                this.setState(TrapTransportState.DISCONNECTING);
        }
        try
        {
            synchronized (this.ctx)
            {
                this.ctx.close();
                this.ctx = null;
            }
            
        }
        catch (Exception e)
        {
            synchronized (this)
            {
                // TODO: Gracefully do something
                if (this.getState() != TrapTransportState.DISCONNECTED)
                    this.setState(TrapTransportState.ERROR);
            }
        }
        finally
        {
        }
    }
    
    public void notifyError()
    {
        this.setState(TrapTransportState.ERROR);
        
        if (this.ctx != null)
            this.ctx.close();
        this.ctx = null;
    }
    
    public void notifyOpen()
    {
        this.fillContext(this.contextMap, this.contextKeys);
        this.setState(TrapTransportState.CONNECTED);
    }
    
    public synchronized void notifyClose()
    {
        if (this.getState() != TrapTransportState.ERROR)
            this.setState(TrapTransportState.DISCONNECTED);
            
        if (this.ctx != null)
            this.ctx.close();
        this.ctx = null;
        
    }
    
    public void notifyMessage(String string)
    {
        //byte[] decoded = Base64.decode(string);
        // Disable binary mode, to prevent us from confusing the browser
        this.binary = false;
        
        byte[] decoded = StringUtil.toUtfBytes(string);
        this.receive(decoded, 0, decoded.length);
    }
    
    public void notifyMessage(byte[] data)
    {
        // Ensure binary mode is activated for correct response
        this.binary = true;
        this.receive(data, 0, data.length);
    }
    
    // TODO: Expose IP information on websocket level...
    @Override
    public void fillAuthenticationKeys(HashSet<String> keys)
    {
        super.fillAuthenticationKeys(keys);
        keys.add(TrapContextKeys.LocalIP);
        keys.add(TrapContextKeys.RemoteIP);
        keys.add(TrapContextKeys.LocalPort);
        keys.add(TrapContextKeys.RemotePort);
    }
    
    @Override
    public void fillContext(Map<String, Object> context, Collection<String> filter)
    {
        super.fillContext(context, filter);
        
        if (filter.contains(TrapContextKeys.LocalIP))
            context.put(TrapContextKeys.LocalIP, ((InetSocketAddress) this.ctx.channel().localAddress()).getAddress().getHostAddress());
            
        if (filter.contains(TrapContextKeys.LocalPort))
            context.put(TrapContextKeys.LocalPort, ((InetSocketAddress) this.ctx.channel().localAddress()).getPort());
            
        if (filter.contains(TrapContextKeys.RemoteIP))
            context.put(TrapContextKeys.RemoteIP, ((InetSocketAddress) this.ctx.channel().remoteAddress()).getAddress().getHostAddress());
            
        if (filter.contains(TrapContextKeys.RemotePort))
            context.put(TrapContextKeys.RemotePort, ((InetSocketAddress) this.ctx.channel().remoteAddress()).getPort());
            
    }
    
    public void notifyPong(byte[] payload)
    {
        // TODO: Change keepalives to use WebSockets?
        
    }
    
    public void notifyError(Throwable t)
    {
        this.logger.error("WebSocket Error", t);
        notifyError();
    }
    
    protected void updateConfig()
    {
        
        String eString = this.getOption(OPTION_BINARY);
        if (eString != null)
        {
            try
            {
                this.binary = Boolean.parseBoolean(eString);
            }
            catch (Exception e)
            {
                this.logger.warn("Failed to parse transport {} binary flag", this.getTransportName(), e);
            }
        }
        
        super.updateConfig();
    }
    
    public ChannelHandler serverHandshake(ChannelHandlerContext ctx, FullHttpRequest msg)
    {
        
        this.ctx = ctx;
        // Handshake
        WebSocketServerHandshaker handshaker;
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(msg), null, true);
        handshaker = wsFactory.newHandshaker(msg);
        if (handshaker == null)
        {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
        else
        {
            ChannelPromise promise = ctx.channel().newPromise();
            promise.addListener(future -> {
                if (promise.isSuccess())
                {
                    this.notifyOpen();
                }
                else
                {
                    this.notifyError();
                }
            });
            handshaker.handshake(ctx.channel(), msg, null, promise);
        }
        
        return new MyChannelHandler((ctx1, frame) -> {
            handshaker.close(ctx1.channel(), (CloseWebSocketFrame) frame.retain());
        });
    }
    
    class MyChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame>
    {
        private BiConsumer<ChannelHandlerContext, WebSocketFrame> closeHandler;
        
        public MyChannelHandler(BiConsumer<ChannelHandlerContext, WebSocketFrame> closeHandler)
        {
            super(WebSocketFrame.class);
            this.closeHandler = closeHandler;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception
        {
            if (frame instanceof CloseWebSocketFrame)
            {
                closeHandler.accept(ctx, frame);
            }
            else if (frame instanceof PingWebSocketFrame)
            {
                ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            }
            else if (frame instanceof BinaryWebSocketFrame)
            {
                ByteBuf content = frame.content();
                if (content.hasArray())
                    notifyMessage(((BinaryWebSocketFrame) frame).content().array());
                else
                {
                    int readableBytes = content.readableBytes();
                    byte[] buf = new byte[readableBytes];
                    content.readBytes(buf);
                    notifyMessage(buf);
                }
            }
            else if (frame instanceof TextWebSocketFrame)
            {
                notifyMessage(((TextWebSocketFrame) frame).text());
            }
            
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            notifyError(cause);
            ctx.close();
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx)
        {
            notifyClose();
        }
    }
    
    private String getWebSocketLocation(FullHttpRequest req)
    {
        String uri = req.uri();
        String location = req.headers().get(HttpHeaderNames.HOST) + uri;
        if (ssl)
        {
            return "wss://" + location;
        }
        else
        {
            return "ws://" + location;
        }
    }
    
}
