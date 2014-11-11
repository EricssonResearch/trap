/**
 * A TrapClient in JavaScript differs from the Java implementation in a number of significant aspects.
 * 
 * First, the JS client will automatically open itself after the current run loop has finished,
 * eliminating the need of the open() command.
 * 
 * Second, the JS client can be constructed using new Trap.Client(cfgString) directly, as opposed to
 * having to use a factory. This is due to the dynamic nature of the language allowing us to load
 * the implementation directly instead of the API.
 */

Trap.Client = function(cfgString){};
Trap.Endpoint.prototype = new Trap.Endpoint;
Trap.Endpoint.prototype.constructor = Trap.Client;

Trap.Client.prototype.open = function(){};
