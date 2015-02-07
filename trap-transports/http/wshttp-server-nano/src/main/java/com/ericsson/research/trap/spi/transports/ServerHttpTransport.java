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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.nhttpd.IHTTPSession;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.Response.Status;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.spi.nhttp.CORSUtil;
import com.ericsson.research.trap.spi.nhttp.FullRequestHandler;
import com.ericsson.research.trap.utils.ThreadPool;
import com.ericsson.research.trap.utils.UID;

public class ServerHttpTransport extends AbstractTransport implements Runnable, FullRequestHandler
{

	long	                            expirationDelay	      = 28000;	                                // Almost
																										// 30
																										// seconds
	long	                            reregistrationTimeout	= 10 * 1000;	                        // Ten
																										// seconds
																										// reregistration
																										// timeout
	long	                            disconnectTime	      = Long.MAX_VALUE;	                    // When
																										// this
																										// transport
																										// will
																										// be
																										// killed
																										// for
																										// inactivity
	LinkedBlockingQueue<TrapMessage>	messagesToSend	      = new LinkedBlockingQueue<TrapMessage>();
	private TrapTransportState	        oldState;

	boolean	                            send	              = false;
	private final ListenerHttpTransport	parent;
	private final String	            path;
	private Logger	                    jLogger;

	public ServerHttpTransport()
	{
		this.parent = null;
		this.path = "";
	}

	public ServerHttpTransport(ListenerHttpTransport parent)
	{
		this.jLogger = Logger.getLogger("com.sun.net.httpserver");
		this.jLogger.setLevel(Level.WARNING);
		this.parent = parent;
		this.path = "/" + UUID.randomUUID().toString().replace("-", "");
		ThreadPool.executeAfter(this, this.expirationDelay);
		this.state = TrapTransportState.CONNECTING;
		this.transportPriority = 1100;
	}

	@Override
	public String getTransportName()
	{
		return "http";
	}

