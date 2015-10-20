package com.ericsson.research.trap.spi.transports;

import java.net.InetSocketAddress;
import java.util.Collection;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.TrapEndpointImpl;
import com.ericsson.research.trap.spi.ListenerTrapTransport;
import com.ericsson.research.trap.spi.ListenerTrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapConfiguration;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportDelegate;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.nhttp.WebSocketConstants;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class ServerWebSocketTransport extends AbstractListenerTransport implements ListenerTrapTransport
{
    public static final String            REGISTER_RESOURCE = "_connectTrapWS";
    private ListenerTrapTransportDelegate listenerDelegate;
    private Object                        listenerContext;
    private ListenerHttpTransport         server;
    
    public String getTransportName()
    {
        return "websocket";
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.WEBSOCKET;
    }
    
    public void listen(ListenerTrapTransportDelegate listener, Object context) throws TrapException
    {
        
        TrapEndpointImpl ep = (TrapEndpointImpl) listener;
        Collection<TrapTransport> transports = ep.getTransports();
        
        server = null;
        
        for (TrapTransport t : transports)
            if (t instanceof ListenerHttpTransport)
                server = (ListenerHttpTransport) t;
                
        if (server == null)
            throw new TrapException("Could not locate the appropriate server!");
        
        server.webSocket = this;
            
        this.listenerDelegate = listener;
        this.listenerContext = context;
        
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
        
    }
    
    public void getClientConfiguration(TrapConfiguration destination, String defaultHost)
    {
        
        if (server == null || server.ch == null)
            return;
            
        InetSocketAddress address = (InetSocketAddress) server.ch.localAddress();
        
        // Check for pre-existing port
        String port = this.getOption("autoconfig.port");
        
        if (port == null)
            port = Integer.toString(address.getPort());
            
        String hostName = this.getOption("autoconfig.host");
        
        if (hostName == null)
            hostName = defaultHost;
            
        if (hostName == null)
            hostName = this.getHostName(address.getAddress(), this.server.defaultHost, true);
            
        String targetUri = "ws" + (this.server.secure ? "s" : "") + "://" + hostName + ":" + port + "/" + REGISTER_RESOURCE;
        
        destination.setOption(this.prefix, WebSocketConstants.CONFIG_URI, targetUri);
        
    }
    
    @Override
    protected void internalDisconnect()
    {
        server.hostedObjects.remove(REGISTER_RESOURCE);
    }
    
    @Override
    public void flushTransport()
    {
    
    }
    
    public ChannelHandler createHandler(ChannelHandlerContext ctx, FullHttpRequest msg)
    {
        WebSocketTransport transport = new WebSocketTransport();
        this.listenerDelegate.ttsIncomingConnection(transport, this, this.listenerContext);
        ChannelHandler handler = transport.serverHandshake(ctx, msg);
        return handler;
    }
    
}
