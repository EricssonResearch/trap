//< needs(trap)
//<needs(eventobject)


if (!Trap)
	Trap = {};

Trap.Configuration = function(){};
Trap.Configuration.prototype = new Trap.EventObject;
Trap.Configuration.prototype.constructor = Trap.Configuration;

//>> ::String
Trap.Configuration.CONFIG_HASH_PROPERTY = "trap.confighash";
Trap.Configuration.prototype.initFromString = function(configString){};
Trap.Configuration.prototype.getOptions = function(optionsPrefix){};
Trap.Configuration.prototype.getOption = function(optionName){};
Trap.Configuration.prototype.setOption = function(optionName, optionValue){};
Trap.Configuration.prototype.toString = function(){return "";};

