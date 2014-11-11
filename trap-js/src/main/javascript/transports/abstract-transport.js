Trap.AbstractTransport = function()
{

	Trap.EventObject.call(this);

	this._headersMap		= new Trap.Map();
	this._configuration		= new Trap.Configuration();
	this._prefix 			= "trap.transport." + this.getTransportName().toLowerCase();
	this._authentication	= new Trap.Authentication();

	this._delegate			= null;
	this._delegateContext	= null;

	this._availableKeys		= [];
	this._contextKeys		= [];
	this._contextMap		= new Trap.Map();

	this._transportPriority	= 0;
	
	this._lastAckTimestamp 	= 0;
	this._acks				= null;
	this._ackTask			= 0;
	
	this._format 			= Trap.Constants.MESSAGE_FORMAT_DEFAULT;

	this.logger 			= Trap.Logger.getLogger(this.getTransportName());
	this.fillAuthenticationKeys(this.availableKeys);

	Trap._compat.__defineSetter(this, 'transportPriority', function(newPrio) {
		this._transportPriority = newPrio;
	});
	Trap._compat.__defineGetter(this, 'transportPriority', function() {
		return this._transportPriority;
	});

	Trap._compat.__defineSetter(this, 'configuration', function(newConfig){
		this._configuration = newConfig;
		this.updateConfig();
	});

	Trap._compat.__defineGetter(this, 'configuration', function() {
		return this._configuration.toString();
	});

	Trap._compat.__defineGetter(this, 'state', function() {
		return this._state;
	});

	Trap._compat.__defineSetter(this, 'trapID', function(newID){
		this._trapID = newID;
	});

	Trap._compat.__defineGetter(this, 'trapID', function() {
		return this._trapID;
	});

	Trap._compat.__defineGetter(this, "enabled", function() {
		return this._enabled;
	});

	Trap._compat.__defineSetter(this, "enabled", function(b) {

		if (typeof(b) != "boolean")
			throw "Cannot set to a non-boolean value. Please set enabled to true or false";

		if (b)
			this.enable();
		else
			this.disable();
	});

	Trap._compat.__defineGetter(this, 'keepaliveInterval', function() {
		return this._keepalivePredictor.getKeepaliveInterval();
	});

	Trap._compat.__defineSetter(this, 'keepaliveInterval', function(newInterval) {
		this._keepalivePredictor.setKeepaliveInterval(newInterval);

		if (this.state == Trap.Transport.State.CONNECTED || this.state == Trap.Transport.State.AVAILABLE)
			this._keepalivePredictor.start();
	});

	Trap._compat.__defineGetterSetter(this, 'keepalivePredictor', '_keepalivePredictor');

	Trap._compat.__defineGetterSetter(this, 'keepaliveExpiry', null, function() {
		return this._keepalivePredictor.getKeepaliveExpiry();
	}, function(newExpiry) {
		this._keepalivePredictor.setKeepaliveExpiry(newExpiry);
	});

	Trap._compat.__defineGetter(this, "format", function() {
		return this._format;
	});

	Trap._compat.__defineSetter(this, "format", function(f) {
		this._format = f;
	});

	Trap.AbstractTransport.prototype.init.call(this);
};

Trap.AbstractTransport.prototype = new Trap.EventObject;
Trap.AbstractTransport.prototype.constructor = Trap.AbstractTransport;

