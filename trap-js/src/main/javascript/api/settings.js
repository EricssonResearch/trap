if (!Trap)
	Trap = {};

Trap.Settings = function(){};
	
Trap.Settings.prototype.enableTransport = function(transportName){};
	
Trap.Settings.prototype.disableTransport = function(transportName){};
	
Trap.Settings.prototype.isTransportEnabled = function(transportName){};
	
Trap.Settings.prototype.getConfiguration = function(){};
	
Trap.Settings.prototype.configure = function(cfgString){};
	
Trap.Settings.prototype.configureTransport = function(transportName, configurationKey, configurationValue){};
	
Trap.Settings.prototype.getTransports = function(){};
	
Trap.Settings.prototype.getTransport = function(transportName){};