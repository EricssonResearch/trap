package com.ericsson.research.trap.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import com.ericsson.research.trap.nio.Socket.SocketHandler;

public class NioOutputStream extends OutputStream implements SocketHandler
{
    LinkedBlockingDeque<ByteBuffer> bufs          = new LinkedBlockingDeque<ByteBuffer>();
    private SocketHandler           handler;
    private Socket                  sock;
    private boolean                 closeWhenDone = false;
    
    public NioOutputStream(Socket sock)
    {
        this.sock = sock;
        handler = sock.getHandler();
        sock.setHandler(this);
    }
    
    @Override
    public synchronized void sent(Socket sock)
    {
        ByteBuffer peek = bufs.peek();
        
        while (peek != null && !peek.hasRemaining())
            peek = bufs.poll();
        
        if (peek == null)
        {
            if (closeWhenDone && sock.getHandler().equals(this))
                sock.setHandler(handler);
            return;
        }
        
        sock.send(peek);
        
    }
    
    @Override
    public void received(ByteBuffer data, Socket sock)
    {
        handler.received(data, sock);
    }
    
    @Override
    public void opened(Socket sock)
    {
        handler.opened(sock);
    }
    
    @Override
    public void closed(Socket sock)
    {
        handler.closed(sock);
    }
    
    @Override
    public void error(Throwable exc, Socket sock)
    {
        handler.error(exc, sock);
    }
    
    @Override
    public void write(int b) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(4).putInt(b);
        buffer.flip();
        bufs.add(buffer);
        sent(sock);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(len - off).put(b, off, len);
        buffer.flip();
        bufs.add(buffer);
        sent(sock);
    }

    public void setHandler(SocketHandler handler)
    {
        this.handler = handler;
    }
}