Trap.AbstractTransport.prototype.init = function()
{
	this._enabled			= Trap.Constants.TRANSPORT_ENABLED_DEFAULT;
	this._state				= Trap.Transport.State.DISCONNECTED;
	this._trapID			= 0;

	this.lastAlive			= 0;
	this._livenessCheckData	= null;

	this.connectTimeout 	= 30000;

	// Used by the receive method to buffer as needed

	this._bos	= this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();

	// Keepalive information
	if (this._keepalivePredictor)
		this._keepalivePredictor.stop();

	this._keepalivePredictor = new Trap.Keepalive.StaticPredictor();
	this._keepalivePredictor.setDelegate(this);
	
	this.messagesInTransit = new Trap.List();
	this.transportMessageBuffer = new Trap.List();
	
	if (this.connectionTimeoutTask)
		clearTimeout(this.connectionTimeoutTask);
	this.connectionTimeoutTask = null;
	
	if (this.disconnectExpiry != null)
		clearTimeout(this.disconnectExpiry);
	this.disconnectExpiry = null;
	
};

/* **** ABSTRACT METHODS!!! MUST BE SUBCLASSED!!! */

/**
 * Asks the transport to fill the set with the available context keys it can
 * provide for authentication. These keys will be offered to the
 * authentication provider, and can not be changed after the call to this
 * function. The keys are set on a per-transport basis.
 * <p>
 * This function MUST NOT throw.
 * 
 * @param keys
 *            The keys to fill in. The transport should only add keys to
 *            this set.
 */
Trap.AbstractTransport.prototype.fillAuthenticationKeys = function(keys) {};

/**
 * Asks the subclass to update the context map, filling in the keys. This
 * can be called, for example, when a new authentication method is set that
 * may have modified contextKeys.
 */
Trap.AbstractTransport.prototype.updateContext = function() {};

/**
 * Performs the actual sending of a TrapMessage. This method MUST NOT
 * perform any checks on the outgoing messages. It may still perform checks
 * on the transport, and throw appropriately.
 * 
 * @param {Trap.Message} message
 *            The message to send.
 *            @param {Boolean} expectMore Deprecated. Must be set to true.
 * @throws TrapTransportException
 *             If an error occurred while trying to send the message. Before
 *             this exception is thrown, the transport MUST change its state
 *             to ERROR, as it means this transport can no longer be used.
 */
Trap.AbstractTransport.prototype.internalSend = function(message, expectMore) {};


/**
 * Triggers the connect call for the transport (if available). May throw if it cannot connect.
 */
Trap.AbstractTransport.prototype.internalConnect = function(){};

Trap.AbstractTransport.prototype.internalDisconnect = function(){};

Trap.AbstractTransport.prototype.getTransportName = function(){ return "abstract"; };

/*
 * Implementation follows. Feel free to ignore.
 */

Trap.AbstractTransport.prototype.isEnabled = function()
{
	return this._enabled;
};

Trap.AbstractTransport.prototype.isConnected = function()
{
	return this.getState() == Trap.Transport.State.CONNECTED || this.getState() == Trap.Transport.State.AVAILABLE;
};

Trap.AbstractTransport.prototype.configure = function(configurationKey, configurationValue) 
{
	if (!configurationKey.startsWith(this._prefix))
		configurationKey = this._prefix + "." + configurationKey;
	this._configuration.setOption(configurationKey, configurationValue);
	this.updateConfig();
};

Trap.AbstractTransport.prototype.updateConfig = function()
{
	var eString = this.getOption(Trap.Transport.Options.Enabled);
	if (eString != null)
	{
		try
		{
			this._enabled = ("true" == eString);
		}
		catch (e)
		{
			this.logger.warn("Failed to parse transport {} enabled flag; {}", this.getTransportName(), e);
		}
	}

	this.transportPriority = this._configuration.getIntOption(Trap.Transport.Options.Priority, this.transportPriority);

	this.keepaliveInterval = this._configuration.getIntOption("trap.keepalive.interval", this.keepaliveInterval);
	this.keepaliveExpiry = this._configuration.getIntOption("trap.keepalive.expiry", this.keepaliveExpiry);

};

Trap.AbstractTransport.prototype.canConnect = function()
{
	return false;
};

Trap.AbstractTransport.prototype.canListen = function()
{
	return false;
};

