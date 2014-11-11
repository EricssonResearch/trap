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

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.ThreadPool;

public class AsynchronousLoopbackTransport extends LoopbackTransport implements Runnable
{
	public static final String					name		= "asyncloopback";

	private final Object						sendLock	= new Object();
	protected LinkedBlockingQueue<TrapMessage>	buf			= new LinkedBlockingQueue<TrapMessage>(1000);

	private boolean								sending		= false;

	public AsynchronousLoopbackTransport()
	{
		super();
		this.enabled = true; // Start with disabled mode. Config can re-enable. Prevents us from disturbing other tests.
		this.transportPriority = -1100;
	}

	@Override
	public String getTransportName()
	{
		return name;
	}

	@Override
	public synchronized void internalConnect() throws TrapException
	{
		super.internalConnect();
	}

	@Override
	public void internalSend(final TrapMessage message, boolean expectMore) throws TrapTransportException
	{

		// Oddly enough, this throttle shows a 5% performance GAIN on the performance tests
		// This is confirmed over 200 million messages.
		// It probably is a GC-related gain.

		try
		{
			this.buf.put(message);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		if (!this.sending)
			synchronized (this.sendLock)
			{
				if (!this.sending)
				{
					ThreadPool.executeFixed(new Runnable() {

						@Override
						public void run()
						{
							try
							{

								synchronized (AsynchronousLoopbackTransport.this)
								{
									if (AsynchronousLoopbackTransport.this.sending)
										return;
									AsynchronousLoopbackTransport.this.sending = true;
								}

								LoopbackTransport t = AsynchronousLoopbackTransport.this.remote.get();

								if (t == null)
								{
									AsynchronousLoopbackTransport.this.setState(TrapTransportState.ERROR);
									return;
								}

								for (;;)
								{
									TrapMessage m = AsynchronousLoopbackTransport.this.buf.poll();

									if (m != null)
									{
										t._receive(m);
									}
									else
									{
										Thread.yield();

										synchronized (AsynchronousLoopbackTransport.this.sendLock)
										{
											if (AsynchronousLoopbackTransport.this.buf.peek() == null)
											{
												AsynchronousLoopbackTransport.this.sending = false;
												return;
											}
										}
									}

								}
							}
							catch (Exception e)
							{
								AsynchronousLoopbackTransport.this.logger.warn("Exception occurred: {}", e);
							}
							finally
							{
								AsynchronousLoopbackTransport.this.sending = false;
							}
						}
					});
				}
			}

	}

	@Override
	protected void _receive(TrapMessage message)
	{
		super._receive(message);
	}

	protected LoopbackTransport _connect(LoopbackTransport remote)
	{
		LoopbackTransport local = new AsynchronousLoopbackTransport();
		local.remote = new WeakReference<LoopbackTransport>(remote);

		// Optional: configure

		// Now notify
		this.sListener.ttsIncomingConnection(local, this, this.sContext);
		local.setState(TrapTransportState.CONNECTED);

		// Now allow the remote party to benefit.
		return local;
	}

	public synchronized void run()
	{
		this.setState(TrapTransportState.AVAILABLE);
	}
}
