/**
 * @constructor
 * @returns {Trap.WebsocketListenerTransport}
 */
Trap.Transports.WebsocketListenerTransport = function() {
	
	this.WebSocketServer = require('ws').Server;
	this.wss = null;
	
	Trap.AbstractListenerTransport.prototype.constructor.call(this);
	
};

Trap.Transports.WebsocketListenerTransport.prototype = new Trap.AbstractListenerTransport;
Trap.Transports.WebsocketListenerTransport.prototype.constructor = Trap.WebsocketListenerTransport;

Trap.Transports.WebsocketListenerTransport.prototype.getTransportName = function() {
	return "websocket";
};

Trap.Transports.WebsocketListenerTransport.prototype.getProtocolName = function() {
	return "websocket";
};

/**
 * @param listener
 * @param context
 */
Trap.Transports.WebsocketListenerTransport.prototype.listen = function(listener, context)
	{
		var mt = this;
		this.listenerDelegate = listener;
		this.listenerContext = context;
		
		this.port = parseInt(this.getOption("port"));
	if (isNaN(this.port))
		this.port = 10080;
	
	wss = new this.WebSocketServer({port: this.port});
	
	wss.on('connection', function(ws) {
		var t = new Trap.Transports.WebSocket();
		t.init();
		t.setState(Trap.Transport.State.CONNECTED);
		t.ws = ws;
		t._initWS();
		t.useBinary = true;
		
		listener.ttsIncomingConnection(t, mt, context);
		
	});

	this.setState(Trap.Transport.State.CONNECTED);
	
};

Trap.Transports.WebsocketListenerTransport.prototype.getClientConfiguration = function(destination, failOnUnreachable)
{
	var hostName = this.getOption("autoconfig.host");

	if (hostName == null)
		hostName = "localhost";

	var targetUri = "ws://" + hostName + ":" + this.port ;

	destination.setOption(this._prefix, "wsuri", targetUri);
};

Trap.Transports.WebsocketListenerTransport.prototype.internalDisconnect = function() {
	wss = null;
};