Trap.AbstractTransport.prototype.setTransportDelegate = function(newDelegate, newContext)
{
	this.delegate = newDelegate;
	this._delegateContext = newContext;

	this.onmessage = function(e)
	{
		newDelegate.ttMessageReceived(e.message, this, newContext);
	};
	this.onmessagesent = function(e)
	{
		newDelegate.ttMessageSent(e.message, this, newContext);
	};
	this.onstatechange = function(e)
	{
		newDelegate.ttStateChanged(e.newState, e.oldState, this, newContext);
	};
	this.onfailedsending = function(e)
	{
		newDelegate.ttMessagesFailedSending(e.messages, this, newContext);
	};
	
};

Trap.AbstractTransport.prototype.setAuthentication = function(authentication)
{
	this._authentication = authentication;
	this.contextKeys = authentication.getContextKeys(this.availableKeys);
	this.updateContext();
};

Trap.AbstractTransport.prototype.isAvailable = function()
{
	return this.state == Trap.Transport.State.AVAILABLE;
};

/**
 * Changes the state of the transport, and notifies the listener.
 * 
 * @param newState
 *            The state to change to.
 */
Trap.AbstractTransport.prototype.setState = function(newState)
{
	if (newState == this._state)
		return;

	var oldState = this._state;
	this._state = newState;

	if (this.delegate == null)
		this.logger.trace("Transport {} changed state from {} to {}", this.getTransportName(), oldState, newState );

	try
	{
		this._dispatchEvent({type: "statechange", newState:newState, oldState:oldState});
	}
	catch(e)
	{
		this.logger.error("Exception while dispatching statechange: {}", e);
	}
	
	if ((newState == Trap.Transport.State.AVAILABLE) && (this.connectionTimeoutTask != null))
	{
		clearTimeout(this.connectionTimeoutTask);
		this.connectionTimeoutTask = null;
	}

	// Autostart keepalives, if applicable.
	if (newState == Trap.Transport.State.CONNECTED)
	{
		this._keepalivePredictor.start();
	}

	// Autostart keepalives, if applicable.
	if ((newState == Trap.Transport.State.DISCONNECTED) || (newState == Trap.Transport.State.DISCONNECTING) || (newState == Trap.Transport.State.ERROR))
	{
		this._keepalivePredictor.stop();
		
		if (this.disconnectExpiry != null)
			clearTimeout(this.disconnectExpiry);
		this.disconnectExpiry = null;
		
		if (this.messagesInTransit.size() > 0)
		{
			this._dispatchEvent({type: "failedsending", data: this.messagesInTransit, messages: this.messagesInTransit});
		}
	}
	if ((newState == Trap.Transport.State.AVAILABLE) && (oldState == Trap.Transport.State.UNAVAILABLE))
	{
		var mt = this;
		setTimeout(function() {
			mt.flushTransportMessages(false);
		}, 5);
	}
};

Trap.AbstractTransport.prototype.enable = function()
{
	try
	{
		this.configure(Trap.Transport.Options.ENABLED, "true");
	}
	catch (e)
	{
		// Cannot happen.
		this.logger.warn(e.getMessage(), e);
	}
};

Trap.AbstractTransport.prototype.disable = function()
{
	try
	{
		this.configure(Trap.Transport.Options.ENABLED, "false");
	}
	catch (e)
	{
		this.logger.warn(e);
	}
	this.disconnect();
};

Trap.AbstractTransport.prototype.connect = function()
{
	if (!this.isEnabled())
		throw "Transport "+this.getTransportName()+" is unavailable...";

	if (!this.canConnect())
		throw "Transport "+this.getTransportName()+" cannot act as a client";

	if (this.getState() != Trap.Transport.State.DISCONNECTED)
		throw "Cannot connect from state that is not DISCONNECTED";
	
	if (!this.isClientConfigured())
	{
		this.logger.debug("Configuration Error. {} not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.", this);
		this.setState(Trap.Transport.State.ERROR);
		return;
	}

	var mt = this;

	this.connectionTimeoutTask = setTimeout(function() {
		if (mt.getState() == Trap.Transport.State.CONNECTING)
		{
			mt.logger.debug("Connection Error. {} failed to move to state OPEN after 15 seconds... purging it", mt);
			mt.setState(Trap.Transport.State.ERROR);
			mt.internalDisconnect();
		}
	}, this.connectTimeout);

	this.setState(Trap.Transport.State.CONNECTING);
	this.internalConnect();
};

