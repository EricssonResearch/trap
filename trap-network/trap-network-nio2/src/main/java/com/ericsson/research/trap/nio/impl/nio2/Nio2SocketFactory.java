/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.research.trap.nio.impl.nio2;

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
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

import javax.net.ssl.SSLContext;

import com.ericsson.research.trap.nio.ServerSocket;
import com.ericsson.research.trap.nio.SocketFactory;
import com.ericsson.research.trap.nio.ServerSocket.ServerSocketHandler;
import com.ericsson.research.trap.nio.Socket;

/**
 *
 * @author Vladimir Katardjiev
 */
public class Nio2SocketFactory implements SocketFactory {
    
	@Override
    public Socket client() throws IOException {

        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        return new Nio2Socket(channel);
        
    }

    @Override
    public ServerSocket server(ServerSocketHandler handler) throws IOException
    {
        AsynchronousServerSocketChannel ss = AsynchronousServerSocketChannel.open();
        return new Nio2ServerSocket(ss, handler);
    }

	@Override
    public Socket sslClient(SSLContext sslc) throws IOException
    {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
	    return new Nio2SSLSocket(channel, sslc, true);
    }

	@Override
    public ServerSocket sslServer(SSLContext sslc, ServerSocketHandler handler) throws IOException
    {
        AsynchronousServerSocketChannel ss = AsynchronousServerSocketChannel.open();
	    return new Nio2SSLServerSocket(ss, handler, sslc);
    }
    
}
