//< needs(compat)

Trap.Endpoint = function()
{
	Trap.EventObject.constructor.call(this);
	this.transportsMap = new Trap.Map();
	this.transports = new Trap.List();
	this.config = new Trap.Configuration();
	
	this.availableTransports = new Trap.List();
	this._state = Trap.Endpoint.State.CLOSED;
	
	this.channels = new Array();
	this.messageQueue = new Trap.ChannelMessageQueue();
	
	this.messageQueueType = Trap.Endpoint.Queue.REGULAR;
	
	this._maxActiveTransports = 65535;
	
	this.sending = false;
	
	this.trapID = Trap.Constants.ENDPOINT_ID_UNDEFINED;
	this.trapFormat = (this.useBinary ? Trap.Message.Format.REGULAR : Trap.Message.Format.SEVEN_BIT_SAFE);
	
	this._authentication = new Trap.Authentication();
	this.logger = Trap.Logger.getLogger("TrapEndpoint"); // The number of
	// milliseconds
	// async mode is
	// allowed to wait
	// for messages (to
	// reorder)
	
	// Timeouts & Keepalives.
	/**
	 * Last known activity of the connection. Activity is defined as any form of
	 * message FROM the client. In general, the TrapEndpoint will not concern
	 * itself with ensuring this.value is continually updated, as that is mostly
	 * unnecessary overhead. It will update it during the following conditions:
	 * <p>
	 * <ul>
	 * <li>A transport disconnects. Even in this.case, the lastActivity field
	 * will only represent some most recent communication with the remote side,
	 * unless all transports have disconnected.
	 * <li>The application specifically queries. In this.case, the TrapEndpoint
	 * will specifically ensure that lastActivity has the most recent value.
	 * </ul>>
	 */
	this._lastAlive = 0;
	
	/**
	 * The last known timestamp where we can reliably wake up the underlying
	 * transports. If we have a wakeup mechanism, this.will be a non-negative
	 * value, and represents when we can unilaterally tell the application the
	 * connection is permanently dead (unless we can extend the wakeup
	 * mechanism).
	 */
	this.canWakeupUntil = 0;
	
	/**
	 * The last permitted timestamp for the client to re-establish connectivity.
	 * this.must be equal to or greater than canWakeupUntil, in order to
	 * maintain the same promise to the application.
	 */
	this.canReconnectUntil = 0;
	
	/**
	 * The number of milliseconds that the endpoint should wait for a response
	 * (and/or attempt to reconnect/resend) to do an orderly close. After this
	 * time, the transport will simply deallocate all of its resources and
	 * vanish.
	 */
	this.keepaliveExpiry = 5000;
	this.keepaliveInterval = Trap.Keepalive.Policy.DEFAULT;
	this.keepaliveTask = null;
	
	this.reconnectTimeout = 180000;
	this.async = true;
	this.compressionEnabled = this.useBinary;
	
	Trap._compat.__defineGetter(this, "state", function()
	{
		return this._state;
	});
	
	Trap._compat.__defineGetter(this, "queueType", function()
	{
		return this.messageQueueType;
	});
	
	Trap._compat.__defineSetter(this, "queueType", function(t)
	{
		this.messageQueueType = t;
	});
	
	Trap._compat.__defineGetter(this, "maxActiveTransports", function()
	{
		return this._maxActiveTransports;
	});
	
	Trap._compat.__defineSetter(this, "maxActiveTransports", function(l)
	{
		this._maxActiveTransports = l;
	});
	
	Trap._compat.__defineGetter(this, "authentication", function()
	{
		return this._authentication;
	});
	
	Trap._compat.__defineSetter(this, "authentication", function(a)
	{
		this._authentication = a;
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).setAuthentication(a);
	});
	
	Trap._compat.__defineGetter(this, "format", function()
	{
		return this.trapFormat;
	});
	
	Trap._compat.__defineSetter(this, "format", function(f)
	{
		this.trapFormat = f;
		
		var it = this.transports.iterator();
		
		while (it.hasNext())
			it.next().setFormat(f);
	});
	
	if (this.useBinary)
	{
		
		if (Trap._useGetters)
		{
			this._dispatchMessageEvent = function(message)
			{
				var evt = new Trap._GetterMessageEvent(message);
				this._dispatchEvent(evt);
			};
		}
		else
		{
			// Fallback approach
			this._dispatchMessageEvent = function(message)
			{
				var evt = {
					type : "message",
					message : message.getData(),
					// data: message.getData(),
					dataAsString : message.getString(),
					// string: message.getString(),
					buffer : message.getData().buffer.slice(message.data.byteOffset, message.data.byteOffset + message.data.byteLength),
					compression: message.getCompression(),
					channel: message.getChannel(),
					object: JSON.parse(message.getString())
				};
				
				// Remove redundant calls to message to increase performance.
				evt.data = evt.message;
				evt.string = evt.dataAsString;
				
				this._dispatchEvent(evt);
			};
		}
	}
	else
	{
		this._dispatchMessageEvent = function(message)
		{
			var evt = {
				type : "message",
				message : message.getString(),
				channel: message.getChannel()
			// data: message.getString(),
			// dataAsString: message.getString(),
			// string: message.getString()
			};
			
			evt.data = evt.message;
			evt.dataAsString = evt.data;
			evt.string = evt.data;
			
			this._dispatchEvent(evt);
		};
	}
	
	this.channels[0] = new Trap.Channel(this, 0);
	this.channels[0].setPriority(Number.MAX_VALUE);
	
	this.messageQueue.rebuild(this.channels);
	
};
Trap.Endpoint.prototype = new Trap.EventObject;
Trap.Endpoint.prototype.constructor = Trap.Endpoint;

