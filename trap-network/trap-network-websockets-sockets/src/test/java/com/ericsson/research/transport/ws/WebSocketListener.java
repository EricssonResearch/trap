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

import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;

public class WebSocketListener implements WSListener
{
	
	protected WSInterface socket;
	private boolean open = false;
	private boolean opened = false;
	private String string;
	private byte[] data;
	
	public synchronized void notifyClose()
	{
		//System.out.println("Socket " + socket + " did close.");
		this.open = false;
		this.notifyAll();
	}
	
	public synchronized byte[] waitForBytes(long timeout) throws Exception
	{
		try
		{
			long expiry = System.currentTimeMillis() + timeout;
			while(this.data == null)
			{
				long waitTime = expiry - System.currentTimeMillis();
				if (waitTime <= 0)
					throw new Exception("Timeout");
				
				try
				{
					this.wait(waitTime);
				}
				catch (InterruptedException e)
				{
				}
			}
			
			return this.data;
		}
		finally
		{
			this.data = null;
		}
	}
	
	public synchronized void waitForClose(long timeout) throws Exception
	{
		long expiry = System.currentTimeMillis() + timeout;
		while(open)
		{
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0)
				throw new Exception("Timeout");
			
			try
			{
				this.wait(waitTime);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	public synchronized void notifyMessage(byte[] data)
	{
		//System.out.println("Socket " + socket + " received data (" + data.length + " bytes).");
		this.data = data;
		this.notify();
		
	}
	
	public void notifyOpen(WSInterface socket)
	{
		synchronized(this)
		{
			this.socket = socket;
			//System.out.println("Socket " + socket + " opened successfully.");
			this.open  = true;
			this.opened = true;
			this.notifyAll();
		}
	}
	
	public void waitForOpen(long timeout) throws Exception
	{
		synchronized(this)
		{
			
			long expiry = System.currentTimeMillis() + timeout;
			while(!open)
			{
				long waitTime = expiry - System.currentTimeMillis();
				if (waitTime <= 0 && !open)
					throw new Exception("Timeout");
				
				try
				{
					this.wait(100);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		//System.out.println("No longer waiting...");
	}
	
	public synchronized void notifyMessage(String string)
	{
		//System.out.println("Socket " + socket + " received string: " + string);
		this.string = string;
		this.notify();
	}
	
	public synchronized String waitForString(long timeout) throws Exception
	{
		try
		{
			long expiry = System.currentTimeMillis() + timeout;
			while(this.string == null)
			{
				long waitTime = expiry - System.currentTimeMillis();
				//System.out.println(waitTime);
				if (waitTime <= 0)
					throw new Exception("Timeout");
				
				try
				{
					this.wait(waitTime);
				}
				catch (InterruptedException e)
				{
				}
			}
			
			return this.string;
		}
		finally
		{
			this.string = null;
		}
	}
	
	public void printDebugInfo()
	{
		System.out.println("Open: " + open + "(was opened: " + opened + ")");
		System.out.println("String: " + string);
		System.out.println("Data: " + data);
		
	}

	public void notifyPong(byte[] payload) {
	}

	public void notifyError(Throwable t) {
		t.printStackTrace();
	}
	
}
