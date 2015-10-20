//< needs(endpoint)

/**
 * Instantiates a new Trap Client.
 * 
 * @classdesc A Trap.ClientEndpoint is a Trap.Endpoint capable of opening an
 *            outgoing connection, commonly to a TrapListener. It is able to
 *            reconnect transports at will, unlike a ServerTrapEndpoint. This is
 *            the main entry point for JavaScript. Create a Trap.ClientEndpoint
 *            like so
 *            <p>
 * 
 * <pre>
 * client = new Trap.ClientEndpoint(&quot;http://trapserver.com:8888&quot;);
 * client.onopen = function() {
 * };
 * client.onmessage = function(evt) {
 * };
 * </pre>
 * 
 * <p>
 * The client will open automatically once instantiated. Assign at least the
 * <b>onopen</b> and <b>onmessage</b> callbacks (and preferably <b>onerror</b>)
 * in order to get a working client.
 * @constructor
 * @param {Boolean} configuration.useBinary Enables (default) or disables binary
 *            support.
 * @param {Boolean} configuration.autoConfigure Enables (default) or disables
 *            automatic configuration
 * @param {String} configuration.remote A URI or TrapConfiguration to the remote
 *            host.
 * 
 * @param {String} configuration The alternate means to instantiate a
 *            ClientEndpoint is to supply it with a single string constituting a
 *            complete configuration.
 * @extends Trap.Endpoint
 */
Trap.ClientEndpoint = function(configuration, autoConfigure) {

	if (typeof (configuration) == "object") {
		this.useBinary = typeof (configuration.useBinary) == "boolean" ? configuration.useBinary
				: true;

		this.autoConfigure = (typeof (configuration.autoConfigure) == "boolean" ? configuration.autoConfigure
				: true);

		configuration = configuration.remote;
	} else {
		this.autoConfigure = (typeof (autoConfigure) == "boolean" ? autoConfigure
				: true);
		this.useBinary = typeof (Trap.useBinary) != "undefined" ? Trap.useBinary
				: Trap.supportsBinary;
	}

	if (this.useBinary && !Trap.supportsBinary)
		throw "Cannot enable binary mode; it is unsupported on this client. Either no transport supported it, or Uint8Array is not present.";

	this.cTransport;

	/**
	 * The list of transports that have not been tried before or after all
	 * transports failed and being tried again.
	 */
	this.transportsToConnect = new Trap.Set();

	/**
	 * The transports that have failed in some non-fatal way. There will be an
	 * attempt taken in the future to recover them.
	 */
	this.failedTransports = new Trap.List();

	this.activeTransports = new Trap.List();

	this.recovering = false;

	Trap.Endpoint.prototype.constructor.call(this);
	this._maxActiveTransports = 1;

	this.trapID = Trap.Constants.ENDPOINT_ID_CLIENT; // Allow the server to
														// override our trap ID.

	// Load the appropriate transports
	for ( var tName in Trap.Transports) {
		var t = Trap.Transports[tName];

		if (t.prototype
				&& typeof (t.prototype.setTransportPriority) == "function") // is a
																			// transport
		{
			var transport = new t();
			this.logger.trace("Initialising new Transport for client: {}",
					transport.getTransportName());

			if (!transport.canConnect()) {
				this.logger.trace("Skipping it; it cannot connect");
				continue;
			}

			if (this.useBinary && !transport.supportsBinary) {
				this.logger
						.info("Skipping it; Trap Binary Mode requested, but transport only supports text");
				continue;
			}

			transport.useBinary = this.useBinary;

			// Unlike Java, TrapEndpoint only defines one addTransport, and that
			// one sets
			// this object as delegate. Thus, we're done.
			this.addTransport(transport);
		}
	}

	if (this.transports.size() == 0)
		throw "No transports could be initialised; either no transports could connect, or transports did not support binary mode (if requested)";

	if ((configuration != null) && (configuration.trim().length > 0)) {

		// Check if we need to redo the configuration.
		if (configuration.startsWith("ws://")
				|| configuration.startsWith("wss://")) {
			// WebSocket Transport
			configuration = "trap.transport.websocket.wsuri = " + configuration;
		} else if (configuration.startsWith("socket://")) {
			// TODO: Socket URI
		} else if (configuration.startsWith("http://")
				|| configuration.startsWith("https://")) {
			configuration = "trap.transport.http.url = " + configuration;
		} else if (!configuration.startsWith("trap"))
			throw "Unknown configuration; invalid format or garbage characters entered";

	}

	this.configure(configuration);

	this.transportRecoveryTimeout = 15 * 60 * 1000;

	var mt = this;
	setTimeout(function() {

		mt.logger.trace("##### CLIENT OPEN ####");
		mt.logger.trace("Config is: {}", mt.config.toString());
		for ( var i = 0; i < mt.transports.size(); i++) {
			var t = mt.transports.get(i);
			mt.logger.trace("Transport [{}] is enabled: {}", t
					.getTransportName(), t.isEnabled());
		}

		mt.setState(Trap.Endpoint.State.OPENING);
		mt.doOpen();

		// Also start recovery
		var recoveryFun = function() {

			mt.failedTransports.clear();

			for ( var i = 0; i < mt.transports.size(); i++) {
				if (mt.getState() == Trap.Endpoint.State.CLOSING
						|| mt.getState() == Trap.Endpoint.State.CLOSED
						|| mt.getState() == Trap.Endpoint.State.CLOSED)
					return;

				var t = mt.transports.get(i);

				// Check if t is active
				var active = false;
				for ( var j = 0; j < mt.activeTransports.size(); j++) {
					if (mt.activeTransports.get(j) == t) {
						active = true;
						break;
					}
				}

				if (!active)
					mt.transportsToConnect.add(t);
			}

			if (!mt.recovering)
				mt.kickRecoveryThread();

			setTimeout(recoveryFun, mt.transportRecoveryTimeout);
		}

		setTimeout(recoveryFun, mt.transportRecoveryTimeout);

	}, 0);
};