	@Override
	public String getProtocolName()
	{
		return TrapTransportProtocol.HTTP;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fillAuthenticationKeys(@SuppressWarnings("rawtypes") HashSet keys)
	{
		super.fillAuthenticationKeys(keys);
	}

	@Override
	public void fillContext(Map<String, Object> context, Collection<String> filter)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void updateConfig()
	{
		super.updateConfig();

		String newReregistrationTimeout = this.getOption("reregistrationTimeout");
		try
		{
			this.reregistrationTimeout = Long.parseLong(newReregistrationTimeout);
		}
		catch (Exception e)
		{
		}

		String newexpirationDelay = this.getOption("expirationDelay");
		try
		{
			this.expirationDelay = Long.parseLong(newexpirationDelay);
		}
		catch (Exception e)
		{
		}
	}

	@Override
	public synchronized void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
	{

		if ((this.getState() != TrapTransportState.AVAILABLE) && (message.getMessageId() != 0))
			throw new TrapTransportException(message, this.getState());

		if (this.logger.isTraceEnabled())
			this.logger.trace("Queueing message id {} to send with HTTP; got more is [{}]", message.getMessageId(), expectMore);

		this.messagesToSend.add(message);

		if (expectMore)
			return;

		this.flushTransport();
	}

	@Override
	protected void internalConnect() throws TrapException
	{
		throw new TrapException("Cannot connect a ServerTransport");
	}

	@Override
	protected void internalDisconnect()
	{
		this.setState(TrapTransportState.DISCONNECTED);
		flushTransport();
	}

	byte[]	             readBuf	= new byte[4096];
	private Response	 response;
	private IHTTPSession	request;

	/*
	 * HTTP transport will only switch to available when a GET is present. Thus,
	 * we'll await the inevitable conclusion.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ericsson.research.trap.spi.transports.AbstractTransport#onOpened(
	 * com.ericsson.research.trap.spi.TrapMessage)
	 */

	@Override
	protected boolean onOpened(TrapMessage message)
	{
		boolean authed = this.checkAuthentication(message);
		if (authed && this.getState() == TrapTransportState.CONNECTING)
			this.setState(TrapTransportState.CONNECTED);
		return authed;
	}

	@Override
	protected boolean onOpen(TrapMessage message)
	{
		boolean authed = this.checkAuthentication(message);
		if (authed && this.getState() == TrapTransportState.CONNECTING)
			this.setState(TrapTransportState.CONNECTED);
		return authed;
	}

	public void handle(IHTTPSession request, Response response)
	{
		try
		{
			response.setAsync(true);
			String method = request.getMethod().toString();
			this.lastAlive = System.currentTimeMillis();

			this.logger.trace("HTTP Handling new message {}{}", method, request.getUri());
			
			synchronized (this)
			{
				if ((this.getState() == TrapTransportState.DISCONNECTED) || (this.getState() == TrapTransportState.DISCONNECTING))
				{

					// permit POST requests anyway. This way we won't drop data
					// from the clients.
					// There may be a situation where the client is still
					// uploading a data packet while
					// the transport disconnects. Allow it to be valid (as long
					// as it can be authed with a higher layer)
					if ("GET".equals(method) && !this.messagesToSend.isEmpty())
					{
						this.logger.trace("HTTP performing flush of excessive messages", method);

						// Flush response headers, giving XHR a proper handle on
						// things
						CORSUtil.setCors(request, response);
						response.setStatus(Status.CREATED);

						try
						{
							// Serialize all messages
							ByteArrayOutputStream bos = new ByteArrayOutputStream();

							for (TrapMessage m : this.messagesToSend)
							{
								if (this.logger.isTraceEnabled())
									this.logger.trace("Flushing message with id {}", m.getMessageId());

								bos.write(m.serialize());
							}

							byte[] body = bos.toByteArray();

							response.setData(new ByteArrayInputStream(body));
							request.finish(response);

							this.messagesToSend.clear();
							return;
						}
						catch (Exception e)
						{
							e.printStackTrace();
							this.messagesToSend.clear();
							request.finish(response);
							// No reason here.
							return;
						}
						finally
						{
						}
					}
					else if (!"POST".equals(method))
					{

						CORSUtil.setCors(request, response);
						response.setStatus(Status.OK);
						request.finish(response);
						return;

					}
				}
			}

			if ("POST".equals(method))
			{
				try
				{
					// Shove the body up
					InputStream is = request.getInputStream();

					int received = 0;
					int read = 0;
					while ((read = is.read(this.readBuf)) > -1)
					{
						received++;
						super.receive(this.readBuf, 0, read);
					}

					if (received == 0)
					{
						synchronized (this)
						{
							// HACK: Disconnect
							this.setState(TrapTransportState.DISCONNECTING);
							this.setState(TrapTransportState.DISCONNECTED);

							// Release longpoll (the client won't)
							flushTransport();
						}

						CORSUtil.setCors(request, response);

						response.setStatus(Status.OK);
						request.finish(response);
						return;
					}

					CORSUtil.setCors(request, response);

					response.setStatus(Status.ACCEPTED);
					request.finish(response);
				}
				catch (Exception e)
				{
					this.logger.debug("Exception while sending a POST response: ", e);
					e.printStackTrace();
					request.finish(response);
				}

			}
			else if ("GET".equals(method))
			{
				Map<String, String> params = request.getParms();
				String expires = params.get("expires");

				if (expires != null)
					this.expirationDelay = Long.parseLong(expires);

				// Hang this request for up to 30 seconds.
				long endTime = System.currentTimeMillis() + this.expirationDelay;
				this.disconnectTime = endTime + this.reregistrationTimeout;

				// Flush response headers, giving XHR a proper handle on things
				CORSUtil.setCors(request, response);
				response.setStatus(Status.CREATED);

				synchronized (this)
				{

					if ((this.getState() == TrapTransportState.AVAILABLE))
					{
						this.logger.warn("Invalid HTTP state: Labelled as AVAILABLE when already have a longpoll running");
						this.wait(1000);
						request.finish(response);
						return;
					}

					// TODO: Save request!!!

					if ((this.getState() == TrapTransportState.DISCONNECTED) || (this.getState() == TrapTransportState.DISCONNECTING)
					        || (this.getState() == TrapTransportState.ERROR))
					{
						request.finish(response);
						return;
					}

					if (this.getState() == TrapTransportState.UNAVAILABLE)
					{
						if (this.oldState == TrapTransportState.CONNECTED)
							this.setState(TrapTransportState.AVAILABLE);
						else
							this.setState(this.oldState);
					}
					else if (this.getState() == TrapTransportState.CONNECTED)
						this.setState(TrapTransportState.AVAILABLE);

					this.request = request;
					this.response = response;
					ThreadPool.executeAfter(new Runnable()
					{
						
						@Override
						public void run()
						{
							flushTransport();
						}
					}, expirationDelay);
				}

			}
			else if ("OPTIONS".equals(method))
			{
				CORSUtil.setCors(request, response);
				response.setStatus(Status.OK);
				request.finish(response);
			}
			else if ("PUT".equals(method))
			{

				// We need to make the full loop to not trigger a reconnect
				// method
				this.setState(TrapTransportState.DISCONNECTING);
				this.setState(TrapTransportState.DISCONNECTED);

				CORSUtil.setCors(request, response);
				response.setStatus(Status.OK);
				request.finish(response);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			request.finish(response);
		}
	}

	@Override
	protected void setState(TrapTransportState newState)
	{
		super.setState(newState);

		if ((newState == TrapTransportState.DISCONNECTED) || (newState == TrapTransportState.ERROR))
		{
			this.parent.unregister(this);
		}
	}

	public String getPath()
	{
		return this.path;
	}

	@Override
	public void run()
	{

		if ((this.getState() == TrapTransportState.DISCONNECTED) || (this.getState() == TrapTransportState.DISCONNECTING)
		        || (this.getState() == TrapTransportState.ERROR))
			return;

		if (System.currentTimeMillis() > this.disconnectTime)
		{
			// Expire the connection. Brutal disconnect
			this.setState(TrapTransportState.DISCONNECTED);
			flushTransport();
		}
		else
		{
			// Reschedule us to connect
			ThreadPool.executeAfter(this, this.expirationDelay);
		}
	}

	public static Map<String, String> getUriParameters(URI uri) throws UnsupportedEncodingException
	{

		String query = uri.getQuery();
		Map<String, String> params = new HashMap<String, String>();
		if (query != null)
		{
			for (String param : query.split("&"))
			{
				String pair[] = param.split("=");
				String key = URLDecoder.decode(pair[0], "UTF-8");
				String value = "";
				if (pair.length > 1)
				{
					value = URLDecoder.decode(pair[1], "UTF-8");
				}
				params.put(key, value);
			}
		}
		return params;
	}

	protected void acknowledgeTransitMessage(TrapMessage message)
	{
	}

	@Override
	public synchronized void flushTransport()
	{
		
		if (request == null || response == null)
			return;
		
		// kick the sending thread
		if (this.getState() == TrapTransportState.AVAILABLE)
		{
			this.oldState = TrapTransportState.AVAILABLE;
			this.setState(TrapTransportState.UNAVAILABLE);
		}

		try
		{
			// Serialize all messages
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			for (TrapMessage m : this.messagesToSend)
			{
				if (this.logger.isTraceEnabled())
					this.logger.trace("Flushing message with id {}", new Long(m.getMessageId()));

				bos.write(m.serialize());
			}

			byte[] body = bos.toByteArray();

			response.setData(new ByteArrayInputStream(body));

			this.messagesToSend.clear();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LinkedList<TrapMessage> failedMessages = new LinkedList<TrapMessage>();

			for (TrapMessage m : this.messagesToSend)
				if (m.getMessageId() != 0)
					failedMessages.add(m);

			this.delegate.ttMessagesFailedSending(failedMessages, this, this.delegateContext);
		}
		finally
		{

			// Also flip to unavailable when this GET expires...
			if ((this.getState() == TrapTransportState.CONNECTED) || (this.getState() == TrapTransportState.AVAILABLE))
			{
				this.oldState = this.getState();
				this.setState(TrapTransportState.UNAVAILABLE);
			}

			request.finish(response);
			response = null;
			request = null;
			this.logger.trace("Longpoll ended...");
		}
	}

}
