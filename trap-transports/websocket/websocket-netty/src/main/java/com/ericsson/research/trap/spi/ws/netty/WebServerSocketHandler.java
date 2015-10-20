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

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;

import com.ericsson.research.trap.spi.transports.WebSocketTransport;

public class WebServerSocketHandler extends SimpleChannelUpstreamHandler
{
	@SuppressWarnings("unused")
	private static final InternalLogger	logger			= InternalLoggerFactory.getInstance(WebServerSocketHandler.class);
	
	private static final String			WEBSOCKET_PATH	= "/websocket";
	
	private WebSocketServerHandshaker	handshaker;
	public WebSocketTransport			t;
	
	private ChannelHandlerContext		ctx;
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
	{
		Object msg = e.getMessage();
		if (msg instanceof HttpRequest)
		{
			this.handleHttpRequest(ctx, (HttpRequest) msg);
		}
		else if (msg instanceof WebSocketFrame)
		{
			this.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}
	
	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception
	{
		// Allow only GET methods.
		if (req.getMethod() != GET)
		{
			sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}
		
		// Send the demo page and favicon.ico
		if (req.getUri().equals("/"))
		{
			return;
		}
		else if (req.getUri().equals("/favicon.ico"))
		{
			HttpResponse res = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
			sendHttpResponse(ctx, req, res);
			return;
		}
		
		this.ctx = ctx;
		
		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
		this.handshaker = wsFactory.newHandshaker(req);
		if (this.handshaker == null)
		{
			wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
		}
		else
		{
			ChannelFuture handshake = this.handshaker.handshake(ctx.getChannel(), req);
			handshake.addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
			handshake.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception
				{
					WebServerSocketHandler.this.t.notifyOpen();
				}
			});
		}
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
	{
		
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame)
		{
			this.t.notifyClose();
			this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
			return;
		}
		else if (frame instanceof PingWebSocketFrame)
		{
			ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
			return;
		}
		else if ((frame instanceof TextWebSocketFrame))
		{
			ChannelBuffer binaryData = ((TextWebSocketFrame) frame).getBinaryData();
			byte[] arr = new byte[binaryData.readableBytes()];
			binaryData.readBytes(arr);
			this.t.notifyMessage(arr);
		}
		else
		{
			// Send the uppercase string back.
			ChannelBuffer binaryData = ((BinaryWebSocketFrame) frame).getBinaryData();
			byte[] arr = new byte[binaryData.readableBytes()];
			binaryData.readBytes(arr);
			this.t.notifyMessage(arr);
		}
	}
	
	private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res)
	{
		// Generate an error page if response status code is not OK (200).
		if (res.getStatus().getCode() != 200)
		{
			res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
			setContentLength(res, res.getContent().readableBytes());
		}
		
		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(res);
		if (!isKeepAlive(req) || (res.getStatus().getCode() != 200))
		{
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
	{
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
	
	private static String getWebSocketLocation(HttpRequest req)
	{
		return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
	}
	
	public void send(byte[] b)
	{
		// How simple isn't this?
		this.ctx.getChannel().write(new BinaryWebSocketFrame(new BigEndianHeapChannelBuffer(b)));
	}
}
