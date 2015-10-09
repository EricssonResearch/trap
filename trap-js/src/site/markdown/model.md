Development Model
====

The Trap JS API is written to closely mirror the Java API, with concessions primarily being done in the areas of namespaces and callbacks. Whereas Java uses a delegate model for callbacks, JavaScript has those same delegates be callback functions. Whereas Java has packages, JS uses the Trap. namespace.

In order to facilitate a good model for both languages, the Trap JS API has some adaptations.

# Getters and Setters

Properties on Trap objects can be accessed as normal JavaScript properties, as follows:

	channel = endpoint.getChannel(1);
	channel.streaming = false;
	
However, to ensure code compatibility, any property also has accessor methods set.

	channel.setStreaming(false);
	
If trying to serialise a Trap object, whether for debug, printing or other purposes, take care to export the property values as values and not as functions.

# Events

The JavaScript API dispatches events wherever the Java API would call delegates. These events can be registered for using the _.on(callback)_ method and unregistered using _.off(callback)_. (Alternative syntax being add/removeEventListener). One callback per object can be set using the .on_event_ property, e.g. `client.onopen = function(){}`

# Logging

The Trap JS library comes with rudimentary logging functionality. This can be enabled and disabled using Trap._logger.

# Multiple connections

Trap can be instantiated multiple times, as long as the underlying versions match. This includes binary support.

# Java API Compatibility

The JavaScript library closely tracks the Java library. As such, internal usage of Trap (including extending internal methods) uses Java constructs. Trap ports over parts of the following APIs for compatibility purposes:

* Trap.ByteArrayOutputStream
* Trap.List
* Trap.Map
* Trap.Set
* Trap.StringBuffer

# zlib

Trap embeds the zlib library from <https://github.com/imaya/zlib.js>. This is used for compression.