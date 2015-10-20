/*
 * Copyright 2012 The Netty Project
 * 
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import com.ericsson.research.trap.spi.transports.WebServerSocketTransport;

/**
 * A HTTP server which serves Web Socket requests at: http://localhost:8080/websocket Open your browser at
 * http://localhost:8080/, then the demo page will be loaded and a Web Socket connection will be made automatically.
 * This server illustrates support for the different web socket specification versions and will work with:
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Firefox 11+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * </ul>
 */
public class WebSocketServer
{
    
    public int               port;
    
    WebServerSocketTransport t;
    
    private ServerBootstrap  bootstrap;
    
    private Channel          bind;
    
    private String           host;
    
    public WebSocketServer(String host, int port, WebServerSocketTransport t)
    {
        this.host = host;
        this.port = port;
        this.t = t;
    }
    
    public void run()
    {
        // Configure the server.
        this.bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        
        // Set up the event pipeline factory.
        this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            
            @Override
            public ChannelPipeline getPipeline() throws Exception
            {
                WebServerSocketHandler webServerSocketHandler = new WebServerSocketHandler();
                ChannelPipeline pipeline = new DefaultChannelPipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("handler", webServerSocketHandler);
                
                WebSocketServer.this.t.notifyAccept(webServerSocketHandler);
                
                return pipeline;
            }
        });
        
        // Bind and start to accept incoming connections.
        this.bind = this.bootstrap.bind(new InetSocketAddress(this.host, this.port));
        this.port = ((InetSocketAddress) this.bind.getLocalAddress()).getPort();
        
        System.out.println("Web socket server started at port " + this.port + '.');
        System.out.println("Open your browser and navigate to http://" + this.host + ":" + this.port + '/'); 
    }
    
    public void close()
    {
        ChannelFuture close = this.bind.close();
        close.syncUninterruptibly();
    }
}
