//< needs(trap)

//> public void fn()
Trap.EventObject = function()
{
	this._eventlistenersMap = {};
};

// (void) addEventListener(String, Function)
Trap.EventObject.prototype.addEventListener = function(type, listener) {
    if (!this._eventlistenersMap[type])
        this._eventlistenersMap[type] = [];
    var eventlisteners = this._eventlistenersMap[type];
    for (var i = 0; i<eventlisteners.length; i++) {
        if(listener === eventlisteners[i])
            return;
    }
    eventlisteners[i] = listener;
};

//(void) removeEventListener(String, Function)
Trap.EventObject.prototype.removeEventListener = function(type, listener) {
    if (!this._eventlistenersMap[type])
        return;
    var eventlisteners = this._eventlistenersMap[type];
    for (var i = 0; i < eventlisteners.length; i++) {
        if (listener === eventlisteners[i]) {
            eventlisteners.splice(i,1);
            break;
        }
    }
};

Trap.EventObject.prototype.on = Trap.EventObject.prototype.addEventListener;
Trap.EventObject.prototype.off = Trap.EventObject.prototype.removeEventListener;

Trap.EventObject.prototype._dispatchEvent = function(evt) {
    var listeners = this._eventlistenersMap[evt.type];
    
    if (!evt.target)
    	evt.target = this;
    
    if(!!listeners)
    {
    	for (var i = 0; i < listeners.length; i++)
    	{
    		try
    		{
        		listeners[i](evt);
    		}
    		catch (e)
    		{
    			if (this.logger)
    			{
    				this.logger.warn("Exception while dispatching event to listener; ", e, " to ", listeners[i], ". Event was ", evt);
    			}
    		}
    		
    	}
    }
    
    var f;
	try
	{
	    f = this["on"+evt.type];
	    if (f && typeof(f) == "function") f.call(this, evt);
	}
	catch (e)
	{
		if (this.logger)
		{
			this.logger.warn("Exception while dispatching event to listener; ", e, " to ", f, ". Event was ", evt);
		}
	}
};