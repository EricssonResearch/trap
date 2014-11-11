/**
 * @constructor
 * @returns {Trap.HTTPListenerTransport}
 */
Trap.Transports.HTTPListenerTransport = function() {
	
	this.http = require('http');
	this.transports = {};
	
	Trap.AbstractListenerTransport.prototype.constructor.call(this);
	
};

Trap.Transports.HTTPListenerTransport.prototype = new Trap.AbstractListenerTransport;
Trap.Transports.HTTPListenerTransport.prototype.constructor = Trap.HTTPListenerTransport;

Trap.Transports.HTTPListenerTransport.prototype.getTransportName = function() {
	return "http";
};

Trap.Transports.HTTPListenerTransport.prototype.getProtocolName = function() {
	return "http";
};

/**
 * @param listener
 * @param context
 */
Trap.Transports.HTTPListenerTransport.prototype.listen = function(listener, context)
{
	var mt = this;
	this.listenerDelegate = listener;
	this.listenerContext = context;
	
	this.port = parseInt(this.getOption("port"));
	
	if (isNaN(this.port))
		this.port = 8088;
	
	var parser = require('url');
	
	this.http.createServer(function(req, resp) {
		var url = parser.parse(req.url, true);
		var path = url.pathname;
		console.log("got path: " + path + " with method " + req.method);
		
		var t = mt.transports[path];
		if (!t)
		{
			if (path.length > 1)
			{
				resp.statusCode = 404;
				resp.end();
				return;
			}
			else
			{
				var mid = Trap._uuid();
				var res = "/" + mid;
				t = new Trap.Transports.HTTPServer();
				mt.transports[res] = t;
				
				listener.ttsIncomingConnection(t, mt, context);

				resp.statusCode = 200;
				t.setCors(req, resp);
				resp.write(mid);
				resp.end();
				return;
			}
			
		}
		console.log("Forwarding to T");
		t.handle(req, resp);
		
	}).listen(this.port);

	this.setState(Trap.Transport.State.CONNECTED);
	
};

Trap.Transports.HTTPListenerTransport.prototype.getClientConfiguration = function(destination, failOnUnreachable)
{
	var hostName = this.getOption("autoconfig.host");

	if (hostName == null)
		hostName = "localhost";

	var targetUri = "http://" + hostName + ":" + this.port ;

	destination.setOption(this._prefix, "url", targetUri);
};

Trap.Transports.HTTPListenerTransport.prototype.internalDisconnect = function() {
	wss = null;
};
