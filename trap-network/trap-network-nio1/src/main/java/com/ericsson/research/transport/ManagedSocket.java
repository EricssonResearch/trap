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
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ManagedSocket implements NioEndpoint {
	
	public static enum State { NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING }
	
	private State mState;
	private NioManager	nioManager = NioManager.instance();
	private SelectionKey	key;
	private InetSocketAddress mSocketAddress;
	private ManagedSocketClient mClient;
	
	public ManagedSocket() {
		this(false);
	}
	
	protected ManagedSocket(boolean connected)
	{
		if (connected)
			this.mState = State.CONNECTED;
		else
			this.mState = State.NOT_CONNECTED;
	}

	public void registerClient(ManagedSocketClient client) {
		if (this.mClient != null)
			return;
		this.mClient = client;
	}
	
	public State getState() {
		return this.mState;
	}
	
	public InetSocketAddress getInetAddress() {
		return this.mSocketAddress;
	}
	
	public void connect(String host, int port) throws UnknownHostException {
		this.connect(new InetSocketAddress(InetAddress.getByName(host), port));
	}
	
	public void connect(InetSocketAddress address) {
		if (this.mState != State.NOT_CONNECTED)
			throw new IllegalStateException("not connected");
		
		if (this.mClient == null)
			throw new IllegalStateException("Attempted to open a socket with no registered client");
		
		this.mSocketAddress = address;
		this.mState = State.CONNECTING;
		this.nioManager.open(this, this.mSocketAddress);
	}
	
	public void write(byte[] data) throws IOException {
		if (this.mState != State.CONNECTED)
			throw new IllegalStateException("not connected");
		
		this.write(data, data.length);
	}

	public void write(byte[] data, int size) throws IOException
	{
		
		if (this.key == null)
		{
			System.err.println("Breakpoint for debugging here");
			return;
		}
		
		this.nioManager.send(this.key, data, size);
	}
	
	public void disconnect() {
		if (this.mState == State.NOT_CONNECTED)
			// illegal state?
			// Vlad: Don't think so. close() repeated times is generally a non-volatile operation
			return;
		
		this.mState = State.DISCONNECTING;

		// closing the channel will remove any associated selection keys
		this.nioManager.close(this.key);
	}
	
	
	public NioEndpoint createAcceptChild()
	{
		return null;
	}
	
	public void notifyAccepted(NioEndpoint endpoint)
	{
		throw new IllegalAccessError();
	}
	
	public void notifyClosed()
	{
		if (this.mState == State.NOT_CONNECTED)
			return;

		this.mState = State.NOT_CONNECTED;
		this.mClient.notifyDisconnected();
	}
	
	public void notifyConnected()
	{
		this.mState = State.CONNECTED;
		
		this.mClient.notifyConnected();
	}
	
	public void notifyError(Exception e)
	{
		if (this.mState == State.NOT_CONNECTED)
			return;

		this.mState = State.NOT_CONNECTED;
		this.mClient.notifyError(e);
	}
	
	public void receive(byte[] data, int size)
	{
		this.mClient.notifySocketData(data, size); 
	}
	
	public void setNioManager(NioManager nioManager, SelectionKey key)
	{
		this.nioManager = nioManager;
		this.key = key;
	}

	public boolean canAccept()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable
	{
		if (this.mState != State.NOT_CONNECTED)
		{
			this.mState = State.DISCONNECTING;
			this.nioManager.close(this.key);
		}
		super.finalize();
	}

	public SelectionKey getKey()
	{
		return this.key;
	}

	public static byte[] copy(byte[] data, int size)
	{
		byte[] rv = new byte[size];
		System.arraycopy(data, 0, rv, 0, size);
		return rv;
	}

	public InetSocketAddress getLocalSocketAddress()
	{
		return (InetSocketAddress) ((SocketChannel) this.key.channel()).socket().getLocalSocketAddress();
	}
	
	public InetSocketAddress getRemoteSocketAddress()
	{
		return (InetSocketAddress) ((SocketChannel) this.key.channel()).socket().getRemoteSocketAddress();
	}

}
