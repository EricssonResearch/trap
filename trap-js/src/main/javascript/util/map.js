//<needs(trap)
/*
 * The Map class provides an implementation-agnostic way to have a map with any
 * keys/values but without the hassle of being affected during iterations by
 * third party libraries, since you can ask for all keys.
 */

Trap.Map = function(src)
{
	this._map = {};
	this._keys = [];
	
	if (typeof(src) != "undefined")
	{
		// Clone
		for (var key in src.allKeys())
			this.put(key, src.get(key));
	}
};

Trap.Map.prototype.put = function(key, value)
{
	if (!(key in this._map))
		this._keys.push(key);
	
	this._map[key] = value;
};

Trap.Map.prototype.get = function(key)
{
	return this._map[key];
};

Trap.Map.prototype.allKeys = function()
{
	return this._keys;
};

Trap.Map.prototype.containsKey = function(key)
{
	return typeof(this._map[key]) != "undefined";
};

Trap.Map.prototype.remove = function(key)
{
	for (var i=0; i<this._keys.length; i++)
		if (this._keys[i] == key)
			this._keys.splice(i, 1);
	
	delete this._map[key];
};

Trap.Map.prototype.size = function()
{
	return this._keys.length;
};

Trap.Map.prototype.putAll = function(src)
{
	var keys = src.allKeys();
	
	for (var i=0; i<keys.length; i++)
		this.put(keys[i], src.get(keys[i]));
};