
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.ericsson.research.trap.nio.ServerSocket;

public class Nio2ServerSocket implements ServerSocket
{
    private final AsynchronousServerSocketChannel ss;
    private final ServerSocketHandler handler;
    
    public Nio2ServerSocket(AsynchronousServerSocketChannel ss, ServerSocketHandler handler)
    {
        this.ss = ss;
        this.handler = handler;
    }
    
    public InetSocketAddress getInetAddress() throws IOException
    {
        return (InetSocketAddress) ss.getLocalAddress();
    }
    
    public void listen(int port) throws IOException
    {
        this.listen("localhost", port);
    }
    
    public void listen(InetAddress host, int port) throws IOException
    {
        this.listen(new InetSocketAddress(host, port));
    }
    
    public void listen(String addr, int port) throws IOException
    {
        this.listen(new InetSocketAddress(InetAddress.getByName(addr), port));
    }
    
    public void listen(InetSocketAddress address) throws IOException
    {
        ss.bind(address);
        _accept();
    }
    
    private void _accept()
    {
        if (!ss.isOpen())
            return;
        
        ss.accept(this, new CompletionHandler<AsynchronousSocketChannel, Nio2ServerSocket>()
        {
            
            @Override
            public void completed(AsynchronousSocketChannel sock, Nio2ServerSocket arg1)
            {
                Nio2SocketBase n2s = createAcceptSocket(sock);
                handler.accept(n2s, arg1);
                n2s._read();
                _accept();
            }
            
            @Override
            public void failed(Throwable exc, Nio2ServerSocket arg1)
            {
                handler.error(exc, arg1);
                try
                {
                    ss.close();
                }
                catch (IOException e)
                {
                	handler.error(e, arg1);
                }
            }
        });
    }
    
    Nio2SocketBase createAcceptSocket(AsynchronousSocketChannel channel) {
    	return new Nio2Socket(channel);
    }

	@Override
    public void close()
    {
		try
        {
	        ss.close();
        }
        catch (IOException e)
        {
        	handler.error(e, this);
        }
    }

    @Override
    public boolean isClosed()
    {
        return !ss.isOpen();
    }
}
