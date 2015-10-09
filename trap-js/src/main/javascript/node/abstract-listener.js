Trap.AbstractListenerTransport = function()
{
	Trap.AbstractTransport.prototype.constructor.call(this);
};

Trap.AbstractListenerTransport.prototype = new Trap.AbstractTransport;
Trap.AbstractListenerTransport.prototype.constructor = Trap.AbstractListenerTransport;

Trap.AbstractListenerTransport.prototype.canListen = function()
{
	return true;
};

Trap.AbstractListenerTransport.prototype.disconnect = function()
{
	if ((this.state == Trap.Transport.State.DISCONNECTING) || (this.state == Trap.Transport.State.DISCONNECTED) || (this.state == Trap.Transport.State.ERROR))
		return; // Cannot re-disconnect
		
	// Send directly to child.
	this.internalDisconnect();
	this.setState(Trap.Transport.State.DISCONNECTED);
};

Trap.AbstractListenerTransport.prototype.internalSend = function(message, expectMore)
{
	throw "I can't do that, Dave";
};

Trap.AbstractListenerTransport.prototype.internalConnect = function()
{
	throw"Cannot connect server transport.";
};