/*
 * The server trap endpoint does not really need to care about a lot of
 * things... So let's not have it do so
 * 
 * @author Vladimir Katardjiev
 * @param {Trap.ListenerEndpoint} listenerTrapEndpoint The listener that spawned this endpoint. The endpoint MUST notify the listener when it is closed to allow for garbage collection
 * @constructor
 */
Trap.ServerEndpoint = function(listenerTrapEndpoint)
{
	
	Trap.Endpoint.prototype.constructor.call(this);
	this.listener = listenerTrapEndpoint;
	
	this.setState(Trap.Endpoint.State.OPENING);
	
	/*
	 * Server should default to not limit MAT. If client wants a limit, that's fine. If user adds a limit, that's fine too. But default should be max.
	 */
	this.maxActiveTransports = Number.MAX_VALUE;
	
};

Trap.ServerEndpoint.prototype = new Trap.Endpoint;
Trap.ServerEndpoint.prototype.constructor = Trap.ServerEndpoint;

/**
 * @param {Trap.Message} openMessage
 * @return {Trap.Message}
 */
Trap.ServerEndpoint.prototype.createOnOpenedMessage = function(openMessage)
{
	console.log("Creating onOpened message");
	var onopened = Trap.Endpoint.prototype.createOnOpenedMessage.call(this,openMessage);
	var body = new Trap.Configuration(openMessage.getDataAsString());
	
	// Disable compression if the client doesn't advertise the feature.
	var compression = body.getBooleanOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, false);
	this.config.setOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, compression);
	this.compressionEnabled = compression;
	
	var configHash = body.getOption(Trap.Constants.CONFIG_HASH_PROPERTY);
	if (configHash == null)
	{
		this.logger.trace("Client did not request updated configuration...");
		return onopened;
	}

	var clientConfiguration = this.listenerTrapEndpoint.getClientConfiguration(true);
	
	if (clientConfiguration.length == 0)
	{
		this.logger.debug("Automatic configuration update disabled; at least one transport did not have a non-zero IP number configured");
	}

	var digest = Trap.MD5(clientConfiguration);
	if (digest == configHash)
	{
		this.logger.debug("Client configuration was up to date");
		return onopened;
	}
	
	this.logger.debug("Sending updated configuration to the client");
	onopened.setData(clientConfiguration);
	return onopened;
};

/**
 * This method should try to wakeup the client if we have wakeup mechanisms available.
 *  Once that is done, it should wait until the timeout expires and our state is OPEN
 * (non-Javadoc)
 * @see com.ericsson.research.trap.impl.TrapEndpointImpl#reconnect(long)
 * @param {Number} timeout
 * @return null
 */
Trap.ServerEndpoint.prototype.reconnect = function(timeout)
{
	// Asked server to reconnect is an illegal operation so far. In future versions of Trap this should attempt to wakeup if possible.
	this.setState(Trap.Endpoint.State.CLOSED);
	
};

/**
 * @param {Trap.Transport.State} newState
 * @param {Trap.Transport.State} oldState
 * @param {Trap.Transport} transport
 * @param {Object} context
 * @return null
 */
Trap.ServerEndpoint.prototype.ttStateChanged = function(newState, oldState, transport, context)
{
	Trap.Endpoint.prototype.ttStateChanged.call(this, newState, oldState, transport, context);
	
	if ((newState == Trap.Transport.State.DISCONNECTED) || (newState == Trap.Transport.State.ERROR))
	{
		// What happened?
		if ((this.getState() == Trap.Endpoint.State.CLOSING) || (this.getState() == Trap.Endpoint.State.CLOSED))
		{
			// IT's all good
		}
		else
		{
			// It's not good. The transport has disconnected. We should allow for a limited time during which at least one transport should be available.
			if (this.availableTransports.size() == 0)
			{
				this.setState(Trap.Endpoint.State.SLEEPING);
				// Create a task that will kill this endpoint in due time.
				var mt = this;
				setTimeout(function() {
					
						if (mt.getState() == Trap.Endpoint.State.SLEEPING)
						{
							var it = mt.transports.iterator();
							
							while (it.hasNext())
								it.next().disconnect();
							
							mt.setState(Trap.Endpoint.State.CLOSED);
						}
				}, this.reconnectTimeout);
			}
		}
	}
};

//TODO: Add integration test for isAlive(timeout)
Trap.ServerEndpoint.prototype.isAlive = function(timeout, callback)
{
	// TODO: Change this once reconnect doesn't spaz out on me
	return this.isAlive(timeout, true, false, timeout, callback);
};


Trap.ServerEndpoint.prototype.close = function()
{
	Trap.Endpoint.prototype.close.call(this);
	this.listener.endpoints.remove(this.trapID);
	
};