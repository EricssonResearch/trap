Trap.Transports.HTTP = function()
{
	Trap.AbstractTransport.call(this);
	this._transportPriority	= 100;
	this.expirationDelay = 28000;
	this.connectionTimeout = 10000;
	this.latencyEstimate = 1000; // Start with a reasonably generous latency estimate

	this._buf = [];
};

Trap.Transports.HTTP.prototype = new Trap.AbstractTransport;
Trap.Transports.HTTP.prototype.constructor = new Trap.Transports.HTTP;

Trap.Transports.HTTP.CONFIG_URL = "url";

try
{
	Trap.Transports.HTTP.prototype.supportsBinary = typeof new XMLHttpRequest().responseType === 'string';
}
catch(e)
{
	Trap.Transports.HTTP.prototype.supportsBinary = false;
}

//Trap.supportsBinary = Trap.supportsBinary && Trap.Transports.HTTP.prototype.supportsBinary;

Trap.Transports.HTTP.prototype.init = function() {
	
	this._buf = [];
	
	// Abort any pre-existing polls so they don't interfere with our state changes...
	if (this._longpoll)
		this._longpoll.onreadystatechange = function() {};
	
	Trap.AbstractTransport.prototype.init.call(this);
};

Trap.Transports.HTTP.prototype.getTransportName = function()
{
	return "http";
};

Trap.Transports.HTTP.prototype.getProtocolName = function()
{
	return "http";
};

Trap.Transports.HTTP.prototype.updateConfig = function()
{
	Trap.AbstractTransport.prototype.updateConfig.call(this);
	
	if ((this.getState() == Trap.Transport.State.DISCONNECTED) || (this.getState() == Trap.Transport.State.CONNECTING))
	{
		this.url = this.getOption(Trap.Transports.HTTP.CONFIG_URL);
	}
	else
		this.logger.debug("Updating HTTP configuration while open; changes will not take effect until HTTP is reconnected");

	this.expirationDelay = this._configuration.getIntOption(this._prefix + ".expirationDelay", this.expirationDelay);
	this.connectionTimeout = this._configuration.getIntOption(this._prefix + ".connectionTimeout", this.connectionTimeout);
};

//TODO: Expose IP information on websocket level...
Trap.Transports.HTTP.prototype.fillAuthenticationKeys = function(keys)
{
};

Trap.Transports.HTTP.prototype.updateContext = function()
{
	// TODO Auto-generated method stub

};

Trap.Transports.HTTP.prototype.internalSend = function(message, expectMore) 
{

	var mt = this;
	
	mt._sendQueued = true;

	if (!!message)
		this._buf.push(message);

	if (expectMore)
	{
		if (mt._sendTimer)
			clearTimeout(mt._sendTimer);
		
		mt._sendTimer = setTimeout(function() {
			mt.internalSend(null, false);
		}, 1000);
		
		return;
	}
	
	if (mt._sendTimer)
	{
	    clearTimeout(mt._sendTimer);
	    mt._sendTimer = null;   
	}
	
	if (this._buf.length == 0)
		return; // Erroneous call.

	// Slam the messages
	
	var bos = (this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream());
	for (var i=0; i<this._buf.length; i++)
		bos.write(this._buf[i].serialize(this.useBinary));
	
	var data = (this.useBinary ? bos.toArray() : bos.toString());
	
	if (mt.getState() == Trap.Transport.State.AVAILABLE || mt.getState() == Trap.Transport.State.CONNECTED)
		mt.setState(Trap.Transport.State.UNAVAILABLE);
	
	var x = this.openConnection("POST");
	x.setRequestHeader("Content-Type", "x-trap");

	x.send(data.buffer ? data.buffer : data);
	
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
		{ 
			if (x.hasError || x.hasTimeout || x.isAborted)
			{
				mt._dispatchEvent({type: "failedsending", messages: mt._buf});
				mt.setState(Trap.Transport.State.ERROR);
			}
			else
			{
				
				for (var i=0; i<mt._buf.length; i++)
					if (mt._buf[i].getMessageId() > 0)
						mt._dispatchEvent({type:"messagesent", data: mt._buf[i], message: mt._buf[i]});
				
				mt._buf = [];
				
				// Prevent state to go to AVAILABLE when we're actually DISCONNECTED.
				if (mt.getState() == Trap.Transport.State.UNAVAILABLE || mt.getState() == Trap.Transport.State.CONNECTED)
					mt.setState(Trap.Transport.State.AVAILABLE);
			}
			
			mt._sendQueued = false;
		}
	};

};

Trap.Transports.HTTP.prototype.isClientConfigured = function()
{
	return this.url && typeof(this.url) == "string" && this.url.length > 4;
};

