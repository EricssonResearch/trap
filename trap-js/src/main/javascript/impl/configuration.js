Trap.Configuration = function(cfgString)
{
	
	// Set up local fields
	this.config = new Trap.Map();
	
	if (cfgString != null)
		this.initFromString(cfgString);
};

Trap.Configuration.CONFIG_HASH_PROPERTY = "trap.confighash";

Trap.Configuration.prototype.initFromString = function(configString)
{
	var strings = configString.split('\n');
	
	for (var i=0; i<strings.length; i++)
	{
		var c = strings[i].trim();
		
		var pos = c.indexOf('=');
		
		// Not found, alternatively no value
		if(pos < 0 || pos >= c.length - 1)
			continue;
		
		this.config.put(c.substring(0, pos).trim(), c.substring(pos+1).trim());
	}
};

Trap.Configuration.prototype.createPuttableGettableMap = function(optionsPrefix, cutPrefixes)
{
	var mt = this;
	var m = new Trap.Map();
	
	if (typeof(cutPrefixes) == "undefined")
		cutPrefixes = true;
	
	m.prefixKey = function(key) 
	{
		sb = (cutPrefixes?optionsPrefix:key);
		
		if(cutPrefixes) 
		{
			if(!optionsPrefix.endsWith("."))
				sb += ".";
			sb += key;
		}
		return sb;
	};
	
	m.put = function(key, value)
	{
		if(key == null || value == null)
			throw "Cannot put nil key or value";
		
		mt.config.put(m.prefixKey(key), value);
		Trap.Map.prototype.put.call(m, key, value);
	};
	
	return m;
};

Trap.Configuration.prototype.getOptions = function(optionsPrefix, cutPrefixes)
{
	if (typeof(cutPrefixes) == "undefined")
		cutPrefixes = true;
	
	var x = (cutPrefixes && !optionsPrefix.endsWith("."))?1:0;
	var m = this.createPuttableGettableMap(optionsPrefix, cutPrefixes);
	
	var keys = this.config.allKeys();
	
	for (var i=0; i<keys.length; i++)
	{
		var key = keys[i];
		var value = this.config.get(key);
		if(key.startsWith(optionsPrefix)) {
			if(cutPrefixes)
				key = key.substring(optionsPrefix.length+x);
			m.put(key, value);
		}
	}
	return m;
};

Trap.Configuration.prototype.toString = function()
{
	var keys = this.config.allKeys().sort();
	
	var sb = new Trap.StringBuilder();
	
	for (var i = 0; i < keys.length; i++)
	{
		sb.append(keys[i]);
		sb.append(" = ");
		sb.append(this.config.get(keys[i]));
		sb.append("\n");
	}
	return sb.toString();
};

/*
 * This code is unreadable. Refer to the Java implementation for what it does.
 * The mess here is because JavaScript doesn't support multiple signatures for
 * the same function name.
 */
Trap.Configuration.prototype.getOption = function(a1, a2)
{
	return this.config.get((typeof(a2) != "undefined"?a1+"."+a2:a1));
};

Trap.Configuration.prototype.getIntOption = function(option, defaultValue)
{
	
	var rv = parseInt(this.getOption(option));
	
	if (isNaN(rv))
		return defaultValue;
	
	return rv;

};

Trap.Configuration.prototype.getBooleanOption = function(option, defaultValue)
{
	
	var rv = this.getOption(option);
	
	if (typeof(rv) != "string")
		return defaultValue;
	
	if ("true" === rv.toLowerCase())
		return true;
	
	if ("false" === rv.toLowerCase())
	
	return defaultValue;

};

Trap.Configuration.prototype.setOption = function(a1, a2, a3)
{
	this.config.put((typeof(a3) != "undefined"?a1+"."+a2:a1), (typeof(a3) != "undefined"?a3:a2));
};