Trap.Endpoint.State = {
	CLOSED : "Trap.Endpoint.State.CLOSED",
	OPENING : "Trap.Endpoint.State.OPENING",
	OPEN : "Trap.Endpoint.State.OPEN",
	SLEEPING : "Trap.Endpoint.State.SLEEPING",
	ERROR : "Trap.Endpoint.State.ERROR",
	CLOSING : "Trap.Endpoint.State.CLOSING"
};

Trap.Endpoint.Queue = {
	REGULAR : "Trap.Endpoint.Queue.REGULAR"
};

/* Settings methods */

Trap.Endpoint.prototype.enableTransport = function(transportName)
{
	if (this.isTransportEnabled(transportName)) return;
	
	this.getTransport(transportName).enable();
};

Trap.Endpoint.prototype.disableTransport = function(transportName)
{
	if (!this.isTransportEnabled(transportName)) return;
	
	this.getTransport(transportName).disable();
};

Trap.Endpoint.prototype.disableAllTransports = function()
{
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).disable();
};

Trap.Endpoint.prototype.isTransportEnabled = function(transportName)
{
	try
	{
		return this.getTransport(transportName).isEnabled();
	}
	catch (e)
	{
		return false;
	}
};

Trap.Endpoint.prototype.getConfiguration = function()
{
	return this.config.toString();
};

Trap.Endpoint.prototype.parseConfiguration = function(configuration)
{
	return new Trap.Configuration(configuration);
};

Trap.Endpoint.prototype.configure = function(configuration)
{
	this.config = this.parseConfiguration(configuration);
	
	// Iterate over all transports
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setConfiguration(this.config);
	
	var option = this.config.getIntOption("trap.keepalive.interval", this.keepaliveInterval);
	this.setKeepaliveInterval(option);
	
	option = this.config.getIntOption("trap.keepalive.expiry", this.keepaliveExpiry);
	this.setKeepaliveExpiry(option);
	
	this.compressionEnabled = this.config.getBooleanOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, this.compressionEnabled);
};

Trap.Endpoint.prototype.configureTransport = function(transportName, configurationKey, configurationValue)
{
	this.getTransport(transportName).configure(configurationKey, configurationValue);
};

Trap.Endpoint.prototype.getTransports = function()
{
	return this.transports;
};

Trap.Endpoint.prototype.getTransport = function(transportName)
{
	
	var t = this.transportsMap.get(transportName);
	
	if (t == null) throw "Unknown Transport";
	
	return t;
};