Trap.ClientEndpoint.prototype = new Trap.Endpoint;
Trap.ClientEndpoint.prototype.constructor = Trap.ClientEndpoint;

Trap.ClientEndpoint.prototype.parseConfiguration = function(configuration) {
	return new Trap.CustomConfiguration(configuration);
};

Trap.ClientEndpoint.prototype.open = function() {

};

//> (void)fn()
Trap.ClientEndpoint.prototype.doOpen = function() {
	// If the list of transports that still can be used and is empty -> die!
	if (this.transports.size() == 0) {
		this.setState(Trap.Endpoint.State.ERROR);
		throw "No transports available";
	}
	// Clean all the failed transports so far, we'll retry all of them anyway.
	this.failedTransports.clear();
	this.activeTransports.clear();
	this.availableTransports.clear();
	this.transportsToConnect.clear();

	// Let transportsToConnect be the list of transports that we haven't tried.
	this.transportsToConnect.addAll(this.transports);

	// Pick the first untested transport (the one with the highest priority)
	this.kickRecoveryThread();
};

// One of our transports has changed the state, let's see what happened...
//> (void) fn(Trap.Endpoint.State, Trap.Endpoint.State, Trap.Transport)
Trap.ClientEndpoint.prototype.ttStateChanged = function(newState, oldState,
		transport) {

	this.logger.debug("Transport {} changed state to {}", transport
			.getTransportName(), newState);
	// Super will manage available transports. All we need to consider is what
	// action to take.
	Trap.Endpoint.prototype.ttStateChanged.call(this, newState, oldState,
			transport);

	if (this.getState() == Trap.Endpoint.State.CLOSED
			|| this.getState() == Trap.Endpoint.State.CLOSING
			|| this.getState() == Trap.Endpoint.State.ERROR)
		return;

	// This is fine. We're not interested in disconnecting transports; super has
	// already managed this for us.
	if (oldState == Trap.Transport.State.DISCONNECTING) {
		this.activeTransports.remove(transport);
		this.availableTransports.remove(transport);
		return;
	}

	// What to do if we lose a transport
	if (newState == Trap.Transport.State.DISCONNECTED
			|| newState == Trap.Transport.State.ERROR) {

		this.activeTransports.remove(transport);

		// This was an already connected transport. If we have other transports
		// available, we should silently try and reconnect it in the background
		if (oldState == Trap.Transport.State.AVAILABLE
				|| oldState == Trap.Transport.State.UNAVAILABLE
				|| oldState == Trap.Transport.State.CONNECTED) {

			if (this.activeTransports.size() != 0) {
				this.transportsToConnect.add(transport);
				this.kickRecoveryThread();
				return;
			}

			if (this.getState() == Trap.Endpoint.State.OPENING) {
				// The current transport failed. Just drop it in the failed
				// transports pile.
				// (Failed transports are cycled in at regular intervals)
				this.failedTransports.add(transport);

				// Also notify recovery that we have lost a transport. This may
				// schedule another to be reconnected.
				this.kickRecoveryThread();
				return;
			} else {

				var openTimeout = 1000;

				if (this.getState() == Trap.Endpoint.State.OPEN) {
					// We have to report that we've lost all our transports.
					this.setState(Trap.Endpoint.State.SLEEPING);

					// Adjust reconnect timeout
					this.canReconnectUntil = new Date().valueOf()
							+ this.reconnectTimeout;

					// This is the first time, just reconnect immediately
					openTimeout = 0;
				}

				if (this.getState() != Trap.Endpoint.State.SLEEPING) {
					// We have nothing to do here
					return;
				}

				var mt = this;

				if (new Date().valueOf() < this.canReconnectUntil) {
					setTimeout(
							function() {

								try {
									mt.doOpen();
								} catch (e) {
									mt.logger
											.error(
													"Error while reconnecting after all transports failed",
													e);
									return;
								}

							}, openTimeout);

				}

			}
		} else if (oldState == Trap.Transport.State.CONNECTING) {
			this.cycleTransport(transport, "connectivity failure");
		} else {
			// disconnecting, so do nothing
		}
	}

	if (newState == Trap.Transport.State.CONNECTED) {
		if (oldState == Trap.Transport.State.CONNECTING) {
			this.sendOpen(transport);
		} else {
			this.logger
					.error("Reached Trap.Transport.State.CONNECTED from a non-CONNECTING state. We don't believe in this.");
		}
	}
};