Trap.AbstractTransport.prototype.disconnect = function()
{
	if (this.state == Trap.Transport.State.DISCONNECTING || this.state == Trap.Transport.State.DISCONNECTED || this.state == Trap.Transport.State.ERROR)
		return; // Cannot re-disconnect
		
	if (this.getState() == Trap.Transport.State.CONNECTING)
	{
		this.internalDisconnect();
		return;
	}

	this.setState(Trap.Transport.State.DISCONNECTING);
	this.internalSend(this.createMessage().setOp(Trap.Message.Operation.CLOSE), false);
	
	this.keepalivePredictor.dataSent();
	
	var mt = this;
	this.disconnectExpiry = setTimeout(function() {
		if (mt.getState() != Trap.Transport.State.DISCONNECTED && mt.getState == Trap.Transport.State.ERROR)
		{
			mt.internalDisconnect();
			mt.logger.debug("Disconnection Error. {} moving to state ERROR as failed to disconnect in time. Triggering state was {}", mt, cs);
			mt.setState(Trap.Transport.State.ERROR);
		}
		mt.disconnectExpiry = null;
		
	}, 5000);
	this.internalDisconnect();
};

/* Transport (Abstract) logic follows! This logic will refer to the MOST PARANOID TRANSPORT and MUST be overridden by LESS PARANOID transports */

/**
 * Send checks if the transport is in the correct state, if the message is
 * authenticated (otherwise adds authentication) and performs additional
 * checks when needed.
 * @param {Trap.Message} message
 * @param {Boolean} expectMore
 */
Trap.AbstractTransport.prototype.send = function(message, expectMore) 
{
	if (this.state != Trap.Transport.State.AVAILABLE && this.state != Trap.Transport.State.CONNECTED)
		throw {message: message, state: this.state};

		message.setAuthData(this._authentication.createAuthenticationResponse(null, this.headersMap, message.getData(), this.contextMap));
		
		if (this.logger.isTraceEnabled())
			this.logger.trace("Sending {}/{} on transport {} for {}.", message.getOp(), message.getMessageId(), this, this.delegate );
		
		this.internalSend(message, expectMore);

		if (message.getMessageId() != 0)
			this.addTransitMessage(message);

		this._keepalivePredictor.dataSent();
};

Trap.AbstractTransport.prototype.sendTransportSpecific = function(message)
{
	message.setAuthData(this._authentication.createAuthenticationResponse(null, this.headersMap, message.getData(), this.contextMap));
	this.transportMessageBuffer.add(message);
	this.flushTransportMessages(false);
};

Trap.AbstractTransport.prototype.flushTransportMessages = function(expectMoreAtEnd)
{

	while ((this.getState() == Trap.Transport.State.AVAILABLE || this.getState() == Trap.Transport.State.CONNECTED || this.getState() == Trap.Transport.State.CONNECTING || this.getState() == Trap.Transport.State.DISCONNECTING) && (this.transportMessageBuffer.size() > 0))
	{
		var message = null;
		try
		{
			message = this.transportMessageBuffer.remove();
			if (this.logger.isTraceEnabled())
				this.logger.trace("Sending {}/{} on transport {} for {}.", message.getOp(), message.getMessageId(), this, this.delegate );
			this.internalSend(message, expectMoreAtEnd ? true : this.transportMessageBuffer.size() > 0);
			this._keepalivePredictor.dataSent();
		}
		catch (e)
		{
			this.transportMessageBuffer.addFirst(message);
		}
	}
	
	this.flushTransport();
};

