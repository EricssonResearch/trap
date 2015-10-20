package com.ericsson.research.trap.impl;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.HexConverter;
import com.ericsson.research.trap.utils.StringUtil;
import com.ericsson.research.trap.utils.ThreadPool;

/**
 * The server trap endpoint does not really need to care about a lot of
 * things... So let's not have it do so
 * 
 * @author Vladimir Katardjiev
 */
public class ServerTrapEndpoint extends TrapEndpointImpl
{
	
	protected ListenerTrapEndpoint	listenerTrapEndpoint;
	
	public ServerTrapEndpoint(ListenerTrapEndpoint listenerTrapEndpoint) throws TrapException
	{
		super();
		this.listenerTrapEndpoint = listenerTrapEndpoint;
		// ServerTrapEndpoint must be able to auto-open
		this.setState(TrapState.OPENING);
		
		/*
		 * Server should default to not limit MAT. If client wants a limit, that's fine. If user adds a limit, that's fine too. But default should be max.
		 */
		this.maxActiveTransports = Integer.MAX_VALUE;
	}
	
	protected TrapMessage createOnOpenedMessage(TrapMessage openMessage)
	{
		TrapMessage onopened = super.createOnOpenedMessage(openMessage);
		TrapConfigurationImpl body = new TrapConfigurationImpl(StringUtil.toUtfString(openMessage.getData()));
		
		// Disable compression if the client doesn't advertise the feature.
		boolean compression = body.getBooleanOption(TrapEndpoint.OPTION_ENABLE_COMPRESSION, false);
		this.config.setOption(TrapEndpoint.OPTION_ENABLE_COMPRESSION, Boolean.toString(compression));
		this.compressionEnabled = compression;
		
		String configHash = body.getOption(TrapConfigurationImpl.CONFIG_HASH_PROPERTY);
		if (configHash == null)
		{
			this.logger.trace("Client did not request updated configuration...");
			return onopened;
		}
		
		String autoHost = body.getOption(TrapEndpoint.OPTION_AUTO_HOSTNAME);

		byte[] clientConfiguration = StringUtil.toUtfBytes(this.listenerTrapEndpoint.getClientConfiguration(autoHost));
		
		if (clientConfiguration.length == 0)
		{
			this.logger.debug("Automatic configuration update disabled; at least one transport did not have a non-zero IP number configured");
		}

		try
		{
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(clientConfiguration);
			byte[] clientConfigArr = HexConverter.toByteArray(configHash);
			if (Arrays.equals(digest.digest(), clientConfigArr))
			{
				this.logger.debug("Client configuration was up to date");
				return onopened;
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			this.logger.warn("Could not check client configuration hash", e);
		}
		this.logger.debug("Sending updated configuration to the client");
		onopened.setData(clientConfiguration);
		return onopened;
	}
	
	/*
	 * This method should try to wakeup the client if we have wakeup mechanisms available.
	 *  Once that is done, it should wait until the timeout expires and our state is OPEN
	 * (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapEndpointImpl#reconnect(long)
	 */

	protected void reconnect(long timeout)
	{
		
		// Asked server to reconnect is an illegal operation so far. In future versions of Trap this should attempt to wakeup if possible.
		this.setState(TrapState.CLOSED);
		/*
		// TODO: Ask for wakeup usage.
		
		// Jettison all current transports.
		synchronized (transports)
		{
			for (int i = 0; i < transports.size(); i++)
				((TrapTransport) transports.get(i)).disconnect();
		}
		
		// After having jettisonned all transports, create new data structs for them
		transportsMap = new HashMap();
		transports = new LinkedList();
		availableTransports = new LinkedList();

		long endTime = System.currentTimeMillis() + timeout;
		
		try
		{
			while (this.getState() == TrapState.SLEEPING)
			{
				synchronized (this)
				{
					long waitTime = endTime - System.currentTimeMillis();
					if (waitTime <= 0)
						break;
					this.wait(waitTime);
				}
			}
		}
		catch (InterruptedException e)
		{
			logger.warn(e.getMessage(), e);
		}
		
		// Error cleanup.. If, at this point, we STILL do not have a connection, we must DIE.
		if (this.getState() != TrapState.OPEN)
			this.setState(TrapState.CLOSED);
		
		*/
		
	}
	
	public synchronized void ttStateChanged(TrapTransportState newState, TrapTransportState oldState, TrapTransport transport, Object context)
	{
		super.ttStateChanged(newState, oldState, transport, context);
		
		if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
		{
			// What happened?
			if ((this.getState() == TrapState.CLOSING) || (this.getState() == TrapState.CLOSED))
			{
				// IT's all good
			}
			else
			{
				// It's not good. The transport has disconnected. We should allow for a limited time during which at least one transport should be available.
				if (this.availableTransports.size() == 0)
				{
					this.setState(TrapState.SLEEPING);
					// Create a task that will kill this endpoint in due time.
					ThreadPool.executeAfter(new Runnable() {
						
						public void run()
						{
							if ((ServerTrapEndpoint.this.getState() == TrapState.SLEEPING))
							{
								Iterator<TrapTransport> it = ServerTrapEndpoint.this.transports.iterator();
								
								while (it.hasNext())
									it.next().disconnect();
								
								ServerTrapEndpoint.this.setState(TrapState.CLOSED);
							}
						}
					}, this.reconnectTimeout);
				}
			}
		}
	}
	
	//TODO: Add integration test for isAlive(timeout)
	public Callback<Boolean> isAlive(long timeout)
	{
		// TODO: Change this once reconnect doesn't spaz out on me
		return this.isAlive(timeout, true, false, timeout);
	}
	
}