Trap.ClientEndpoint.prototype.ttMessageReceived = function(message, transport) {
	if (transport == this.cTransport) {
		if (message.getOp() == Trap.Message.Operation.OPENED) {
			this.cTransport = null;
			// received configuration from the server
			if (this.autoConfigure && message.getData().length > 0)
				this.config.setStaticConfiguration(message.getData());
		} else {
			this.cycleTransport(transport, "illegal open reply message op");
			return;
		}
	}
	Trap.Endpoint.prototype.ttMessageReceived.call(this, message, transport);
};

Trap.ClientEndpoint.prototype.sendOpen = function(transport) {
	var m = this.createMessage().setOp(Trap.Message.Operation.OPEN);
	var body = new Trap.Configuration();
	if (this.autoConfigure) {
		try {
			var str = this.getConfiguration().toString();
			var hashed = Trap.MD5(str); // Represented as hex string
			body.setOption(Trap.Configuration.CONFIG_HASH_PROPERTY, hashed);
		} catch (e) {
			this.logger.warn("Could not compute client configuration hash", e);
		}
		;
	}

	if (this.connectionToken == null)
		this.connectionToken = Trap._uuid();

	body.setOption("trap.connection-token", this.connectionToken);
	body.setOption(Trap.Constants.OPTION_MAX_CHUNK_SIZE, "" + (16 * 1024));
	body.setOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, "true"); // Forcibly
																		// enable
																		// compression
																		// for
																		// the
																		// time
																		// being

	if (!!this.config.getOption(Trap.Constants.OPTION_AUTO_HOSTNAME))
		body.setOption(Trap.Constants.OPTION_AUTO_HOSTNAME, this.config
				.getOption(Trap.Constants.OPTION_AUTO_HOSTNAME));

	m.setData(body.toString());

	try {
		transport.send(m, false);
	} catch (e) {
		this.cycleTransport(transport, "open message send failure");
	}
};

Trap.ClientEndpoint.prototype.cycleTransport = function(transport, reason) {
	this.logger.debug("Cycling transports due to {} {}...", transport
			.getTransportName(), reason);
	transport.onmessage = function() {
	};
	transport.onstatechange = function() {
	};
	transport.onfailedsending = function() {
	};
	transport.onmessagesent = function() {
	};
	transport.disconnect();

	this.activeTransports.remove(transport);
	this.failedTransports.add(transport);

	// Recover only if we have active transports. Otherwise do open...
	if (this.transportsToConnect.size() == 0) {

		if (this.activeTransports.size() > 0) {
			// Let recovery take care of reopening.
			return;
		}

		if (this.getState() == Trap.Endpoint.State.OPENING) {
			this.logger
					.error("Could not open a connection on any transport...");
			this.setState(Trap.Endpoint.State.ERROR);
			return;
		}

		var mt = this;

		// Don't recycle!
		if (this._cycling)
			return;

		this._cycling = setTimeout(function() {
			try {
				this._cycling = null;
				mt.doOpen();
			} catch (e) {
				mt.logger.warn(e);
			}
		}, 1000);
	} else
		this.kickRecoveryThread();
};