Trap.Endpoint.prototype.addTransport = function(t, message)
{
	
	if (!t.canConnect() && !t.canListen() && t.getState() == Trap.Transport.State.DISCONNECTED)
	{
		this.logger.debug("Attempting to add transport class [{}] for handler [{}] that can neither connect nor listen. Skipping...", t, t.getTransportName());
		return false;
	}
	
	var old = this.transportsMap.get(t.getTransportName());
	
	if (old != null)
	{

		var oldPrio = old.getTransportPriority();
		var newPrio = t.getTransportPriority();
		
		// Strict lesser equality. This allows us to be replaced by, well, the same transport.
		if (oldPrio < newPrio)
		{
			this.logger.debug("Attempting to add new handler for [{}] when the old handler had a higher priority. New class was [{}]/{}, old class was [{}]{}. Skipping...", t.getTransportName(), t.getClass().getName(), t.getTransportPriority(), old.getClass().getName(), old.getTransportPriority() );
			return false;
		}
		
		this.transports.remove(old);
	}

	this.transportsMap.put(t.getTransportName(), t);
	this.transports.add(t);
	t.setFormat(this.getFormat());
	
	// Hook the delegate methods
	var mt = this;
	t.onmessage = function(e)
	{
		mt.ttMessageReceived(e.message, t, null);
	};
	t.onmessagesent = function(e)
	{
		mt.ttMessageSent(e.message, t, null);
	};
	t.onstatechange = function(e)
	{
		mt.ttStateChanged(e.newState, e.oldState, t, null);
	};
	t.onfailedsending = function(e)
	{
		mt.ttMessagesFailedSending(e.messages, t, null);
	};
	
	// See public synchronized void addTransport(TrapTransport t, TrapMessage
	// message)
	// Used to add a transport to a listener.
	if (message)
	{
		
		t.setConfiguration(this.config);
		t.setAuthentication(this.authentication);
		t.setFormat(this.getFormat());

		var l = new Trap.List(this.availableTransports);
		var aliveCB =  function(i) {
			if (i >= l.size())
				return;
			
			var t = l.get(i);
			t.isAlive(0, true, true, 4000, function(rv) {
				if (!rv)
					t.forceError();
				else if (t.getState() == Trap.Transport.State.AVAILABLE)
					this.addTransportToAvailable(t);
			});
			
		};
		
		if (t.getState() == Trap.Transport.State.AVAILABLE)
		{
			// This, in general, means we get a second transport on an existing session. We should re-check the liveness of the existing transports, in case
			// this is a disconnect
			
			// We should temporarily clear the available transports.
			this.availableTransports.clear();
			aliveCB(0);
			this.addTransportToAvailable(t);
		}
		else
		{
			// The second case is trickier. We get a new unavailable transport (=sporadic availability). We can't make any assumptions
			// but it is nevertheless wise to check the available transports.
			aliveCB(0);
		}
		
		// Trigger incoming message (=OPEN) in order to reply properly.
		this.ttMessageReceived(message, t, null);
	}
};

Trap.Endpoint.prototype.setTrapID = function(newId)
{
	this.trapID = newId;
};

Trap.Endpoint.prototype.getTrapID = function()
{
	return this.trapID;
};

Trap.Endpoint.prototype.removeTransport = function(t)
{
	this.transportsMap.remove(t.getTransportName());
	this.transports.remove(t);
};

/**
 * Closes this.Trap endpoint, terminating any outstanding Trap transports.
 */
Trap.Endpoint.prototype.close = function()
{
	if (this.getState() != Trap.Endpoint.State.OPEN)
	{
		// We can't close a non-open connection.
		
		if (this.getState() == Trap.Endpoint.State.SLEEPING)
		{
			// TODO: We should WAKE UP then DISCONNECT.
			// Since SLEEPING is NYI, we'll leave this
			this.setState(Trap.Endpoint.State.CLOSING);
			this.onEnd(null, null);
		}
		else
		{
			if (this.getState() == Trap.Endpoint.State.CLOSING || this.getState() == Trap.Endpoint.State.CLOSED)
			{
				// Harmless call.
				return;
			}
			
			if (this.getState() == Trap.Endpoint.State.ERROR)
			{
				// Technically harmless call, but we will log it to point out
				// potential laziness in the coding of the error handling of our
				// parent.
				this.logger.debug("Called close() on an endpoint in state ERROR. This might be caused by recovery code shared between regular and normal states");
				return;
			}
			
			if (this.getState() == Trap.Endpoint.State.OPENING)
			{
				// TODO: This one is troublesome. close() has been called on a
				// connection that is opening().
				// I think we can handle it normally (i.e. switch to closing and
				// just end()) but it might be worth investigating
				// We will log.
				this.logger.debug("Called close() on an endpoint in state OPENING. This message is logged for debug purposes (if we don't fully close).");
			}
		}
	}
	this.setState(Trap.Endpoint.State.CLOSING);
	
	// We'll send END to the other side
	// After that has happened, we'll close (in onend)
	
	try
	{
		this.sendMessage(this.createMessage().setOp(Trap.Message.Operation.END));
	}
	catch (e)
	{
		this.logger.error("Setting Trap.Endpoint.State to ERROR due to an error while disconnecting that may have left the implementation in an inconsistent state");
		this.setState(Trap.Endpoint.State.ERROR);
		// TODO: Cleanup/recovery?
	}
	;
};

/**
 * Attempts to queue data for sending. If the queue length is exceeded, it may
 * block or throw an exception, as per the queue type.
 * <p>
 * Please note that while send will accurately send the data over to the other
 * endpoint, it is advisable to instead use {@link #send(TrapObject)} if the
 * data being sent is a serialized object. If the other endpoint is locally
 * deployed, the TrapObject will never be serialized, thus saving on large
 * amounts of processing power.
 * 
 * @param data
 * @param {Number}
 *            channel The channel to send on
 * @param {Boolean}
 *            useCompression Whether to use compression for this message or not.
 * @throws TrapException
 *             if the queue length is exceeded, or a timeout occurs on a
 *             blocking queue
 */