/**
 * Call this when data is received.
 * 
 * @param data
 */
Trap.AbstractTransport.prototype.receive = function(data, offset, length)
{
	try
	{
		// We need to handle the case where message data is spread out over two or more incoming data blobs (e.g. socket, udp, etc)...
		// Therefore, we'll need to do some buffer shuffling.

		this._bos.write(data, offset, length);
		var dataArray = this._bos.toArray();
		var consumed = 0;

		do
		{
			var m = new Trap.Message();
			var thisLoop = m.deserialize(dataArray, consumed, dataArray.length - consumed);

			if (thisLoop == -1)
			{
				break;
			}

			this.receiveMessage(m);

			consumed += thisLoop;
		} while (consumed < dataArray.length);

		if (consumed > 0)
		{
			this._bos = this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();
			try
			{
				this._bos.write(dataArray, consumed, dataArray.length - consumed);
			}
			catch (t)
			{
				this.logger.warn(t);
			}
		}
	}
	catch (e)
	{
		this.logger.warn(e);
		try
		{
			this.sendTransportSpecific(this.createMessage().setOp(Trap.Message.Operation.END));
		}
		catch (e1)
		{
			this.logger.warn(e1);
		}

		// Close the transport, since it's invalid
		// It's illegal to raise an UnsupportedEncodingException at this point in time.
		this.disconnect();
	}
};

Trap.AbstractTransport.prototype.toString = function()
{
	return this.getTransportName() + "/" + this.getTrapID() + "/" + this.getState();
};

/**
 * Called when a message is received, in the most general case.
 * 
 * @param message
 */
Trap.AbstractTransport.prototype.receiveMessage = function(message)
{
	
	if (this.logger.isTraceEnabled())
		this.logger.trace("Received: {}/{} on {} for {}", message.getOp(), message.getMessageId(), this, this.delegate);

	this.lastAlive = new Date().getTime();
	this._keepalivePredictor.dataReceived();
	// Authenticated message.

	var propagate = true;

	// Note to leo: I hate retarded, k?
	switch (message.getOp())
	{
	case 1:
		propagate = this.onOpen(message);
		break;

	case 2:
		propagate = this.onOpened(message);
		break;

	case 3:
		propagate = this.onClose(message);
		break;

	case 4:
		propagate = this.onEnd(message);
		break;

	case 5:
		propagate = this.onChallenge(message);
		break;

	case 6:
		propagate = this.onError(message);
		break;

	case 8:
	case Trap.Message.Operation.FRAGMENT_START:
	case Trap.Message.Operation.FRAGMENT_END:
		propagate = this.onMessage(message);
		break;

	case 9:
		propagate = false;
		this.onAck(message);
		break;

	case 16:
		propagate = this.onOK(message);
		break;

	case 17:
		propagate = this.onPing(message);
		break;

	case 18:
		propagate = this.onPong(message);
		break;

	case 19:
		propagate = this.onTransport(message);
		break;

	default:
		return;

	}

	if (propagate)
	{
		this._dispatchEvent({type: "message", data: message, message: message});
		this.acknowledgeTransitMessage(message);
	}
};

