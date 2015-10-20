Trap.Transports.HTTPServer = function()
{
	Trap.AbstractTransport.call(this);
	this._transportPriority	= 100;
	this.expirationDelay = 28000;
	this.connectionTimeout = 10000;
	this.latencyEstimate = 1000; // Start with a reasonably generous latency estimate
	this._state = Trap.Transport.State.CONNECTING;
	this.useBinary = true;

	this._buf = [];
};

Trap.Transports.HTTPServer.prototype = new Trap.AbstractTransport;
Trap.Transports.HTTPServer.prototype.constructor = new Trap.Transports.HTTPServer;

Trap.Transports.HTTPServer.CONFIG_URL = "url";

//Trap.supportsBinary = Trap.supportsBinary && Trap.Transports.HTTPServer.prototype.supportsBinary;

Trap.Transports.HTTPServer.prototype.init = function() {
	
	this._buf = [];
	
	// Abort any pre-existing polls so they don't interfere with our state changes...
	if (this._longpoll)
		this._longpoll.onreadystatechange = function() {};
	
	Trap.AbstractTransport.prototype.init.call(this);
};

Trap.Transports.HTTPServer.prototype.getTransportName = function()
{
	return "http";
};

Trap.Transports.HTTPServer.prototype.getProtocolName = function()
{
	return "http";
};

Trap.Transports.HTTPServer.prototype.setCors = function(req, res)
{
	var origin = req.headers['origin'];
	
	if (origin == null)
		origin = "null";
	
	res.setHeader("Allow", "GET,PUT,POST,DELETE,OPTIONS");
	res.setHeader("Access-Control-Allow-Origin", origin);
	res.setHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
	res.setHeader("Access-Control-Allow-Headers", "Content-Type");
	res.setHeader("Access-Control-Request-Methods", "GET,PUT,POST,DELETE,OPTIONS");
	res.setHeader("Access-Control-Request-Headers", "Content-Type");
	res.setHeader("Access-Control-Max-Age", "3600");
};

	
/*
 * HTTP transport will only switch to available when a GET is present. Thus, we'll await the inevitable conclusion.
 * 
 * (non-Javadoc)
 * @see com.ericsson.research.trap.spi.transports.AbstractTransport#onOpened(com.ericsson.research.trap.spi.TrapMessage)
 */

Trap.Transports.HTTPServer.prototype.onOpened = function(message)
{
	var authed = this.checkAuthentication(message);
	if (authed && this.getState() == Trap.Transport.State.CONNECTING)
		this.setState(Trap.Transport.State.CONNECTED);
	return authed;
};

Trap.Transports.HTTPServer.prototype.onOpen = function(message)
{
	var authed = this.checkAuthentication(message);
	if (authed && this.getState() == Trap.Transport.State.CONNECTING)
	{
		this.setState(Trap.Transport.State.CONNECTED);
		
		if (this.pollReq && this.pollRes)
			this.setState(Trap.Transport.State.AVAILABLE);
		
	}
	
	console.log(authed);
		
	return authed;
};

Trap.Transports.HTTPServer.prototype.handle = function(req, res)
{
	
	console.log("Got method " + req.method);
	if (req.method == "GET")
	{
		var mt = this;
		this.pollReq = req;
		this.pollRes = res;
	
		if (this._buf.length > 0)
		{
			this.flushTransport();
		}	
		else
		{
			if (this.getState() == Trap.Transport.State.UNAVAILABLE)
			{
				if (this.oldState == Trap.Transport.State.CONNECTED)
					this.setState(Trap.Transport.State.AVAILABLE);
				else
					this.setState(this.oldState);
			}
			else if (this.getState() == Trap.Transport.State.CONNECTED)
				this.setState(Trap.Transport.State.AVAILABLE);
			
			this.pollTimeout = setTimeout(function() { mt.flushTransport(true); }, 28000);
		}
		
	}
	else if (req.method == "POST")
	{
		
		var numRead = 0;
		var mt = this;
		
		req.on("data", function(chunk) {
			console.log("Got CHUNK: [" + chunk + "]");
			numRead += chunk.length;
			if (chunk.length > 0)
				mt.receive(chunk, 0, chunk.length);
		});
		
		req.on("end", function() {
			
			console.log("Got Stream END; " + numRead);
			
			if(numRead == 0)
			{
				// logout
				mt.setState(Trap.Transport.State.DISCONNECTING);
				mt.setState(Trap.Transport.State.DISCONNECTED);
				mt.flushTransport(true);
				res.statusCode = 200;
			}
			else
			{
				res.statusCode = 202;
			}
			
			mt.setCors(req, res);
			res.end();

		});
		
	}
	else if (req.method == "DELETE")
	{

		this.setCors(req, res);
		res.end();
	}
	else if (req.method == "OPTIONS")
	{
		console.log("Responding with CORS options");
		res.statusCode = 200;
		this.setCors(req, res);
		res.write("");
		res.end();
	}
	
	

};


Trap.Transports.HTTPServer.prototype.updateConfig = function()
{
	Trap.AbstractTransport.prototype.updateConfig.call(this);

	this.expirationDelay = this._configuration.getIntOption(this._prefix + ".expirationDelay", this.expirationDelay);
	this.connectionTimeout = this._configuration.getIntOption(this._prefix + ".connectionTimeout", this.connectionTimeout);
};

//TODO: Expose IP information on websocket level...
Trap.Transports.HTTPServer.prototype.fillAuthenticationKeys = function(keys)
{
};

Trap.Transports.HTTPServer.prototype.updateContext = function()
{
	// TODO Auto-generated method stub
};

Trap.Transports.HTTPServer.prototype.internalSend = function(message, expectMore) 
{
	this._buf.push(message);
	console.log(this._buf);
};

Trap.Transports.HTTPServer.prototype.isClientConfigured = function()
{
	return this.url && typeof(this.url) == "string" && this.url.length > 4;
};

Trap.Transports.HTTPServer.prototype.internalConnect = function()
{
	
	this.logger.debug("Cannot connect a server!");
};

Trap.Transports.HTTPServer.prototype.internalDisconnect = function()
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

Trap.Transports.HTTPServer.prototype.flushTransport = function(force)
{
	
	if (this._buf.length == 0 && !force)
		return
	
	console.log("HTTP flushing...");
	
	this.pollRes.statusCode = 201;
	this.setCors(this.pollReq, this.pollRes);
	
	console.log(this._buf);
	
	if (this._buf.length > 0)
	{
		// Serialise and flush the messages
		var bos = (this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream());

		for (var i=0; i<this._buf.length; i++)
		{
			bos.write(this._buf[i].serialize(this.useBinary));
		}
		
		var data = (this.useBinary ? bos.toArray() : bos.toString());
		
		this._buf = [];
		this.pollRes.write(new Buffer(data));
		
		console.log("Wrote: [" + data + "]");
		
	}
	
	this.pollRes.end();
	
	console.log("Ended poll at " + this.getState());
	if ((this.getState() == Trap.Transport.State.CONNECTED) || (this.getState() == Trap.Transport.State.AVAILABLE))
	{
		this.oldState = this.getState();
		this.setState(Trap.Transport.State.UNAVAILABLE);
	}
		
	this.pollRes = null;
	this.pollReq = null;
	clearTimeout(this.pollTimeout);
	this.pollTimeout = null;
};

Trap.Transports.HTTPServer.prototype.setState = function()
{
	if (this.getState() == Trap.Transport.State.DISCONNECTED || this.getState() == Trap.Transport.State.ERROR)
		this.running = false;

	Trap.AbstractTransport.prototype.setState.apply(this, arguments);
};