Trap.Endpoint.prototype.send = function(data, channel, useCompression)
{
	var m = this.createMessage().setOp(Trap.Message.Operation.MESSAGE).setData(data);
	
	if (useCompression)
		m.setCompressed(useCompression && this.compressionEnabled && this.useBinary);
	else
		m.setCompressed(false);
	
	if (typeof (channel) == "number") m.setChannel(channel);
	
	this.sendMessage(m);
};



Trap.Endpoint.prototype.sendMessage = function(message)
{
	// All other states do not allow the sending of messages.
	if (this.getState() != Trap.Endpoint.State.OPEN && message.getOp() != Trap.Message.Operation.END && this.getState() != Trap.Endpoint.State.SLEEPING) throw "Tried to send to non-open Trap session";
	
	var channel = this.getChannel(message.getChannel());
	channel.send(message);
	this.kickSendingThread();
};

Trap.Endpoint.prototype._sendFun = function()
{
	try
	{
		
		for (;;)
		{
			
			// Unlike Java, we don't need to check for a message queue rebuild here.
			
			var first = null;
			if (this.messageQueue.peek() != null)
			{
				try
				{
					first = this.availableTransports.get(0);
				}
				catch (t)
				{
				}
				if (first != null)
				{
					while (first.isAvailable())
					{
						try
						{
							var m = this.messageQueue.peek();
							if (m == null || typeof (m) == "undefined") break;
							
							this.logger.debug("Attempting to send message with op {}", m.getOp());
							
							first.send(m, true);
							this.messageQueue.pop();
						}
						catch (e)
						{
							this.logger.debug(e);
							
							// What should happen if we get an exception here?
							// We
							// don't want this loop to continue, that's for
							// sure.
							// The first transport is clearly inadequate for the
							// task.
							if (first.isAvailable())
							{
								// Now, the problem here is that the regular API
								// only allows us to do a graceful disconnect.
								// If we do that, though, recovery code won't be
								// initialised.
								this.logger.warn("Forcibly removing transport {} from available due to infinite loop protection. This code should not occur with a well-behaved transport.", first.getTransportName());
								this.logger.warn("Caused by {}", e);
								
								first.forceError();
							}
							else
							{
								// Transport is no longer unavailable, loop
								// should
								// be broken.
							}
						}
						
					}
					
					if (first.isAvailable())
						first.flushTransport();
				}
			}
			if (this.messageQueue.peek() == null || first == null)
			{
				this.sending = false;
				return;
			}
		}
	}
	catch (t)
	{
		this.logger.error(t);
	}
	finally
	{
		this.messageQueue.rewind();
	}
};

Trap.Endpoint.prototype.kickSendingThread = function()
{
	if (!this.sending)
	{
		this.sending = true;
		var mt = this;
		setTimeout(function()
		{
			mt._sendFun();
		}, 10);
	}
};

Trap.Endpoint.prototype.ttStateChanged = function(newState, oldState, transport)
{
	if (newState == Trap.Transport.State.AVAILABLE)
	{
		this.addTransportToAvailable(transport);
		this.kickSendingThread();
		return;
	}
	
	// newState is NOT available. Remove the transport from availableTransports,
	// if it was there
	this.availableTransports.remove(transport);
	
	// Now we'll enter failure modes.
	if (newState == Trap.Transport.State.DISCONNECTED || newState == Trap.Transport.State.ERROR)
	{
		if (this.getState() == Trap.Endpoint.State.CLOSED || this.getState() == Trap.Endpoint.State.CLOSING)
		{
			
			// Make sure we update our state properly when all transports have
			// disconnected.
			if (this.getState() == Trap.Endpoint.State.CLOSING)
			{
				
				// Verify if this was the last open transport.
				for ( var i = 0; i < this.transports.size(); i++)
				{
					var t = this.transports.get(i);
					if (t.getState() != Trap.Transport.State.ERROR && t.getState() != Trap.Transport.State.DISCONNECTED) return; // If
				}
				
				this.setState(Trap.Endpoint.State.CLOSED);
				
			}
		}
	}
};

// Abstract method (for subclass usage)
Trap.Endpoint.prototype.reconnect = function(timeout, callback)
{
};

// These callbacks replace the Delegate pattern used in Java.
/**
 * Called when the Trap endpoint has received byte data from the other end.
 * this.method executes in a Trap thread, so it should only perform minimal
 * operations before returning, in order to allow for maximum throughput.
 * 
 * @param evt.data
 *            The data received.
 */
Trap.Endpoint.prototype.onmessage = function(evt)
{
};

