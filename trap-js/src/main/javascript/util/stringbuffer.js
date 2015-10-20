//<needs(trap)

//> void fn()
Trap.StringBuffer = function()
{
	this.buf = "";
};

//> void fn(String)
Trap.StringBuffer.prototype.append = function(arg)
{
	this.buf += arg;
};

//> String fn()
Trap.StringBuffer.prototype.toString = function()
{
	return this.buf;
};

Trap.StringBuilder = Trap.StringBuffer;