Trap JavaScript
====

The JavaScript library provides a fully-fledged stack for JS execution environments. It supports all major browsers and also node.js. 

Installation
====

The JS library comes pre-built with a debug and minified version. You can download them here:

* https://repo1.maven.org/maven2/com/ericsson/research/trap/trap-js/

Once downloaded, add it as a script tag

	<script src="trap.js"></script>
	
Usage
====

Once loaded, Trap puts everything under the Trap namespace and can be instantiated from there. Creating a client will automatically connect it, so make sure to set the handlers properly.

	client = new Trap.ClientEndpoint("http://trapserver.com:8888");
	client.onopen = function() {
		console.log("I am now open!");
	}
	
	client.onmessage = function(evt) {
		console.log("Received from server: " + evt.string);
	}
	
You can send messages any time after _onopen_ has been called, including inside the onopen call itself.

	client.onopen = function() {
		client.send("Hello, server");
	}
	
Sending Different Datatypes
----

Trap supports three basic data types natively: ArrayBuffers, Objects and Strings. You can send any of them natively.

	client.send(myArrayBuffer);
	
	client.send({foo: "bar"});
	
	client.send("foobar");

Receiving Different Datatypes
----

Likewise, the different types can be retrieved at any time. Suppose an incoming message contains the string `{"foo":"bar"}`. We can read it out as follows:

	client.onmessage = function(evt) {
	
		evt.bytes; // Returns an ArrayBuffer containing the bytes ['{', '"', 'f', 'o', 'o', '"', ':', '"', 'b', 'a', 'r', '}']
		evt.string; // Returns the string '{"foo":"bar"}'
		evt.object // Returns the object {foo:"bar"}
	
	}
	
Not all of these parameters are applicable to all datatypes. For example, data sent as binary (image) will not translate well to a string. Likewise, if the string does not represent an object, it will not parse well into JSON. It is up to the developer to separate between the types. Data will not be attempted to be parsed until the appropriate field is read.