/**
 * Called when Trap changes state. Includes both the new state, and the previous
 * one.
 * 
 * @param evt.newState
 *            The state Trap changed to.
 * @param evt.oldState
 *            The previous state.
 */
Trap.Endpoint.prototype.onstatechange = function(evt)
{
};

/**
 * Called when a Trap Endpoint knows it has failed to send some messages.
 * this.can occur when the Trap Endpoint is killed forcibly, loses all its
 * transports while still having an outgoing buffer, or fails to wake up a
 * client that has disconnected all its transports normally.
 * <p>
 * Note that there are conditions when Trap may unwittingly lose data (such as
 * data sent during a switch from unauthenticated -> authenticated session, when
 * the authentication is triggered from the remote side), so the sum of data
 * received by the other end, and called on this.method, may be different.
 * Nevertheless, any data returned by this.method definitely failed to send.
 * 
 * @param evt.datas
 *            A collection of transportable objects that failed sending. Usually
 *            byte arrays, but may contain TrapObject instances.
 */
Trap.Endpoint.prototype.onfailedsending = function(evt)
{
};

/* Internal methods follow */

Trap.Endpoint.prototype.createMessage = function()
{
	return new Trap.Message().setFormat(this.trapFormat);
};

Trap.Endpoint.prototype.addTransportToAvailable = function(t)
{
	
	var added = false;
	
	for ( var i = 0; i < this.availableTransports.size(); i++)
	{
		var c = this.availableTransports.get(i);
		
		// Priority goes from negative to positive (most to least preferred)
		if (c.getTransportPriority() > t.getTransportPriority())
		{
			this.availableTransports.add(i, t);
			added = true;
			break;
		}
		else if (c == t)
			return; // don't double add
	}
	
	if (!added) this.availableTransports.addLast(t);
	
	if (this.availableTransports.size() > this.maxActiveTransports)
	{
		var t = this.availableTransports.getLast();
		this.logger.debug("Disconnecting transport [{}] as the max active transports were exceeded. ({} active, {} max)", t.getTransportName(), this.availableTransports.size(), this._maxActiveTransports);
		t.disconnect();
	}
};

Trap.Endpoint.prototype.ttMessageReceived = function(message, transport)
{
	
	this.logger.debug("Received message with op {}", message.getOp());
	if (this.async && (message.getMessageId() != 0))
	{
		this.getChannel(message.getChannel()).receiveMessage(message, transport);
	}
	else
	{
		this.executeMessageReceived(message, transport);
	}
};

Trap.Endpoint.prototype.executeMessageReceived = function(message, transport)
{
	switch (message.getOp())
	{
		case 1:
			this.onOpen(message, transport);
			break;
		
		case 2:
			this.onOpened(message, transport);
			break;
		
		case 3:
			this.onClose(message, transport);
			break;
		
		case 4:
			this.onEnd(message, transport);
			break;
		
		case 5:
			this.onChallenge(message, transport);
			break;
		
		case 6:
			this.onError(message, transport);
			break;
		
		case 8:
			this.onMessage(message, transport);
			break;
		
		case 16:
			this.onOK(message, transport);
			break;
		
		case 17:
			this.onPing(message, transport);
			break;
		
		case 18:
			this.onPong(message, transport);
			break;
		
		case 19:
			this.onTransport(message, transport);
			break;
		
		default:
			return;
			
	}
	
};

Trap.Endpoint.prototype.onTransport = function(message, transport)
{
	// Transport specific messages. May require us to reconfigure a different
	// transport.
	// This is our hook for future extensions.
};

/*
 * Ping/Pong events are generally a transport-specific concern. The events will
 * be received by the TrapEndpoint, but handled by the transports.
 */
Trap.Endpoint.prototype.onPong = function(message, transport)
{
};

Trap.Endpoint.prototype.onPing = function(message, transport)
{
};

/*
 * An OK will acknowledge a successful operation. This should be a TODO...
 */
Trap.Endpoint.prototype.onOK = function(message, transport)
{
};

Trap.Endpoint.prototype.onMessage = function(message, transport)
{
	this._dispatchMessageEvent(message);
};

