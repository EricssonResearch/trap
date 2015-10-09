package com.ericsson.research.trap.spi.ws.netty;

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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.util.CharsetUtil;

import com.ericsson.research.trap.spi.transports.WebSocketClient;

public class NettyWSClient extends SimpleChannelUpstreamHandler
{

	private URI	uri;
	private WebSocketClientHandshaker	handshaker;
	private WebSocketClient				t;
	private Channel						ch;
	
	public NettyWSClient(String uriStr, WebSocketClient webSocketClient)
	{
		
		this.t = webSocketClient;
		this.uri = URI.create(uriStr);
		

		String protocol = this.uri.getScheme();
		if (!protocol.equals("ws"))
		{
			throw new IllegalArgumentException("Unsupported protocol: " + protocol);
		}
		
		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		
		Channel ch = null;

		HashMap<String, String> customHeaders = new HashMap<String, String>();
		customHeaders.put("MyHeader", "MyValue");
		
		// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
		// If you change it to V00, ping is not supported and remember to change
		// HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
		this.handshaker = new WebSocketClientHandshakerFactory().newHandshaker(this.uri, WebSocketVersion.V13, null, false, customHeaders);
		
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception
			{
				ChannelPipeline pipeline = Channels.pipeline();
				
				pipeline.addLast("decoder", new HttpResponseDecoder());
				pipeline.addLast("encoder", new HttpRequestEncoder());
				pipeline.addLast("ws-handler", NettyWSClient.this);
				return pipeline;
			}
		});
		
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(this.uri.getHost(), this.uri.getPort()));
		future.syncUninterruptibly();
		ch = future.getChannel();
		try
		{
			this.handshaker.handshake(ch);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		this.t.notifyClose();
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
	{
		Channel ch = ctx.getChannel();
		if (!this.handshaker.isHandshakeComplete())
		{
			this.ch = ch;
			this.handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
			this.t.notifyOpen();
			return;
		}
		
		if (e.getMessage() instanceof HttpResponse)
		{
			HttpResponse response = (HttpResponse) e.getMessage();
			throw new Exception("Unexpected HttpResponse (status=" + response.getStatus() + ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")");
		}
		
		WebSocketFrame frame = (WebSocketFrame) e.getMessage();
		if (frame instanceof BinaryWebSocketFrame)
		{
			BinaryWebSocketFrame f = (BinaryWebSocketFrame) frame;
			ChannelBuffer binaryData = f.getBinaryData();
			byte[] arr = new byte[binaryData.readableBytes()];
			binaryData.readBytes(arr);
			this.t.notifyMessage(arr);

		}
		else if (frame instanceof PongWebSocketFrame)
		{
			System.out.println("WebSocket Client received pong");
		}
		else if (frame instanceof CloseWebSocketFrame)
		{
			System.out.println("WebSocket Client received closing");
			ch.close();
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
	{
		final Throwable t = e.getCause();
		t.printStackTrace();
		e.getChannel().close();
	}
	
	public void close()
	{
		this.ch.write(new CloseWebSocketFrame());
	}
	
	public void send(byte[] serialize)
	{
		this.ch.write(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(serialize)));
	}
	
}