Trap.Transports.HTTP.prototype.internalConnect = function()
{
	
	this.logger.debug("HTTP Transport Opening...");

	// Check for proper configuration
	if (!this.isClientConfigured())
	{
		this.logger.debug("HTTP Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.");
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
	
	var mt = this;
	try
	{

		var x = this.openConnection("GET");
		x.responseType = "text";
		x.onreadystatechange = function()
		{
			if (x.readyState == 4)
			{
				
				if (x.status == 200 && !x.hasError && !x.hasTimeout && !x.isAborted)
				{
					if ('/' == mt.url.charAt(mt.url.length-1))
						mt.url = mt.url + x.responseText;
					else
						mt.url = mt.url + '/' + x.responseText;

					mt.running = true;
					mt.poll();
					mt.setState(Trap.Transport.State.CONNECTED);
				}
				else
				{
					mt.logger.warn("HTTP transport failed with state ", x.status);
					mt.setState(Trap.Transport.State.ERROR);
					return true;
				}
			}

			return false;
		};
		x.send();
	}
	catch(e)
	{
		this.logger.warn("HTTP transport failed to connect due to ", e);
		if(e.stack)
			this.logger.debug(e.stack);
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
};

Trap.Transports.HTTP.prototype.internalDisconnect = function()
{

	var mt = this;
	
	if (mt._sendQueued)
	{
		setTimeout(function() { mt.internalDisconnect(); }, 100);
		return;
	}
	
	var x = new XMLHttpRequest();
	x.open("POST", this.url, true);

	// The disconnect should succeed whether or not the XHR does.
	// Two ways to call done
	var done = function() {
		mt.running = false;

		if(mt.getState() == Trap.Transport.State.DISCONNECTING)
			mt.setState(Trap.Transport.State.DISCONNECTED);
	};

	// State change -- connection done!
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
			done();
	};
	
	x.send();

};

Trap.Transports.HTTP.prototype.canConnect = function()
{
	return true;
};

Trap.Transports.HTTP.prototype.poll = function()
{
	
	if (!this.running)
		return;
	
	var x = this.openConnection("GET");
	
	if (this.useBinary)
	try
	{
		x.responseType = "arraybuffer";
	}
	catch(e)
	{
		console.error("Asked to use binary but could not use binary mode transport!");
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
		
	var mt = this;
	
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
		{
			if (x.isAborted)
			{
				if (x.abortState == 2)
				{
					mt.poll();
					mt._keepalivePredictor.dataReceived();
				}
				else
				{
					mt.setState(Trap.Transport.State.ERROR);
				}
			}
			else
			{
				if (x.status < 300 && x.status >= 200)
				{
					if (x.responseType == "arraybuffer")
					{
						try
						{
							if (x.response)
							{
								var data = new Uint8Array(x.response);
								mt.receive(data);
							}
							
							if (x.status != 0 || x.statusText.length != 0)
								mt.poll();
						}
						catch(e)
						{
							console.log(e);
						}
					}
					else
					{
						var data = x.responseText;
						// Data will be a Unicode string (16-bit chars). notifyData expects bytes though
						// Encode data as UTF-8. This will align the bytes with the ones expected from the server.
						data = data.toUTF8ByteArray();
						mt.receive(data, 0, data.length);
						
						mt.poll();
						mt._keepalivePredictor.dataReceived();
					}
				}
				else if (x.status == 0 || x.status >= 300)
				{
					if (mt.getState() != Trap.Transport.State.DISCONNECTING && mt.getState() != Trap.Transport.State.DISCONNECTED)
						mt.setState(Trap.Transport.State.ERROR);
				}
			}
		}
	};

	x.send();
	mt._keepalivePredictor.dataSent();

	this._longpoll = x;
};


Trap.Transports.HTTP.prototype.openConnection = function(type)
{
	var x = new XMLHttpRequest();
	x.open(type, this.url + "?expires=" + this.expirationDelay , true);
	x.aborted = false;
	x.responseType === 'arraybuffer';
	
	var mt = this;

	function abort(error)
	{
		x.isAborted = true;
		x.abortState = x.readyState;
		
		x.abort();
	}

	var pollTimeout =  null;
	var connTimeoutFun = function() {
		if (x.readyState == 1)
		{
			abort();
			mt.logger.warn("XHR longpoll failed to connect...");
		}
	};
	x.connectionTimer = setTimeout(connTimeoutFun, this.expirationDelay + this.latencyEstimate*3);
	
	var latencyRecorded = false;
	var start = new Date().valueOf();
	
	// Also used to clear the connection timeout, since latency implies connection.
	function recordLatency()
	{
		if (latencyRecorded)
			return;
		
		latencyRecorded = true;
		
		var end = new Date().valueOf();
		var latency = end - start;
		mt.latencyEstimate = (mt.latencyEstimate + latency)/2;
	}
	
	// Handles timeouts for an upload.
	if(x.upload) x.upload.addEventListener("loadstart", function() 
	{
		
		// We can't wait for the connection timeout when we're uploading...
		clearTimeout(x.connectionTimer);
		// Cannot record latency on an upload since the headers will return only after the body is uploaded.
		latencyRecorded = true;
		
		var progressed = false;
		var granularity = 1000;
		var done = false;
		
		// Add progress handlers
		x.upload.addEventListener("progress", function() {
			progressed = true;
		}, true);
		
		x.upload.addEventListener("error", function() {
			x.hasError = true;
		}, true);
		
		x.upload.addEventListener("timeout", function() {
			x.hasTimeout = true;
		}, true);
		
		x.upload.addEventListener("load", function() {
			clearTimeout(pFunTimeout);
			done = true;
			
			// Restart connectionTimeout -- we're waiting for headers now!
			x.connectionTimer = setTimeout(connTimeoutFun, mt.connectionTimeout);
		}, true);
		
		x.upload.addEventListener("loadend", function() {
			if (!done)
			{
				mt.logger.warn("Incomplete upload: loadend without load");
				x.hasError = true;
			}
		}, true);

		var pFun = function() {

			if (!mt.running)
				return;
			
			if (x.readyState == 4)
				return;

			if (!progressed)
			{
				// Timeout has occurred.
				abort();
				return;
			}
			progressed = false;
			setTimeout(pFun, granularity);

		};

		var pFunTimeout = setTimeout(pFun, mt.connectionTimeout);
		
	}, true);
	
	x.addEventListener("loadstart", function() {
		mt.logger.trace("XHR load started...");
	});

	x.addEventListener("readystatechange", function()
	{
		switch(x.readyState)
		{
		
		case 0:
			break;
		case 1:
			break;
		
		case 2:
			mt.logger.debug("XHR switched state to headers received (we have connection to server). Stopping connectionTimeout, starting pollTimeout");
			// We have connected (connTimeout unnecessary)
			clearTimeout(x.connectionTimer);
			recordLatency();
			
			// Just keep track of the polling time.
			pollTimeout = setTimeout(function() {

				// Guard against timeout incorrectly set.
				if (!mt.running)
					return;

				switch(x.readyState)
				{
				case 0:
				case 1:
					// This should be impossible
					abort();
					mt.logger.warn("XHR ended in an inconsistent state...");
					mt.setState(Trap.Transport.State.ERROR);
					return;

				case 2:
					// Headers received but no body. Most likely network failure
					abort();
					mt.logger.debug("Loading failed after headers loaded");
					return;

				case 3:
					// Body in process of being received
					// Do nothing; subsequent code will take care of it.
					break;

				case 4:
					// Should not happen.
					mt.logger.error("HTTP transport in inconsistent state");
					mt.setState(Trap.Transport.State.ERROR);
					return;

				}

				var progressed = false;

				x.onprogress = function() {
					progressed = true;
				};

				var pFun = function() {

					if (!mt.running)
						return;
					
					if (x.readyState == 4)
						return;

					if (!progressed)
					{
						// Timeout has occurred.
						abort();
						return;
					}
					progressed = false;
					setTimeout(pFun, 100);

				};

				setTimeout(pFun, 100);

//			}, this.expirationDelay + this.latencyEstimate * 3); // Add some spare time for expiration delay to kick in/transfer to occur.
				// This should be slightly longer time, as it takes a while for node.js to switch from 2 to 3. Dangit.
			}, 30000); // Add some spare time for expiration delay to kick in/transfer to occur.

			break;

		case 3:
			mt.logger.debug("XHR switched state to Receiving (data incoming from server)");
			break;

		case 4:
			// Prevent timeout function from being called
			clearTimeout(pollTimeout);
			
			// Handle error cases
			if (x.hasError || x.hasTimeout)
			{
				mt.setState(Trap.Transport.State.ERROR);
			}
			break;
		}
	}, true);
	
	var done = false;
	
	x.addEventListener("error", function() {
		x.hasError = true;
	}, true);
	
	x.addEventListener("timeout", function() {
		x.hasTimeout = true;
	}, true);
	
	x.addEventListener("load", function() {
		done = true;
	}, true);
	
	x.addEventListener("loadend", function() {
		if (!done)
		{
			mt.logger.warn("Incomplete download: loadend without load");
			x.hasError = true;
		}
	}, true);
	
	return x;
};

Trap.Transports.HTTP.prototype.flushTransport = function()
{
	this.internalSend(null, false); 
};

Trap.Transports.HTTP.prototype.setState = function()
{
	if (this.getState() == Trap.Transport.State.DISCONNECTED || this.getState() == Trap.Transport.State.ERROR)
		this.running = false;

	Trap.AbstractTransport.prototype.setState.apply(this, arguments);
};