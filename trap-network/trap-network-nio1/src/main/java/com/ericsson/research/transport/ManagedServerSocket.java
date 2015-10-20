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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

public class ManagedServerSocket implements NioEndpoint
{

    public enum State { NOT_LISTENING, LISTENING }

    private State mState;
    private NioManager mChannelManager;
    private ManagedServerSocketClient mClient;
	private SelectionKey	key;

    public ManagedServerSocket() {
        this.mState = State.NOT_LISTENING;
    }
    
    public void registerClient(ManagedServerSocketClient client) {
        if (this.mClient != null)
            return;

        if (this.mChannelManager == null)
        	this.mChannelManager = NioManager.instance();
        
        this.mClient = client;
    }

    public State getState() {
        return this.mState;
    }

    public InetSocketAddress getInetAddress() {
    	ServerSocket socket = ((ServerSocketChannel) this.key.channel()).socket();
        return new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());
    }
    
    public void listen(int port) throws IOException {
        this.listen("localhost", port);
    }

	public void listen(InetAddress host, int port) {
        this.listen(new InetSocketAddress(host, port));
	}
    
    public void listen(String addr, int port) throws IOException {
        this.listen(new InetSocketAddress(InetAddress.getByName(addr), port));
    }

    public void listen(InetSocketAddress address) {

        if (this.mState != State.NOT_LISTENING)
            throw new IllegalStateException("already listening");
        
        this.mState = State.LISTENING;

        this.mChannelManager.bind(this, address, true);
    }

    public synchronized void close() {
    	
        if (this.mState != State.LISTENING)
            return;

        this.mChannelManager.close(this.key);
        
        while (this.mState == State.LISTENING)
        {
        	try
        	{
        		this.wait();
        	}
        	catch (InterruptedException e)
        	{}
        }
    }

	public boolean canAccept()
	{
		return true;
	}

	public NioEndpoint createAcceptChild() throws UnsupportedOperationException
	{
		return new ManagedSocket(true);
	}

	public void notifyAccepted(NioEndpoint endpoint)
	{
        this.mClient.notifyAccept((ManagedSocket)endpoint);
	}

	public synchronized void notifyClosed()
	{
        this.mState = State.NOT_LISTENING;
		this.notify();
	}

	public void notifyConnected()
	{
		this.mClient.notifyBound(this);
	}

	public void notifyError(Exception e)
	{
		this.mClient.notifyError(e);
	}

	public void receive(byte[] data, int size)
	{
		
	}

	public void setNioManager(NioManager nioManager, SelectionKey key)
	{
		this.mChannelManager = nioManager;
		this.key = key;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable
	{

        this.mChannelManager.close(this.key);
		super.finalize();
	}

	public SelectionKey getKey()
	{
		return this.key;
	}

}