/**
 * Transport messages are most often handled by the Trap layer, then
 * repropagated down. The transport CAN attempt to intercept some but it is
 * NOT recommended.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onTransport = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Ping/Pong should be left to Trap.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onPong = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		//if (this.livenessCheck)
		//	this.livenessCheck(message.getData());

		var bs = message.getDataAsString();
		var type = bs[0];
		var data = bs.substring(7);

		var timer = parseInt(bs.substring(1, 7));

		if (isNaN(timer))
			timer = 30;


		if (type != '3')
			this._keepalivePredictor.keepaliveReceived(false, type, timer, data);
		else if (this.livenessCheck)
			this.livenessCheck(data);
	}

	return authed;
};

/**
 * Ping/Pong should be left to Trap.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onPing = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		
		// Prevent a silly error where an old PING would trigger a PONG while disconnecting
		if (this.getState() == Trap.Transport.State.DISCONNECTING || this.getState() == Trap.Transport.State.DISCONNECTED)
			return authed;
		
		try
		{
			var bs = message.string;
			var type = bs.substring(0, 1);
			var timer = parseInt(bs.substring(1, 7));
			var data = bs.substring(7);

			if (isNaN(timer))
				timer = 30;

			if (type != '3')
				this._keepalivePredictor.keepaliveReceived(true, type, timer, data);
			else
				// isAlive() call
				this.sendKeepalive(false, type, timer, data);
		}
		catch (e)
		{
			this.logger.warn(e);
		}
	}

	return authed;
};

Trap.AbstractTransport.prototype.padTimer = function(timerStr)
{
	while (timerStr.length < 6)
	{
		if (timerStr.startsWith("-"))
			timerStr = "-0" + timerStr.substring(1);
		else
			timerStr = "0" + timerStr;
	}
	return timerStr;
};

/**
 * General ack. Used by Trap; the transport need not apply.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOK = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport should not care for these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onMessage = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MAY inspect these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onError = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onChallenge = function(message)
{
	// We received a challenge.

	try
	{
		var original = new Trap.Message(message.getData());
		var response = this._authentication.createAuthenticationResponse(message.getAuthData(), this.headersMap, original.getData(), this.contextMap);
		original.setAuthData(response);
		this.sendTransportSpecific(original);
	}
	catch (e)
	{
		this.logger.warn(e);
	}

	return false;
};

/**
 * Transport MUST NOT intercept these
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onEnd = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onClose = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		this.disconnect();
		this.internalDisconnect();
		this.setState(Trap.Transport.State.DISCONNECTED);
	}

	return false;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOpened = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		if (this.getState() == Trap.Transport.State.UNAVAILABLE || this.getState() == Trap.Transport.State.CONNECTED)
			this.setState(Trap.Transport.State.AVAILABLE);
		else
			this.logger.debug("Potential race: Transport received onOpen while not connecting");
	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOpen = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		if (this.getState() == Trap.Transport.State.UNAVAILABLE || this.getState() == Trap.Transport.State.CONNECTED)
			this.setState(Trap.Transport.State.AVAILABLE);
		else
			this.logger.debug("Potential race: Transport received onOpen while not connecting");
	}
	else
	{
		// The challenge will have been sent by checkAuth
		// We don't really need to do anything; we'll receive a new
		// OPEN event...
	}

	return authed;
};

Trap.AbstractTransport.prototype.checkAuthentication = function(message)
{

	var authed = this._authentication.verifyAuthentication(message.getAuthData(), this.headersMap, message.getData(), this.contextMap);

	if (!authed)
	{
		// Challenge
		var authChallenge = this._authentication.createAuthenticationChallenge(this.contextMap);
		var challenge = this.createMessage();
		challenge.setOp(Trap.Message.Operation.CHALLENGE);
		challenge.setAuthData(authChallenge);

		try
		{
			challenge.setData(message.serialize());
			this.internalSend(challenge, false);
			this._keepalivePredictor.dataSent();
		}
		catch (e)
		{
			this.logger.warn("Something happened: {}", e);
		}
	}

	return authed;
};

Trap.AbstractTransport.prototype.getOption = function(option)
{
	if (!option.startsWith(this._prefix))
		option = this._prefix + "." + option;

	return this._configuration.getOption(option);
};

Trap.AbstractTransport.prototype.isAlive = function(within, check, timeout, callback)
{
	if (new Date().getTime() - within < this.lastAlive)
	{
		callback(true);
		return;
	}

	if (!check)
	{
		callback(false);
		return;
	}


	if (this.livenessCheckData == null)
	{
		var mt = this;
		this.livenessCheckData = "" + new Date().getTime();
		this.livenessCheck = function(data)
		{
			if (mt.livenessCheckData == data)
			{
				clearTimeout(mt.livenessCheckTimeout);
				callback(true);
			}
		};

		// Don't allow multiple calls to time us out
		if (this.livenessCheckTimeout)
			clearTimeout(this.livenessCheckTimeout);

		this.livenessCheckTimeout = setTimeout(function() {
			callback(false);
		}, timeout);
	}

	this.sendKeepalive(true, '3', this._keepalivePredictor.getNextKeepaliveSend(), this.livenessCheckData);

};

Trap.AbstractTransport.prototype.sendKeepalive = function(ping, type, timer, data)
{

	if (typeof(type) == "undefined" || typeof(timer) == "undefined" || typeof(data) == "undefined")
		throw "Invalid call; Bug.";

	if (type.length != 1)
		throw "Invalid type";

	timer = ""+timer;

	// Now perform the blaady check
	try
	{

		var m = this.createMessage();

		if (ping)
			m.setOp(Trap.Message.Operation.PING);
		else
			m.setOp(Trap.Message.Operation.PONG);

		// Prepare the data. Start with padding timer (0-padded to exactly six characters)
		timer = this.padTimer(timer);

		data = type + timer + data;
		m.setData(data);

		this.sendTransportSpecific(m);

	}
	catch (e)
	{
		this.logger.error(e);
	}


};

Trap.AbstractTransport.prototype.predictedKeepaliveExpired = function(predictor, msec)
{
	this.logger.debug("Keepalive timer for {} expired. Moving to DISCONNECTED.", this.getTransportName());
	this.setState(Trap.Transport.State.DISCONNECTED);
};

Trap.AbstractTransport.prototype.shouldSendKeepalive = function(isPing, type, timer, data)
{
	this.logger.trace("Sending keepalive: {} | {} | {} | {}", isPing, type, timer, data);
	this.sendKeepalive(isPing, type, timer, data);
};

Trap.AbstractTransport.prototype.warnAddressConfiguration = function()
{
	if (this.warnAddressConfigurationPerformed)
		return;
	
	if (!this.configuration.getBooleanOption("warnAddressConfiguration", true))
		return;
	
	this.warnAddressConfigurationPerformed = true;
	
	this.logger.warn("Configuration Error: {} could not detect a single public address; may need configuration!", this);
};

/**
 * Should try and resolve the hostname for an IP address. Servers should override.
 * @param {String} address
 * @param {Boolean} defaultConfig
 * @param {Boolean} failOnUnreachable
 * @return {String}
 */
