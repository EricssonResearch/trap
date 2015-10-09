Transport Abstraction Package (trap)
====
<a href="https://travis-ci.org/EricssonResearch/trap"><img src="https://travis-ci.org/EricssonResearch/trap.svg?branch=master"/></a>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ericsson.research.trap.packaging/trap-full/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ericsson.research.trap.packaging/trap-full/)


This library provides a socket-based interface that abstracts a set of underlying transports (socket, websocket, http, functioncall), while allowing reconnects, network connection changes, compression and multiplexing. Trap can utilise one or more underlying connections to improve performance, is a fully binary protocol, and is designed to handle failures.

# Features
Trap provides

* Transport independent data transfer using a message paradigm.
* Optional, reliable, in-order hop-to-hop transfer, regardless of underlying medium.
* Connection re-establishment, when applicable. This recovers from a network connection change (e.g. wifi to cellular data)
* Multiplexing of up to 255 application streams, at configurable priority levels that allow pre-emption of bulk data.
* Support for Java and JavaScript clients and servers (node.js required for servers).

As an abstraction, Trap applies the following overheads (measured in Java, as of 2014)

* 16 bytes of protocol data per message (excluding transport overhead such as TCP/HTTP)
* Approximately 50 kilobytes of RAM per connection (configurable usage for active connections)
* A minimum of 200 kilobytes of RAM to load class files.
* Two threads for network & timing. Additional threads as required by the application.
* About 500 kilobytes .jar file (if packaging all features, including slf4j which Trap depends on)

# Installation
The easiest way to add Trap for Java is to include the full archive in one step. The following code will include Trap as well as the four default, built-in transports. These transports are not suitable in all contexts, but can serve as an entry point. 

		<dependency>
			<groupId>com.ericsson.research.trap.packaging</groupId>
			<artifactId>trap-full</artifactId>
			<version>1.3</version>
		</dependency>

The recommended way is to include the specific transports as needed. The full list of transports is available in the [main repo](https://github.com/EricssonResearch/trap/tree/master/trap-transports). Some transports, such as HTTP, have multiple providers. It is recommended to use the best provider available (e.g. Servlet) when applicable.

# Getting Started
For more detailed information, see the dedicated getting started guide for [Java](https://ericssonresearch.github.io/trap/trap-api/quickstart.html) or [JavaScript](https://ericssonresearch.github.io/trap/trap-js/index.html), or check out the [Java examples](https://ericssonresearch.github.io/trap/trap-api/apidocs/reference/com/ericsson/research/trap/examples/package-summary.html). 

## Listening for incoming connections

### Java
After you have included the Trap dependency, you can create a listener, which is the equivalent to a server socket.

	listener = TrapFactory.createListener("http://0.0.0.0:8888");
	
This will create and configure a listener with a fixed transport (http) to listen on any IP and port 8888. When the port is unspecified, Trap will automatically allocate available ports. The full list of ports can be accessed via <code>listener.getClientConfiguration()</code>.

The listener, while allocated, is not listening to the sockets yet. In order to start listening, you need to call

	listener.listen(delegate);
	
where _delegate_ is an object that implements at least the [OnAccept](https://ericssonresearch.github.io/trap/trap-api/apidocs/reference/com/ericsson/research/trap/delegates/OnAccept.html) interface from the [Trap Delegate Interfaces](https://ericssonresearch.github.io/trap/trap-api/apidocs/reference/com/ericsson/research/trap/delegates/package-summary.html). This delegate will be called by the listener whenever a new Trap connection is established. 

Whenever an incoming connection is received, the onAccept method will be called with a _TrapEndpoint_ object. The following code exemplifies how this is handled:

	void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context) {
		endpoint.setDelegate(myConnectionDelegate);
		myEndpointCollection.put(endpointKey, endpoint);
	}

### JavaScript (node.js)

Instantiate the endpoint directly. You can optionally pass a configuration as the first parameter, specifying ports, transports, etc.

	Trap = require("../trap-node.js");

	listener = new Trap.ListenerEndpoint();
	
To activate the listener, call the `listen` method. It accepts one argument, an object containing event handlers.
	
	listener.listen({
		incomingTrapConnection: function(endpoint)
		{
			console.log("New connection");
			endpoint.onmessage = function(m)
			{
				endpoint.send(m.data);
			};
		}
	});
	
To verify that initialisation succeeded properly, in both JS and Java you can call

	listener.getClientConfiguration();
	
which will print a complete list of the transports and the host/port combinations they have bound to.

## Connecting to a server

### Java 
Connect to a listener by using the factory to create a new client. After the client is configured and a delegate is set, call `open()` to begin connecting. The connection process is asynchronous.

	client = TrapFactory.createClient("http://127.0.0.1:8888");
	client.setDelegate(myDelegate);
	client.open();
	
This will create a client to connect using http to localhost, port 8888. The delegate should implement at least the [OnOpen](https://ericssonresearch.github.io/trap/trap-api/apidocs/reference/com/ericsson/research/trap/delegates/OnOpen.html) interface to be notified when the connection is open. The _trapOpen()_ function will be called when the connection is established.

### JavaScript
Connect to a listener by instantiating an endpoint directly. Note that, unlike Java, the `open()` method is optional. It exists, but does not need to be called. The client will open automatically after the current run loop.

	client = new Trap.ClientEndpoint("http://127.0.0.1:8888");
	client.onopen = ...
	client.onmessage = ...
	// open() is implicit in JS and does not need to be called

## Sending data

Send data by as calling `TrapEndpoint.send()`, from either a client or server. Continuing the client example above, the relevant entry would be (JS and Java)

	client.send(data);
	
## Receiving data

### Java
To receive data in Java, use the `OnData` or `OnObject` delegates. The implemented method will be called when data is received, on the object registered as a delegate using `setDelegate()`.


    // OnData callback
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        System.out.println("Received message: [" + StringUtil.toUtfString(data) + "] of length " + data.length);
    }

### JavaScript
In JavaScript, the `message` event will be dispatched when a message is received.

	client.onmessage = function(evt) {
		console.log("Received from server: " + evt.string);
	}
	
## Further Guides
There are more detailed guides and examples in the [Trap Site](https://ericssonresearch.github.io/trap).


# Links
* [Home Page](https://ericssonresearch.github.io/trap)
* [Source Code](https://github.com/ericssonresearch/trap)
* [Javadoc](https://ericssonresearch.github.io/trap/trap-api/apidocs/index.html)
* [JSDoc](https://ericssonresearch.github.io/trap/trap-js/jsdoc/index.html)