Trap.ClientEndpoint.prototype.kickRecoveryThread = function() {
	if (this.recovering)
		return;

	var mt = this;

	this.recovering = setTimeout(
			function() {

				// Don't reconnect transports if the endpoint doesn't want them
				// to be.
				if (mt.state == Trap.Endpoint.State.CLOSED
						|| mt.state == Trap.Endpoint.State.CLOSING
						|| mt.state == Trap.Endpoint.State.ERROR)
					return;

				try {
					for (;;) {

						// Sort the connecting transports
						// This ensures we always get the first transport

						mt.transportsToConnect.sort(function(o1, o2) {
							return o1.getTransportPriority()
									- o2.getTransportPriority();
						});

						var first = null;
						if (mt.transportsToConnect.size() > 0) {
							try {

								first = mt.transportsToConnect.remove(0);

								if (first != null) {

									var t = first;

									mt.logger
											.trace(
													"Now trying to connect transport {}",
													t.getTransportName());

									if (!t.canConnect()) {
										mt.logger
												.trace("Skipping: Transport cannot connect");
										continue;
									}

									if (!t.isEnabled()) {
										mt.logger
												.trace("Skipping: Transport is disabled");
										continue;
									}

									// Abort connection attempts if head
									// transport is downprioritised.
									var downPrioritised = false;
									for ( var i = 0; i < mt.availableTransports
											.size(); i++)
										if (mt.availableTransports.get(i)
												.getTransportPriority() < t
												.getTransportPriority())
											downPrioritised = true;

									if (downPrioritised) {
										mt.transportsToConnect.add(0, t);
										mt.logger
												.trace("Skipping: Transport is downprioritised (we'll try a higher prio transport first)");
										break;
									} // */

									t.init(); // Hook the delegate methods

									t.onmessage = function(e) {
										mt.ttMessageReceived(e.message,
												e.target, null);
									};
									t.onstatechange = function(e) {
										mt.ttStateChanged(e.newState,
												e.oldState, e.target, null);
									};
									t.onfailedsending = function(e) {
										mt.ttMessagesFailedSending(e.messages,
												e.target, null);
									};
									t.onmessagesent = function(e) {
										mt.ttMessageSent(e.message, e.target,
												null);
									};

									t.setAuthentication(mt.authentication);
									t.setConfiguration(mt.config);
									t.setFormat(mt.getFormat());

									mt.activeTransports.add(t);
									t.connect();

								}
							} catch (e) {
								if (!!first) {
									mt.failedTransports.add(first);
									mt.activeTransports.remove(first);
								}
							}

						}

						if (mt.transportsToConnect.size() == 0 || first == null) {
							mt.recovering = null;
							return;
						}
					}
				} catch (t) {
					mt.logger.warn(t);
					mt.recovering = null;
				}
				mt.recovering = null;
			}, 0);
};

Trap.ClientEndpoint.prototype.reconnect = function(timeout) {
	// On the client, we'll use the transports list in order to reconnect, so we
	// have to just d/c and clear available transports.
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).disconnect();

	// After having jettisonned all transports, create new data structs for them
	this.availableTransports = new Trap.List();

	// Restart connection attempts
	this.doOpen();

	// Set a timeout reconnect
	var mt = this;
	if (timeout > 0)
		this.reconnectFunTimer = setTimeout(function() {

			if (mt.getState() != Trap.Endpoint.State.OPEN)
				mt.setState(Trap.Endpoint.State.CLOSED);

			mt.reconnectFunTimer = null;

		}, timeout);
};

Trap.ClientEndpoint.prototype.onOpened = function(message, transport) {

	var rv = Trap.Endpoint.prototype.onOpened.call(this, message, transport);

	if (this.trapID != Trap.Constants.ENDPOINT_ID_CLIENT && this.autoConfigure) {
		if (!!message.string && message.string.length > 0) {
			// Message data should contain new configuration
			this.logger.debug("Received new configuration from server...");
			this.logger.debug("Configuration was [{}]", message.string);
			this.configure(message.string);

			// Any transport that is currently non-active should be scheduled to
			// connect
			// This includes transports that weren't connected in the first
			// place (transport priorities may have changed)
			this.failedTransports.clear();

			for ( var i = 0; i < this.transports.size(); i++) {
				var t = this.transports.get(i);

				// Check if t is active
				var active = false;
				for ( var j = 0; j < this.activeTransports.size(); j++) {
					if (this.activeTransports.get(j) == t) {
						active = true;
						break;
					}
				}

				if (!active)
					this.transportsToConnect.add(t);
			}

			// Now make them connect
			this.kickRecoveryThread();

		}
	}

	return rv;

};