Trap.AbstractTransport.prototype.getHostName = function(address, defaultConfig, failOnUnreachable)
{
	return "localhost";
};

/**
 * @param {Boolean} client
 * @param {Boolean} server
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isConfigured = function(client, server)
{
	var rv = true;
	
	if (client)
		rv = rv && this.isClientConfigured();
	
	if (server)
		rv = rv && this.isServerConfigured();
	
	return rv;
};

/**
 * Asks whether the transport has the proper configuration for its server
 * role. Must return false if the transport cannot be a server.
 * 
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isServerConfigured = function()
{
	// Most servers are configured by default, so we'll adjust the default accordingly
	return this.canListen();
};

/**
 * Asks whether the transport has the proper configuration for its client
 * role. Must return false if the transport cannot be a client.
 * 
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isClientConfigured = function()
{
	return false;
};

Trap.AbstractTransport.prototype.forceError = function()
{
	if (this.logger.isTraceEnabled())
	{
		this.logger.trace("Error was forced");
	}
	this.setState(Trap.Transport.State.ERROR);
};

/**
 * The following methods deal with messages-in-transit
 * @param {Trap.Message} message
 */

Trap.AbstractTransport.prototype.onAck = function(message)
{
	// TODO Auto-generated method stub
	var data = message.getData();
	
	if (message.getFormat() == Trap.Message.Format.REGULAR)
	{
		var messageID, channelID;
		
		for (var i=0; i<data.length; i+=5)
		{
			channelID = data[i];
			messageID = Trap.ByteConverter.fromBigEndian(data, i+1);
			this.removeTransitMessageById(messageID, channelID);
		}
		
	}
	else
	{
		
		for (var i=0; i<data.length; i+=4)
		{
			var messageID = Trap.ByteConverter.fromBigEndian7(data, i);
			this.removeTransitMessageById(messageID, 0);
		}

	}
	
};

