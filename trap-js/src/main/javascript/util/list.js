//<needs(trap)

//> public void fn()
Trap.List = function()
{
	this.list = [];
};

Trap.List.prototype.className = "list";

Trap.List.prototype.add = function(a, b)
{
	
	// add(index, object)
	if (!!b && typeof(a) == "number")
	{
		this.list.splice(a, 0, b);
	}
	else
	{
		// add(object)
		this.list.push(a);
	}
};

Trap.List.prototype.addLast = function(o)
{
	this.list.push(o);
};

Trap.List.prototype.pop = function()
{
	return this.remove(0);
};

Trap.List.prototype.remove = function(o)
{
	
	if (!o)
		o = 0;
	
	if (typeof o == "number")
	{
		var orig = this.list[o];
		this.list.splice(o, 1);
		return orig;
	}
	
	for (var i=0; i<this.list.length; i++)
		if (this.list[i] == o)
			this.list.splice(i, 1);
	
	return o;
};

Trap.List.prototype.peek = function()
{
	return (this.list.length > 0 ? this.list[0] : null);
};

Trap.List.prototype.size = function()
{
	return this.list.length;
};

Trap.List.prototype.isEmpty = function()
{
	return this.size() == 0;
};

Trap.List.prototype.get = function(idx)
{
	return this.list[idx];
};

Trap.List.prototype.getLast = function()
{
	return this.get(this.size()-1);
};

Trap.List.prototype.contains = function(needle)
{
	for (var i=0; i<this.list.length; i++)
		if (this.list[i] == needle)
			return true;
	return false;
};

Trap.List.prototype.sort = function()
{
	this.list.sort.apply(this.list, arguments);
};

Trap.List.prototype.clear = function()
{
	this.list = [];
};

Trap.List.prototype.addAll = function()
{
	var args = arguments;
	
	if (args.length < 1)
		return;
	
	if (args.length == 1)
	{
		var o = args[0];
		
		if (o.className == "list")
			args = o.list;
		else if (typeof(o) == "array" || (o.length && o.map))
			args = o;
	}

	// Add all elements
	for (var i=0; i<args.length; i++)
		this.addLast(args[i]);
	
};

Trap.List.prototype.iterator = function()
{
	var list = this;
	var idx = -1;
	return {
		hasNext: function() { return !!list.get(idx+1); },
		next: function() { return list.get(++idx); },
		remove: function() { list.remove(idx); idx; }
	};
};