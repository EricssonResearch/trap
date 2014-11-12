package com.ericsson.research.transport.ws;

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
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class WSFactory
{
    
    public static final int                    VERSION_HIXIE_75 = 1;
    public static final int                    VERSION_HIXIE_76 = 2;
    public static final int                    VERSION_HYBI_10  = 3;
    public static final int                    VERSION_RFC_6455 = 4;
    
    private static WSFactory                   instance;
    private static final Map<String, WSServer> map              = Collections.synchronizedMap(new HashMap<String, WSServer>());
    
    public static WSFactory getInstance() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException
    {
        if (instance == null)
            instance = (WSFactory) Class.forName("0com.ericsson.research.transport.ws.spi.WSFactoryImpl".substring(1)).getConstructor(new Class[] {}).newInstance(new Object[] {});
        return instance;
    }
    
    public static WSServer startWebSocketServer(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException
    {
        final String key = host + ":" + port;
        WSServer acceptSocket = map.get(key);
        if (acceptSocket != null)
            return acceptSocket;
        WSServer sws = createWebSocketServer(host, port, serverListener, securityContext);
        if (port != 0)
            map.put(key, sws);
        return sws;
    }
    
    public static void stopWebSocketServer(WSServer serverSocket)
    {
        WSURI uri = serverSocket.getURI();
        String key = uri.getHost() + ":" + uri.getPort();
        WSServer acceptSocket = map.get(key);
        if (acceptSocket != null)
        {
            map.remove(key);
            acceptSocket.close();
        }
        else
            serverSocket.close();
    }
    
    public static WSInterface createWebSocketClient(WSURI uri, WSListener listener, int version, WSSecurityContext securityContext) throws IOException
    {
        try
        {
            return getInstance()._createWebSocketClient(uri, listener, version, securityContext);
        }
        catch (Exception e)
        {
            throw new IOException("Could not create websocket client", e);
        }
    }
    
    protected abstract WSInterface _createWebSocketClient(WSURI uri, WSListener listener, int version, WSSecurityContext securityContext) throws IOException;
    
    public static WSServer createWebSocketServer(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException
    {
        try
        {
            return getInstance()._createWebSocketServer(host, port, serverListener, securityContext);
        }
        catch (Exception e)
        {
            throw new IOException("Could not create websocket client", e);
        }
    }
    
    protected abstract WSServer _createWebSocketServer(String host, int port, WSAcceptListener serverListener, WSSecurityContext securityContext) throws IOException;
    
}