Trap._GetterMessageEvent = function(message)
{
	this._orig = message;
	this.type = "message";
};

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "message", function()
{
	return this._orig.data;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "data", function()
{
	return this._orig.data;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "string", function()
{
	return this._orig.string;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "channel", function()
{
	return this._orig.channel;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "dataAsString", function()
{
	return this._orig.string;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "object", function()
{
	return JSON.parse(this._orig.string);
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "buffer", function()
{
	return this._orig.message.data.buffer.slice(message.data.byteOffset, message.data.byteOffset + message.data.byteLength);
});

/*
 * Errors should be handled. Onerror will most likely mean that the connection
 * has reached an unrecoverable state and must be discarded. The application
 * MUST be notified of this state.
 */
Trap.Endpoint.prototype.onError = function(message, transport)
{
	this.setState(Trap.Endpoint.State.ERROR);
};

Trap.Endpoint.prototype.onChallenge = function(message, transport)
{
};

Trap.Endpoint.prototype.onEnd = function(message, transport)
{
	
	if (this.getState() == Trap.Endpoint.State.CLOSING)
	{
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).disconnect();
		
		this.setState(Trap.Endpoint.State.CLOSED);
		
		// TODO: Should this do some more cleanup here? Can we reopen this
		// object? If we can't reopen, should we note it in the state somehow?
	}
	else
	{
		this.setState(Trap.Endpoint.State.CLOSING);
		try
		{
			this.sendMessage(this.createMessage().setOp(Trap.Message.Operation.END));
		}
		catch (e)
		{
			// TODO: Can we handle this error gracefully-er?
			this.logger.warn(e);
			
			for ( var i = 0; i < this.transports.size(); i++)
				this.transports.get(i).disconnect();
		}
	}
	
};

/**
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onClose = function(message, transport)
{
};

/**
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onOpened = function(message, transport)
{
	
	if (this.getState() == Trap.Endpoint.State.CLOSED) return;
	
	if (this.getState() == Trap.Endpoint.State.CLOSING) return;
	
	if (this.getState() == Trap.Endpoint.State.ERROR) return;
	
	if (this.trapID == Trap.Constants.ENDPOINT_ID_CLIENT)
	{
		var cfg = new Trap.Configuration(message.string);
		var id = cfg.getOption(Trap.Constants.ENDPOINT_ID);
		this.setTrapID(id);
	}
	
	this.setState(Trap.Endpoint.State.OPEN);
	
};

Trap.Endpoint.prototype.setState = function(newState)
{
	if (newState == this._state) return; // Department of redundancy
	// department.
	
	var oldState = this._state;
	this._state = newState;
	
	this.logger.debug("TrapEndpoint changing state from {} to {}.", oldState, newState);
	
	this._dispatchEvent({
		type : "statechange",
		newState : newState,
		oldState : oldState
	});
	
	if (newState == Trap.Endpoint.State.OPEN) this._dispatchEvent({
		type : "open"
	});
	
	if (newState == Trap.Endpoint.State.CLOSED) this._dispatchEvent({
		type : "close"
	});
	
	if (newState == Trap.Endpoint.State.SLEEPING) this._dispatchEvent({
		type : "sleep"
	});
	
	if (newState == Trap.Endpoint.State.SLEEPING) this._dispatchEvent({
		type : "sleeping"
	});
	
	if (newState == Trap.Endpoint.State.OPENING) this._dispatchEvent({
		type : "opening"
	});
	
	if (newState == Trap.Endpoint.State.CLOSING) this._dispatchEvent({
		type : "closing"
	});
	
	if (newState == Trap.Endpoint.State.ERROR)
	{
		this._dispatchEvent({
			type : "error"
		});
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).disconnect();
	}
	
	if (newState == Trap.Endpoint.State.CLOSED || newState == Trap.Endpoint.State.CLOSING || newState == Trap.Endpoint.State.ERROR) if (this.keepaliveTask) clearTimeout(this.keepaliveTask);
};

/**
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onOpen = function(message, transport)
{
	
	console.log("Endpoint ONOPEN");
	if (this.getState() == Trap.Endpoint.State.CLOSED || this.getState() == Trap.Endpoint.State.CLOSING || this.getState() == Trap.Endpoint.State.ERROR)
	{
		this.logger.debug("Connection Error: Received OPEN message on {}. Returning with END", this);
		transport.sendTransportSpecific(this.createMessage().setOp(Trap.Message.Operation.END));
		var mt = this;
		
		// Ensure the transport is disconnected.
		setTimeout(function() {
			
				if (transport.getState() != TrapTransportState.DISCONNECTED && transport.getState() != TrapTransportState.ERROR)
				{
					mt.logger.debug("Disconnect Error: {} failed to disconnect, despite ending the session on {}", transport, Tmt);
					transport.forceError();
				}
		}, 5000);
		return;
	}

	try
	{
		transport.sendTransportSpecific(this.createOnOpenedMessage(message), false);
		this.setState(Trap.Endpoint.State.OPEN);
	}
	catch (e)
	{
		this.logger.warn(e);
	}
};

Trap.Endpoint.prototype.createOnOpenedMessage = function(message)
{
	// Send new OPENED message
	return this.createMessage().setOp(Trap.Message.Operation.OPENED);
};

/**
 * @param {Array} messages
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.ttMessagesFailedSending = function(messages, transport)
{
	for ( var i = 0; i < messages.length; i++)
	{
		var message = messages[i];
		this.getChannel(message.getChannel).addFailedMessage();
	}
	
	for (var i=0; i<this.channels.length; i++)
		if (this.channels[i] != null)
			this.channels[i].rebuildMessageQueue();
	
	this.kickSendingThread();
};



/**
 * Fetches the last known liveness timestamp of the endpoint. this.is the last
 * time it received a message from the other end. this.includes all messages
 * (i.e. also Trap messages such as keepalives, configuration, etc) so must not
 * be confused with the last known activity of the other application. For
 * example, in the case of a JavaScript remote endpoint, this.does not guarantee
 * an evaluation error has not rendered the JSApp's main run loop as inoperable.
 * 
 * @see #isAlive(long, boolean, boolean, long) if all that is needed is an
 *      evaluation of the liveness status.
 * @return The timestamp of the last message received from the remote side.
 */
Trap.Endpoint.prototype.lastAlive = function()
{
	// Go through all transports and fetch lastAlive
	
	for ( var i = 0; i < this.transports.size(); i++)
	{
		var t = this.transports.get(i);
		var tLastAlive = t.lastAlive;
		
		if (this._lastAlive < tLastAlive) this._lastAlive = tLastAlive;
	}
	
	return this._lastAlive;
};

/**
 * Attempts to verify if the endpoint is alive, or has been alive within a
 * certain number of milliseconds. Effectively, this.can be used to trigger a
 * keepalive check of the endpoint if used with a <i>within</i> parameter of 0
 * and a <i>check</i> parameter of true.
 * <p>
 * this.function has a two-part purpose. The first is for the application to be
 * able to check the last known liveness of the endpoint, to reduce the
 * discovery time of a dead connection. The second is to trigger a check for a
 * dead endpoint, when the application needs to know that it has active
 * connectivity.
 * <p>
 * Note that in normal operation, the endpoint itself will report when it has
 * disconnected; the application does not need to concern itself with
 * this.detail unless it specifically needs to know that it has connectivity
 * right now.
 * <p>
 * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e. when
 * none of the transports can perform the open() function) may cause a client to
 * lose its connectivity. The client may not have discovered the underlying
 * transport is dead yet, and may require more time to reconnect. A wakeup
 * mechanism can be used to establish liveness, but the server's <i>timeout</i>
 * value should be significantly more generous in order to accommodate the
 * client's reconnect and/or wakeup procedure!
 * 
 * @param {long} within
 *            Within how many milliseconds the last activity of the endpoint
 *            should have occurred before the endpoint should question whether
 *            it is alive.
 * @param {boolean} check
 *            Whether the endpoint should attempt to check for liveness, or
 *            simply return false if the last known activity of the endpoint is
 *            not later than within.
 * @param {boolean} reconnect
 *            Whether to attempt to force reconnection if the transports are not
 *            available within the given timeout. this.will ensure available
 *            liveness value reflects what is possible right now, although it
 *            may mean disconnecting transports that still may recover.
 * @param {long} timeout
 *            If check is true, how many milliseconds at most the liveness check
 *            should take before returning false anyway. The application can use
 *            this.value if it has a time constraint on it.
 * @param {Function} callback <i>true</i> if the connection is currently alive (including if
 *           this.function successfully re-established the connection), <i>false</i>
 *           otherwise.
 */
Trap.Endpoint.prototype.isAlive = function(within, check, reconnect, timeout, callback)
{
	// Ensure lastAlive is up to date.
	this.lastAlive();
	
	// Define within
	var mustBeAliveAfter = new Date().valueOf() - within;
	
	// We're within the allotted time window.
	if (this._lastAlive > mustBeAliveAfter)
	{
		callback(true);
		return;
	}
	
	// We're not allowed to perform the liveness check...
	if (!check)
	{
		callback(false);
		return;
	}
	
	// Unlike Java, we have to unroll the loop and handle it with timeouts.
	
	var i = 0;
	
	// Temporary redefinition to cure a compiler warning.
	// Compiler warnings show useful stuff (especially in JS) so I want to keep
	// them on
	var loop = function()
	{
	};
	var mt = this;
	loop = function(success)
	{
		
		if (success)
		{
			callback(true);
			return;
		}
		
		if (i < mt.availableTransports.size())
		{
			mt.availableTransports.get(i).isAlive(within, check, timeout, loop);
			i++;
		}
		else
		{
			// It appears all available transports are dead. We should reconnect
			if (!reconnect) callback(false);
			
			try
			{
				
				mt.setState(Trap.Endpoint.State.SLEEPING);
				mt.reconnect(timeout, function()
				{
					callback(mt.getState() == Trap.Endpoint.State.OPEN);
				});
				
			}
			catch (e)
			{
				mt.logger.error("Setting TrapEndpoint to state ERROR because reconnect failed. We don't know currently how to recover from this state, so the connection is dropped");
				mt.setState(Trap.Endpoint.State.ERROR);
			}
			
			callback(false);
		}
	};
	
	// Kick the callback loop into action
	loop(false);
	
};

Trap.Endpoint.prototype.getKeepaliveInterval = function()
{
	return this.keepaliveInterval;
};

Trap.Endpoint.prototype.setKeepaliveInterval = function(newInterval)
{
	this.keepaliveInterval = newInterval;
	
	// Forward apply on all transports
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setKeepaliveInterval(newInterval);
	
	var mTimer = this.keepaliveInterval;
	
	if ((mTimer == Trap.Keepalive.Policy.DEFAULT) || (mTimer == Trap.Keepalive.Policy.DISABLED)) return;
	
	if (this.keepaliveTask != null) clearTimeout(this.keepaliveTask);
	
	var mt = this;
	this.keepaliveTask = setTimeout(function()
	{
		mt._keepaliveFun();
	}, mTimer * 1000);
};

Trap.Endpoint.prototype._keepaliveFun = function()
{
	// Conditions that should cause this task to exit.
	if ((this.getState() == Trap.Endpoint.State.CLOSING) || (this.getState() == Trap.Endpoint.State.CLOSED) || (this.getState() == Trap.Endpoint.State.ERROR)) return;
	
	if ((this.getKeepaliveInterval() == Trap.Keepalive.Policy.DISABLED) || (this.getKeepaliveInterval() == Trap.Keepalive.Policy.DEFAULT)) return;
	
	// Calculate the expected time we would need for keepalives to be working
	var expectedTime = new Date().valueOf() - (this.keepaliveInterval * 1000) - this.keepaliveExpiry;
	
	// Now verify all transports are within that time.
	for ( var i = 0; i < this.transports.size(); i++)
	{
		var t = this.transports.get(i);
		
		// Check that the transport is active
		if (!t.isConnected())
		{
			// Inactive transports are excused from keepalives
			continue;
		}
		
		if (t.lastAlive < expectedTime)
		{
			// This transport is not conforming.
			this.logger.debug("Transport {} is not compliant with the keepalive timers. Last alive reported was {}, but expected {}", t.getTransportName(), t.lastAlive, expectedTime);
			
			try
			{
				// Perform a manual check
				var mt = this;
				t.isAlive(this.keepaliveExpiry, true, this.keepaliveExpiry, function(rv)
				{
					if (!rv)
					{
						mt.logger.info("Disconnecting transport {} because it had timed out while not performing its own checks", t.getTransportName());
						t.disconnect();
					}
				});
			}
			catch (e)
			{
				this.logger.error("Exception while checking non-conforming transport", e);
			}
		}
	}
	
	// Now reschedule ourselves
	// Performing this jump will prevent a race condition from making us spiral
	// out of control
	var mTimer = this.keepaliveInterval;
	
	if ((mTimer == Trap.Keepalive.Policy.DEFAULT) || (mTimer == Trap.Keepalive.Policy.DISABLED)) return;
	
	var mt = this;
	this.keepaliveTask = setTimeout(function()
	{
		mt._keepaliveFun();
	}, mTimer * 1000);
};

Trap.Endpoint.prototype.setKeepaliveExpiry = function(newExpiry)
{
	this.keepaliveExpiry = newExpiry;
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setKeepaliveExpiry(newExpiry);
};

/**
 * 
 * @param {Number} channelID
 * @returns {Trap.Channel}
 */
Trap.Endpoint.prototype.getChannel = function(channelID)
{
	var c = this.channels[channelID];
	
	if (c == null)
	{
		c = new Trap.Channel(this, channelID);
		var chunkSize = this.getMaxChunkSize();
		chunkSize = Math.min(chunkSize, c.getChunkSize());
		if (chunkSize <= 0)
			chunkSize = Number.MAX_VALUE;
		c.setChunkSize(chunkSize);
		
		this.channels[channelID] = c;
		this.messageQueue.rebuild(this.channels);
	}
	
	return c;
};

/**
 * @returns {Number}
 */
Trap.Endpoint.prototype.getMaxChunkSize = function()
{
	return this.config.getIntOption("trap." + Trap.Constants.OPTION_MAX_CHUNK_SIZE, 64*1024);
};

/**
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 * @param {any} context
 * @returns void
 */
Trap.Endpoint.prototype.ttMessageSent = function(message, transport, context)
{
	this.getChannel(message.getChannel()).messageSent(message);
};