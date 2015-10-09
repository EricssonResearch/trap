//<needs(trap)
//<needs(list)

Trap.Set = function(){
	Trap.List.prototype.constructor.call(this);
};
Trap.Set.prototype = new Trap.List;

Trap.Set.prototype.className = "set";

Trap.Set.prototype.add = function(a, b)
{
	
	var key = a;
	
	if (!!b && typeof(a) == "number")
		key = b;
	
	if (!this.contains(key))
		Trap.List.prototype.add.call(this, a, b);
};