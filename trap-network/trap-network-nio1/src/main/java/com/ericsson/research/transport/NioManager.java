package com.ericsson.research.transport;

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

import java.io.IOError;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CoderMalfunctionError;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioManager implements Runnable
{
    
    private static final int                                         MAX_OUTGOING_SOCKETS                             = 15;
    private static final String                                      KEY_METADATA_LAST_OUTPUT_BUFFER_CHANGE_TIMESTAMP = "KEY_METADATA_LAST_OUTPUT_BUFFER_CHANGE_TIMESTAMP";
    private static final String                                      KEY_METADATA_LAST_OUTPUT_BUFFER_SIZE             = "KEY_METADATA_LAST_OUTPUT_BUFFER_SIZE";
    private static final String                                      KEY_METADATA_LAST_BUFFER_REMAINING               = "KEY_METADATA_LAST_BUFFER_REMAINING";
    private static final long                                        SOCKET_CLOSE_TIMEOUT                             = 5000;
    
    private static final long                                        SOCKET_WRITE_TIMEOUT                             = 15000;
    private static final String                                      KEY_METADATA_LAST_WRITE_TIMESTAMP                = "KEY_METADATA_LAST_WRITE_TIMESTAMP";
    
    private static NioManager                                        instance;
    
    private final ByteBuffer                                         readBuffer                                       = ByteBuffer.allocate(8192);
    private final Map<SelectionKey, LinkedBlockingQueue<ByteBuffer>> outputBuffers                                    = new ConcurrentHashMap<SelectionKey, LinkedBlockingQueue<ByteBuffer>>();
    private final Map<SelectionKey, NioReference<NioEndpoint>>       sockets                                          = new ConcurrentHashMap<SelectionKey, NioReference<NioEndpoint>>();
    private final Collection<NioWaitingSocket>                       waitingSockets                                   = new ConcurrentLinkedQueue<NioWaitingSocket>();
    private final Collection<SelectionKey>                           closeKeys                                        = new ConcurrentLinkedQueue<SelectionKey>();
    private final Collection<SelectionKey>                           writeKeys                                        = new ConcurrentLinkedQueue<SelectionKey>();
    private final Map<SelectionKey, NioWaitingSocket>                connectingSockets                                = new ConcurrentHashMap<SelectionKey, NioWaitingSocket>();
    
    private Selector                                                 selector;
    private Thread                                                   nioThread                                        = null;
    
    public static synchronized NioManager instance()
    {
        if (instance == null)
            instance = new NioManager();
        if (instance.nioThread == null)
            instance.start();
        return instance;
    }
    
    private NioManager()
    {
    }
    
    public static void reset() throws IOException
    {
        if (instance != null)
            instance.stop();
        instance = null;
    }
    
    public void start()
    {
        try
        {
            this.selector = SelectorProvider.provider().openSelector();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        this.nioThread = new Thread(this);
        this.nioThread.start();
    }
    
    public void stop() throws IOException
    {
        this.selector.close();
    }
    
    public void run()
    {
        for (;;)
        {
            try
            {
                
                if (!this.writeKeys.isEmpty())
                {
                    synchronized (this.writeKeys)
                    {
                        for (SelectionKey key : this.writeKeys)
                            if (key.isValid())
                                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        this.writeKeys.clear();
                    }
                }
                
                synchronized (this.selector)
                {
                    
                    // So the iterators below don't tell the whole truth, and it is right that a closeKey can trigger a waitingSocket
                    // Therefore, since I ignore the call to wakeup to not block the thread in some circumstances, I need to make
                    // this check here.
                    if (this.waitingSockets.isEmpty() && this.closeKeys.isEmpty())
                    {
                        if ((this.connectingSockets.size() == 0) && (this.writeKeys.size() == 0))
                            this.selector.select(10000); // Once every ten seconds is acceptable to wake up just in case a race would cause a deadlock.
                        else
                            this.selector.select(1000); // If sockets are connecting, wake up more often to check their timeouts.
                    }
                    else
                    {
                        // Do a very short timeout, in order to process the next set of sockets.
                        this.selector.select(10);
                    }
                    
                    Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext())
                    {
                        SelectionKey key = selectedKeys.next();
                        selectedKeys.remove();
                        if (!key.isValid())
                            continue;
                        try
                        {
                            if (key.isReadable())
                                this.read(key);
                            else if (key.isWritable())
                                this.write(key);
                            else if (key.isAcceptable())
                                this.accept(key);
                            else if (key.isConnectable())
                                this.connect(key);
                        }
                        catch (Throwable e)
                        {
                            
                            // Some conditions are death conditions.
                            if (e instanceof CoderMalfunctionError || e instanceof IOError || e instanceof ThreadDeath)
                                throw new RuntimeException(e);
                            
                            // For everything else, we shouldn't allow the NioManager to be killed.
                            //e.printStackTrace();
                            this.close(key);
                            NioEndpoint ne = this.sockets.get(key).get();
                            if (ne != null)
                                ne.notifyError(e instanceof Exception ? (Exception) e : new Exception(e));
                        }
                    }
                    
                }
                
                // Add write ops, if applicable
                //              synchronized(outputBuffers)
                //              {
                //                  Iterator<SelectionKey> keys = outputBuffers.keySet().iterator();
                //                  while(keys.hasNext())
                //                  {
                //                      SelectionKey key = keys.next();
                //                      if (!key.isValid())
                //                          continue;
                //
                //                      ArrayList<ByteBuffer> ob = outputBuffers.get(key);
                //
                //                      if (!ob.isEmpty())
                //                          key.interestOps(// SelectionKey.OP_READ |  (NOTE: Not mixing read & write in first revision)
                //                                  SelectionKey.OP_WRITE);
                //                  }
                //              }
                
                // Add waiting socket, if applicable
                if (!this.waitingSockets.isEmpty())
                {
                    synchronized (this.waitingSockets)
                    {
                        Iterator<NioWaitingSocket> it = this.waitingSockets.iterator();
                        while (it.hasNext())
                        {
                            NioWaitingSocket waitingSocket = it.next();
                            it.remove();
                            boolean accept = (waitingSocket.getOps() & SelectionKey.OP_ACCEPT) != 0;
                            if (accept && (this.connectingSockets.size() > MAX_OUTGOING_SOCKETS))
                                continue;
                            NioEndpoint endpoint = waitingSocket.getSocket();
                            try
                            {
                                SelectionKey key = waitingSocket.createChannel().register(this.selector, waitingSocket.getOps());
                                endpoint.setNioManager(this, key);
                                this.outputBuffers.put(key, new LinkedBlockingQueue<ByteBuffer>());
                                this.sockets.put(key, new NioReference<NioEndpoint>(endpoint));
                                if (accept)
                                    endpoint.notifyConnected();
                                else
                                    this.connectingSockets.put(key, waitingSocket);
                            }
                            catch (Exception e)
                            {
                                endpoint.notifyError(e);
                            }
                            finally
                            {
                                synchronized (waitingSocket)
                                {
                                    waitingSocket.setDone(true);
                                    waitingSocket.notifyAll();
                                }
                            }
                        }
                    }
                }
                
                if (!this.connectingSockets.isEmpty())
                {
                    // Check timeouts on sockets that are connecting.
                    Iterator<NioWaitingSocket> it = this.connectingSockets.values().iterator();
                    while (it.hasNext())
                    {
                        NioWaitingSocket socket = it.next();
                        if (System.currentTimeMillis() > socket.getTimeoutTime())
                        {
                            it.remove();
                            socket.getChannel().close();
                            // timeout = error, shall we say?
                            socket.getSocket().notifyError(new IOException("Timed out while trying to connect"));
                        }
                    }
                }
                
                if (!this.closeKeys.isEmpty())
                {
                    // Closed notifications will wait
                    LinkedList<NioEndpoint> closedEndpoints = new LinkedList<NioEndpoint>();
                    synchronized (this.closeKeys)
                    {
                        Iterator<SelectionKey> it = this.closeKeys.iterator();
                        while (it.hasNext())
                        {
                            SelectionKey key = it.next();
                            Collection<ByteBuffer> ob = this.outputBuffers.get(key);
                            
                            HashMap<String, Object> metadata = this.getMetadata(key);
                            
                            /*
                             * This case covers the case where we are flushing data.
                             * This is verified every second, but in some cases, sockets can get
                             * stuck in this case. What we'll do is verify no data was sent over a certain period
                             * and, if true, remove the socket anyway
                             */
                            if ((ob != null) && !ob.isEmpty() && key.channel().isOpen())
                            {
                                
                                Long lastChange = (Long) metadata.get(KEY_METADATA_LAST_OUTPUT_BUFFER_CHANGE_TIMESTAMP);
                                Integer lastSize = (Integer) metadata.get(KEY_METADATA_LAST_OUTPUT_BUFFER_SIZE);
                                Integer lastBufRemaining = (Integer) metadata.get(KEY_METADATA_LAST_BUFFER_REMAINING);
                                
                                ByteBuffer first = ob.iterator().next();
                                
                                int size = ob.size();
                                int remaining = first.remaining();
                                long cTime = System.currentTimeMillis();
                                
                                if (lastChange == null)
                                    lastChange = cTime;
                                
                                if (lastSize == null)
                                    lastSize = size;
                                
                                if (lastBufRemaining == null)
                                    lastBufRemaining = remaining;
                                
                                // Now for some verification
                                
                                long endTime = lastChange.longValue() + SOCKET_CLOSE_TIMEOUT;
                                boolean doContinue = true;
                                
                                // We're within the timeout window!!!
                                if (cTime >= endTime)
                                {
                                    if (size == lastSize.intValue())
                                    {
                                        // Nest the ifs so it's easier to debug
                                        
                                        if (remaining == lastBufRemaining)
                                        {
                                            // Same number of buffers, same index, time is
                                            doContinue = false;
                                        }
                                    }
                                }
                                
                                // Update the params. Technically, this means we'll only perform the comparison after 30 seconds,
                                // leading to an effective timeout time of 59 seconds (or 2*socket_close_timeout) but this way simplifies
                                // the calculations necessary
                                if (doContinue)
                                {
                                    metadata.put(KEY_METADATA_LAST_OUTPUT_BUFFER_CHANGE_TIMESTAMP, cTime);
                                    metadata.put(KEY_METADATA_LAST_OUTPUT_BUFFER_SIZE, size);
                                    metadata.put(KEY_METADATA_LAST_BUFFER_REMAINING, remaining);
                                    continue;
                                }
                            }
                            
                            NioEndpoint socket = null;
                            try
                            {
                                socket = this.sockets.remove(key).get();
                            }
                            catch (Exception e)
                            {
                            }
                            
                            try
                            {
                                key.channel().close();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            
                            key.cancel();
                            
                            synchronized (this.outputBuffers)
                            {
                                this.outputBuffers.remove(key);
                            }
                            
                            it.remove();
                            
                            if (socket != null)
                                closedEndpoints.add(socket);
                        }
                    }
                    
                    if (!this.writeKeys.isEmpty())
                    {
                        synchronized (this.writeKeys)
                        {
                            long now = System.currentTimeMillis();
                            for (SelectionKey key : this.writeKeys)
                            {
                                HashMap<String, Object> metadata = this.getMetadata(key);
                                Long lastWrite = (Long) metadata.get(KEY_METADATA_LAST_WRITE_TIMESTAMP);
                                
                                if (lastWrite == null)
                                    lastWrite = now;
                                
                                if ((lastWrite.longValue() + SOCKET_WRITE_TIMEOUT) < now)
                                    this.close(key);
                            }
                        }
                    }
                    
                    // Fire off closed notifications AFTER the synchronized block
                    for (NioEndpoint socket : closedEndpoints)
                    {
                        try
                        {
                            socket.notifyClosed();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (ClosedSelectorException e)
            {
                // This just means we were forcefully closed
                this.selector = null;
                this.nioThread = null;
                this.outputBuffers.clear();
                this.sockets.clear();
                this.waitingSockets.clear();
                this.closeKeys.clear();
                this.writeKeys.clear();
                this.connectingSockets.clear();
                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> getMetadata(SelectionKey key)
    {
        HashMap<String, Object> attachment = (HashMap<String, Object>) key.attachment();
        
        if (attachment == null)
        {
            attachment = new HashMap<String, Object>();
            key.attach(attachment);
        }
        
        return attachment;
    }
    
    private void connect(SelectionKey key) throws IOException
    {
        IOException error = null;
        try
        {
            if (!((SocketChannel) key.channel()).finishConnect())
                error = new IOException("Failed to connect");
        }
        catch (IOException e)
        {
            error = e;
            // Try to recover from the error
            if ("Connection reset by peer".equals(e.getMessage()))
            {
                NioWaitingSocket nws = this.connectingSockets.get(key);
                if (nws != null)
                {
                    if (nws.getRetries() > 0)
                    {
                        synchronized (this.waitingSockets)
                        {
                            this.waitingSockets.add(nws);
                        }
                        nws.setRetries(nws.getRetries() - 1);
                        return;
                    }
                }
            }
        }
        this.connectingSockets.remove(key);
        if (error == null)
        {
            NioEndpoint ne = this.sockets.get(key).get();
            if (ne != null)
                ne.notifyConnected();
            key.interestOps(SelectionKey.OP_READ);
        }
        else
            throw error;
    }
    
    private void write(SelectionKey key) throws IOException
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try
        {
            LinkedBlockingQueue<ByteBuffer> queue = this.outputBuffers.get(key);
            // Write until there's not more data ...
            while (!queue.isEmpty())
            {
                ByteBuffer buf = queue.peek();
                
                int wrote = socketChannel.write(buf);
                if ((wrote == 0) || (buf.remaining() > 0))
                {
                    /*
                        System.out.print("### = "+buf.remaining()+" ("+wrote+") - [");
                        byte[] b = buf.array();
                        for(int i=0;i<b.length;i++)
                            System.out.print((char)b[i]);
                        System.out.println("]");
                     */
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.poll();
            }
            if (queue.isEmpty())
            {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                synchronized (this.writeKeys)
                {
                    // Wrap in synchronized block prevents us from overwriting the result from the main run loop that is clearing writeKeys.
                    if (queue.isEmpty())
                        key.interestOps(SelectionKey.OP_READ);
                }
            }
            
            HashMap<String, Object> metadata = this.getMetadata(key);
            metadata.put(KEY_METADATA_LAST_WRITE_TIMESTAMP, Long.valueOf(System.currentTimeMillis()));
        }
        catch (IOException e)
        {
            this.outputBuffers.remove(key);
            throw e;
        }
    }
    
    private void read(SelectionKey key) throws IOException
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.readBuffer.clear();
        int numRead = socketChannel.read(this.readBuffer);
        if (numRead <= -1)
        {
            // This key will never flush anything
            LinkedBlockingQueue<ByteBuffer> queue = this.outputBuffers.get(key);
            queue.clear();
            this.close(key);
        }
        else
        {
            NioEndpoint socket = this.sockets.get(key).get();
            if (socket != null)
                socket.receive(this.readBuffer.array(), numRead);
        }
    }
    
    protected void close(SelectionKey key)
    {
        if (key == null)
            return;
        synchronized (this.closeKeys)
        {
            this.closeKeys.add(key);
        }
        
        if (Thread.currentThread() != this.nioThread)
            this.selector.wakeup();
    }
    
    private void accept(SelectionKey key) throws IOException
    {
        NioEndpoint endpoint = this.sockets.get(key).get();
        
        if (endpoint == null)
        {
            key.cancel();
            return;
        }
        
        NioEndpoint newPoint = endpoint.createAcceptChild();
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        SelectionKey channelKey = channel.register(this.selector, SelectionKey.OP_READ);
        newPoint.setNioManager(this, channelKey);
        this.sockets.put(channelKey, new NioReference<NioEndpoint>(newPoint));
        this.outputBuffers.put(channelKey, new LinkedBlockingQueue<ByteBuffer>());
        endpoint.notifyAccepted(newPoint);
    }
    
    protected void send(SelectionKey key, byte[] data, int size) throws IOException
    {
        if (!key.isValid())
            throw new IOException("Transport is no longer available");
        if (this.closeKeys.contains(key))
            throw new IOException("Cannot send to a closed socket.");
        // Wrap the data in a buffer outside the synchronized to not block the main thread.
        byte[] mData = new byte[size];
        System.arraycopy(data, 0, mData, 0, size);
        ByteBuffer byteBuffer = ByteBuffer.wrap(mData);
        
        this.outputBuffers.get(key).add(byteBuffer);
        
        synchronized (this.writeKeys)
        {
            this.writeKeys.add(key);
        }
        
        if (Thread.currentThread() != this.nioThread)
            this.selector.wakeup();
    }
    
    public void open(NioEndpoint socket, InetSocketAddress remote)
    {
        this.open(socket, remote, false);
    }
    
    public void open(NioEndpoint socket, InetSocketAddress remote, boolean wait)
    {
        NioWaitingSocket waitingSocket = new NioWaitingSocket(socket, remote, false, SelectionKey.OP_CONNECT);
        synchronized (this.waitingSockets)
        {
            this.waitingSockets.add(waitingSocket);
        }
        
        if (Thread.currentThread() != this.nioThread)
            this.selector.wakeup();
        if (wait && !this.nioThread.equals(Thread.currentThread()))
        {
            synchronized (waitingSocket)
            {
                while (!waitingSocket.isDone())
                {
                    try
                    {
                        this.selector.wakeup();
                        waitingSocket.wait(1000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }
    
    public void bind(NioEndpoint endpoint, SocketAddress local, boolean wait)
    {
        if (!endpoint.canAccept())
            throw new IllegalArgumentException("Received endpoint of type " + endpoint.getClass() + " that cannot accept");
        NioWaitingSocket waitingSocket = new NioWaitingSocket(endpoint, local, true, SelectionKey.OP_ACCEPT);
        synchronized (this.waitingSockets)
        {
            this.waitingSockets.add(waitingSocket);
            
            if (Thread.currentThread() != this.nioThread)
                this.selector.wakeup();
        }
        if (wait && !this.nioThread.equals(Thread.currentThread()))
        {
            synchronized (waitingSocket)
            {
                while (!waitingSocket.isDone())
                {
                    try
                    {
                        waitingSocket.wait(1000);
                        this.selector.wakeup();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    class NioReference<T extends NioEndpoint> extends WeakReference<T>
    {
        
        public NioReference(T referent)
        {
            super(referent);
        }
        
        public void clear()
        {
            T ref = this.get();
            if (ref != null)
                NioManager.this.close(ref.getKey());
            super.clear();
        }
        
    }
    
}
