//<needs(trap)

Trap.Logger = function(name)
{
	this.name = name;
};

Trap.Logger._loggers = {};

Trap.Logger.getLogger = function(name)
{
	var logger = Trap.Logger._loggers[name];
	
	if (!logger)
	{
		logger = new Trap.Logger(name);
		Trap.Logger._loggers[name] = logger;
	}
	
	return logger;
};

// TODO: Proper formatter plx
Trap.Logger.formatter = {};

{
	var _pad = function (val, len) {
		val = String(val);
		len = len || 2;
		while (val.length < len)
			val = "0" + val;
		return val;
	};

	var _logtime = function () {
		var d = new Date();
	    return [_pad(d.getHours(), 2)+":"+_pad(d.getMinutes(), 2)+":"+_pad(d.getSeconds(), 2)+"."+_pad(d.getMilliseconds(), 3)+" - "];
	};
	
	Trap.Logger.formatter._format = function(logMessage)
	{

		var params = _logtime();
		params.push(logMessage.label);
		
		if (logMessage.objects.length > 1 && typeof(logMessage.objects[0]) == "string")
		{
			// Slam the objects as needed.
			var msg = logMessage.objects[0];
			var idx = msg.indexOf("{}");
			var i=1;

			while (idx != -1)
			{
				if (i >= logMessage.objects.length)
					break;
				
				// Replaces first instance.
				msg = msg.replace("{}", logMessage.objects[i]);
				i++;
				
				// Technically, we can do it differently, but this way we'll prevent searching the parts of the string we processed
				idx = msg.indexOf("{}", idx);
			}
			
			params.push(msg);
			
			while (i < logMessage.objects.length)
			{
				var o = logMessage.objects[i++];
				
				params.push(o);
				
				if (o.stack)
					params.push(o.stack);
			}
			
		}
		else
			params.push.apply(params, logMessage.objects);
		
		if (logMessage.objects[0].stack)
			params.push(logMessage.objects[0].stack);
		
		return params;
	};
}

// TODO: Proper appender plx
Trap.Logger.appender = {};
Trap.Logger.appender._info = Trap.Logger.appender._warn = Trap.Logger.appender._error = function(){};

if (self.console && self.console.log) {
    if (self.console.log.apply)
    	Trap.Logger.appender._info = function(params) { self.console.log.apply(self.console, params); };
    else
    	Trap.Logger.appender._info = function(params) { self.console.log(params.join("")); };
    	
    if (self.console.warn) {
	    if (self.console.warn.apply)
	    	Trap.Logger.appender._warn = function(params) { self.console.warn.apply(self.console, params); };
	    else
	    	Trap.Logger.appender._warn = function(params) { self.console.warn(params.join("")); };
    } 
    else
    	Trap.Logger.appender._warn = Trap.Logger.appender._info;
    
    if (self.console.error) {
	    if (self.console.error.apply)
	    	Trap.Logger.appender._error = function(params) { self.console.error.apply(self.console, params); };
	    else
	    	Trap.Logger.appender._error = function(params) { self.console.error(params.join("")); };
    } 
    else
    	Trap.Logger.appender._error = Trap.Logger.appender._info;
}

Trap.Logger.prototype.isTraceEnabled = function() {
	return true;
};

Trap.Logger.prototype.trace = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.debug = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.info = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.warn = function()
{
	Trap.Logger.appender._warn(Trap.Logger.formatter._format({objects: arguments, label: "WARN: ", logger: this.name}));
};

Trap.Logger.prototype.error = function()
{
	Trap.Logger.appender._error(Trap.Logger.formatter._format({objects: arguments, label: "ERROR: ", logger: this.name}));
};