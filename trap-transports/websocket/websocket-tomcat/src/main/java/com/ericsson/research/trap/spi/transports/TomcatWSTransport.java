package com.ericsson.research.trap.spi.transports;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.tomcat.WSSocket;
import com.ericsson.research.trap.spi.tomcat.WSSocket.Delegate;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;

public class TomcatWSTransport extends AbstractTransport implements Delegate
{
	
	private static final String		OPTION_BINARY	= "binary";
	private WSSocket	socket;
	@SuppressWarnings("unused")
	private boolean					binary			= true;
	private long					lastSend		= 0;
	private boolean					delayed			= false;
	private boolean					delayQueued		= false;
	private boolean					useDelay		= false;
	ByteArrayOutputStream			outBuf			= new ByteArrayOutputStream();
	
	// Create a new SocketTransport for receiving (=server)
	public TomcatWSTransport()
	{
		this.socket = new WSSocket(this);
		this.transportPriority = 0;
		this.state = TrapTransportState.CONNECTING;
	}
	
	public String getTransportName()
	{
		return "websocket";
	}
	
	public WSSocket getSocket()
	{
		return this.socket;
	}
	
	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.WEBSOCKET;
	}
	
	@Override
	public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
	{
		try
		{
			WSSocket mSock = this.socket;
			
			if (mSock == null)
				throw new TrapTransportException(message, this.getState());
			
			synchronized (mSock)
			{
				
				byte[] raw = message.serialize();
				
				this.delayed |= this.lastSend == System.currentTimeMillis();
				this.delayed &= this.useDelay;
				if (this.delayed && !this.delayQueued)
				{
					this.delayQueued = true;
					ThreadPool.executeAfter(new Runnable() {
						
						@Override
						public void run()
						{
							TomcatWSTransport.this.flushTransport();
						}
					}, 1);
				}
				
				if (expectMore)
				{
					
					if (this.outBuf == null)
						this.outBuf = new ByteArrayOutputStream();
					
					this.outBuf.write(raw);
					return;
				}
				
				this.performSend(raw);
				
			}
		}
		catch (IOException e)
		{
			this.logger.debug(e.toString());
			this.setState(TrapTransportState.ERROR);
			throw new TrapTransportException(message, this.state);
		}
		catch (TrapTransportException e)
		{
			this.setState(TrapTransportState.ERROR);
			throw e;
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
	
	private void performSend(byte[] raw) throws IOException
	{
		WSSocket mSock = this.socket;
		if (this.outBuf != null)
		{
			this.outBuf.write(raw);
			raw = this.outBuf.toByteArray();
			this.outBuf = null;
		}
		//char[] encoded = Base64.encode(raw);
		
		mSock.send(ByteBuffer.wrap(raw));
		
		this.lastSend = System.currentTimeMillis();
		this.delayed = this.delayQueued = false;
	}
	
	@Override
	protected synchronized void internalDisconnect()
	{
		synchronized (this.socket)
		{
			//this.socket.close();
		}
		this.setState(TrapTransportState.DISCONNECTING);
		
	}
	
	public void notifyError()
	{
		System.out.println("Error!");
		this.setState(TrapTransportState.ERROR);
	}
	
	public void notifyOpen()
	{
		this.setState(TrapTransportState.CONNECTED);
	}
	
	public void notifyClose()
	{
		
		if (this.getState() == TrapTransportState.DISCONNECTED || this.getState() == TrapTransportState.ERROR)
			return;
		
		this.setState(TrapTransportState.DISCONNECTED);
		this.socket = null;
		
	}
	
	public void notifyMessage(String string)
	{
		//byte[] decoded = Base64.decode(string);
		
		// Disable binary mode, to prevent us from confusing the browser
		this.binary = false;
		
		byte[] decoded = StringUtil.toUtfBytes(string);
		this.receive(decoded, 0, decoded.length);
	}
	
	public void notifyMessage(byte[] data)
	{
		// Ensure binary mode is activated for correct response
		this.binary = true;
		this.receive(data, 0, data.length);
	}
	
	protected void updateConfig()
	{
		
		String eString = this.getOption(OPTION_BINARY);
		if (eString != null)
		{
			try
			{
				this.binary = Boolean.parseBoolean(eString);
			}
			catch (Exception e)
			{
				this.logger.warn("Failed to parse transport {} binary flag", this.getTransportName(), e);
			}
		}
	}
	
	public void finalize()
	{
	}
	
	@Override
	protected void internalConnect() throws TrapException
	{
		
	}

	@Override
	public void flushTransport()
	{
		synchronized (TomcatWSTransport.this.socket)
		{
			try
			{
				if (this.outBuf == null)
					return;
				
				byte[] raw = TomcatWSTransport.this.outBuf.toByteArray();
				TomcatWSTransport.this.outBuf = null;
				TomcatWSTransport.this.performSend(raw);
			}
			catch (IOException e)
			{
				TomcatWSTransport.this.logger.debug(e.toString());
				TomcatWSTransport.this.forceError();
			}
		}
	}
	
}