/**
 * Notes to the system that a message is in transit. Some transports (e.g.
 * loopback) are unconcerned about this. HTTP can also reasonably deduce
 * when a transit message has arrived. However, some transports (e.g.
 * socket) have no built-in acknowledgement sequence for a complete message,
 * and may be broken during the course of a transfer.
 * <p>
 * The transit message features allow transports to use built-in methods to
 * detect these failures and trigger ttMessagesFailedSending. This will
 * allow Trap to recover these messages.
 * <p>
 * Override this method to disable transit checking. For performance
 * reasons, {@link #acknowledgeTransitMessage(TrapMessage)} should be
 * overridden as well.
 *
 * @param m
 */
Trap.AbstractTransport.prototype.addTransitMessage = function(m)
{
	if (m.getMessageId() == 0)
		return;
	this.messagesInTransit.add(m);
};

Trap.AbstractTransport.prototype.removeTransitMessageById = function(id, channelID)
{
	var it = this.messagesInTransit.iterator();

	var first = true;

	while (it.hasNext())
	{
		var m = it.next();

		if (m.getMessageId() == id && m.getChannel() == channelID)
		{
			it.remove();

			if (!first)
			{
				// This implies dropped messages!!!
				this.logger.error("It appears we have dropped some messages on an otherwise working transport. Most likely, this transport is bugged; please report this.");
			}
			
			this._dispatchEvent({type: "messagesent", data: m, message: m});
			return;
		}

		first = false;
	}
};

Trap.AbstractTransport.prototype.acknowledgeTransitMessage = function(message)
{
	
	if (message.getMessageId() == 0)
		return;
	
	if (!this._acks)
		this._acks = this.useBinary && this.getFormat() == Trap.Message.Format.REGULAR ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();
	
	if (this._acks.length > 512)
		this.flushAcks();
	
	
	if (this.getFormat() == Trap.Message.Format.REGULAR)
	{
		this._acks.write(message.getChannel());
		this._acks.write(Trap.ByteConverter.toBigEndian(message.getMessageId()));
	}
	else
	{
		this._acks.write(Trap.ByteConverter.toBigEndian7(message.getMessageId()));
	}
	
	
	var ctm = new Date().valueOf();
	
	// Delay acks if we've already sent an ack within the last 5 ms.
	if (this._lastAckTimestamp >= (ctm -5))
	{
		if (!this._ackTask)
		{
			var mt = this;
			this._ackTask = setTimeout(function() {mt.flushAcks();}, 6);
		}
	}
	else
	{
		this.flushAcks();
	}
	
};

Trap.AbstractTransport.prototype.flushAcks = function()
{
	this._lastAckTimestamp = new Date().valueOf();
	var ack = this.createMessage();

	ack.setOp(Trap.Message.Operation.ACK);
	
	ack.setData(this._acks.toArray());
	this._acks.clear();
	
	this.sendTransportSpecific(ack);
	
};


Trap.AbstractTransport.prototype.createMessage = function() {
	var m = new Trap.Message();
	m.setFormat(this.format);
	return m;
};

Trap.AbstractTransport.prototype.isObjectTransport = function() {
	return false;
};
