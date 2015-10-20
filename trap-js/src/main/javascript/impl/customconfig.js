
Trap.CustomConfiguration = function(cfgString) 
{
	// "super"
	Trap.Configuration.prototype.constructor.call(this, cfgString);
	
	this.setStaticConfiguration(cfgString);
	
};

Trap.CustomConfiguration.prototype = new Trap.Configuration;
Trap.CustomConfiguration.prototype.constructor = Trap.CustomConfiguration;

Trap.CustomConfiguration.prototype.setStaticConfiguration = function(configuration) {
	this.staticConfig = new Trap.Configuration(configuration);
};

Trap.CustomConfiguration.prototype.getOptions = function(optionsPrefix, cutPrefixes) {
	var options = this.createPuttableGettableMap(optionsPrefix, cutPrefixes);
	options.putAll(this.staticConfig.getOptions(optionsPrefix, cutPrefixes));
	options.putAll(Trap.Configuration.prototype.getOptions.call(this, optionsPrefix, cutPrefixes));
	return options;
};

Trap.CustomConfiguration.prototype.getOption = function() {
	var val = Trap.Configuration.prototype.getOption.apply(this, arguments);
	if (val == null)
		val = this.staticConfig.getOption.apply(this.staticConfig, arguments);
	return val;
};

Trap.CustomConfiguration.prototype.toString = function() 
{
	var sb = new Trap.StringBuffer();
	var keys = new Array();
	keys.push.apply(keys, this.staticConfig.config.allKeys());
	keys.push.apply(keys, this.config.allKeys());
	keys.sort();
	
	// Eliminate duplicate keys
	var len=keys.length,
	out=[],
	obj={};

	for (var i=0;i<len;i++) {
		obj[keys[i]]=0;
	}
	for (i in obj) {
		out.push(i);
	}

	for(var i=0;i<out.length;i++) {
		var key = out[i];
		sb.append(key);
		sb.append(" = ");
		sb.append(this.getOption(key));
		sb.append("\n");
	}
	return sb.toString();
};